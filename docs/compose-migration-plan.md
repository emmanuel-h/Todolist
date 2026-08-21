# Compose Migration Plan

## The goal

Not "be on Compose." The goal is that adding motion and design stops being expensive.

The sticky-note pad shipped in `fe87b1f` is the worked example. It cost:

- a wrapper `FrameLayout`, three sibling sheet views, and one ghost view
- four drawable XML files, four colours, five dimens, one shape style
- ~50 lines of `AnimatorSet`
- two workarounds — a ghost view so the animation would not block a synchronous test assertion, and a delayed IME call
- one discovered footgun — a chained `ViewPropertyAnimator` silently drops its second phase

That is the tax. This plan removes it.

## Decisions taken

- **Incremental, always shippable.** `ComposeView` interop; both toolkits coexist from Phase 3 to Phase 5. Every phase merges to `main` green and installable.
- **UI tests hoist down.** Most of the 246 Robolectric UI tests assert behaviour that belongs in the ViewModel — where Pitest already gates at 100%. They move down; a thin layer of real Compose UI tests remains.

## What does not move

| Layer | Files | Lines | Change |
| --- | --- | --- | --- |
| `domain/` | 35 | 386 | none |
| `data/` | 11 | 448 | none |
| `presentation/` | 6 | 340 | grows (tutorial commands) |
| **total** | **52** | **1,174** | already framework-free, `StateFlow`-based |

Pitest targets `domain.*, data.*, presentation.*` only. **The 100% mutation gate is untouched by every phase of this plan.** 507 of 753 tests never move.

## What moves

| | Files | Lines |
| --- | --- | --- |
| `ui/` | 8 | 2,408 |
| `res/layout/` | 12 | 1,074 |
| `ui/` tests | 15 | 4,401 |

Roughly 900 of those 2,408 lines are **deleted, not ported**:

| Today | Lines | Becomes |
| --- | --- | --- |
| `TodoListAdapter` + `TodoListsAdapter` | 660 | ~200 lines of row composables |
| `TodoItemAnimator` + `TodoListsItemAnimator` | 249 | `Modifier.animateItem()` — nothing |

## The crux: the tutorial drives views, not state

`TutorialOverlayController` (592 lines) operates the app by reaching into its view tree:

```kotlin
val holder = recycler.findViewHolderForAdapterPosition(inlinePos)
    as? TodoListAdapter.InlineAddViewHolder ?: return
holder.itemView.findViewById<MaterialButton>(R.id.btnInlineSubmit).performClick()
```

None of that survives. Compose has no ViewHolders, no `findViewById`, no `performClick`.

It also spans **both** screens, so neither screen can migrate while the tutorial is coupled to its internals. This is the sequencing constraint of the whole plan: **the tutorial is re-architected first, while still in Views.** Once it drives state instead of views it becomes toolkit-agnostic, and the two screens become independent of each other.

---

## Phase 0 — Compose in the build, nothing rendered ✅ DONE

**Goal** — prove AGP 9.3.1 + built-in Kotlin 2.2.10 + the Compose compiler plugin work together, before any UI depends on it.

### Outcome

**It works.** `org.jetbrains.kotlin.plugin.compose:2.2.10` applies cleanly alongside AGP 9's built-in Kotlin — no `org.jetbrains.kotlin.android` needed, no fallback required. All three gates stayed green: full unit suite, coverage report, and Pitest at **232/232 mutations killed, 100% test strength**.

Four findings worth carrying forward:

- **compileSdk pins the Compose version.** Compose BOM `2026.08.00` (compose.ui 1.12.0) requires `compileSdk 37`; this project is on `36.1`. Pinned to BOM `2026.06.01` (compose.ui 1.11.4) instead, which builds against 36.1 unchanged. `android-37.0` *is* installed locally, so the bump is available whenever it is wanted — but it is deliberately not part of Phase 0, to keep one variable in the de-risking spike.
- **Bumping compileSdk later means touching the Pitest task.** `app/build.gradle.kts` hardcodes `platforms/android-36.1/android.jar` when it assembles the Pitest classpath. That path must move in lockstep with `compileSdk` or mutation testing silently compiles against a different SDK than the app.
- **`createComposeRule()` is deprecated.** Use `androidx.compose.ui.test.junit4.v2.createComposeRule`. The v2 rule uses `StandardTestDispatcher` rather than `UnconfinedTestDispatcher`, so tests relying on immediate coroutine execution need explicit synchronisation. Adopt v2 from the start rather than porting later.
- **Robolectric + Compose works, including interop.** Both a bare composable and a `ComposeView` hosted inside the real `TodoListsActivity` compose and expose a semantics tree under Robolectric. Activity-hosted tests need `MainThreadDatabaseRule`, which swaps in a no-op `NotificationScheduler` — without it `WorkManager` is uninitialised and the activity throws in `onCreate`.

Rather than the throwaway `ComposeView` the plan originally called for, this landed as a permanent regression guard: `app/src/test/java/fr/mandarine/todolist/ui/ComposeToolchainTest.kt`, two tests. Phase 3 depends on exactly this working, so it is worth keeping.

### Deferred to a later phase

Bump `compileSdk` to 37 and Compose BOM to `2026.08.00`+, updating the Pitest `android.jar` path in the same commit. Behaviour-neutral — `targetSdk` stays 36 — but it is its own change with its own blast radius.

**Work**
- apply `org.jetbrains.kotlin.plugin.compose` at the Kotlin version (2.2.10)
- `android { buildFeatures { compose = true } }`
- Compose BOM, `ui`, `material3`, `ui-tooling-preview`, `activity-compose`
- test deps: `ui-test-junit4`, `ui-test-manifest`
- render one throwaway `ComposeView`, confirm it draws, delete it before merge

**Exit** — `testDebugUnitTest`, `createDebugUnitTestCoverageReport` and `pitest` all green; APK builds; nothing user-visible changed.

**Risk — highest uncertainty in the plan.** This repo has already eaten one AGP 9.x incompatibility: the Pitest Gradle plugin does not work because AGP 9 no longer registers `JavaPlugin`, which is why `app/build.gradle.kts` drives the Pitest CLI through a hand-rolled `JavaExec` task. Compose plugin + AGP 9 built-in Kotlin is a similarly young combination. Fallback if it fights: apply `org.jetbrains.kotlin.android` explicitly instead of relying on built-in Kotlin.

Do this phase first, alone, and merge it alone. If it fails, the plan needs rethinking and you have spent exactly one PR finding out.

## Phase 1 — The tutorial drives state ✅ DONE

**Goal** — sever `TutorialOverlayController` from view internals.

### Outcome

**Done, and the tutorial plays identically.** Verified end to end on the emulator after `pm clear`: all five scenes, progress dots advancing, the target→due caption switch, the notification banner, item add, complete/restore/reorder, demo-list deletion, and the `POST_NOTIFICATIONS` prompt firing only once state reaches `Dismissed`.

The 592-line controller split three ways along the seams that were tangled in it:

| Concern | Lands in | Toolkit-bound? |
| --- | --- | --- |
| The script — scene order and all pacing | `presentation/TutorialDirector.kt` | no |
| The vocabulary — anchors and actions | `domain/TutorialAnchor.kt`, `TutorialAction.kt` | no |
| Doing things to a screen | each activity, via `TutorialStage` | **yes — replaced per screen** |
| Drawing the hand, caption, banner | `ui/TutorialOverlayController.kt` as `TutorialOverlay` | **yes — replaced in phase 5** |

`findViewHolderForAdapterPosition`, `performClick` and adapter-position arithmetic still exist, but only inside the activity that owns those views. When a screen migrates, its `TutorialStage` implementation is rewritten and nothing else moves.

Anchors and actions address rows semantically — `ActiveItemToggle(0)`, `CompletedItemToggle(0)` — so `TodoListActivity` is now the only place that knows completed rows sit at `activeItemCount() + 2 + index`.

`TutorialDirector` is inside the Pitest gate and covered by 24 tests running on virtual time against a fake stage and overlay: **368/368 mutations killed, 100%**.

### Deliberate behaviour change

The old scene-4 drag captured `recycler.getChildAt(0)` *before* `adapter.moveItem(1, 0)` and then glided the hand to that captured view — which, after the move, is the row that got displaced *downward*. The hand therefore barely moved, contradicting the documented intent ("drag Apples to top"). It now glides to `ActiveItemRow(0)`, the actual drag destination. Revert by pointing at `ActiveItemRow(1)` if the old look is preferred.

### Two Pitest gotchas found

Both are now written up in `CLAUDE.md`, because any gated code will hit them:

- `--excludedClasses` matched `*Test` but not the synthetic classes Kotlin generates for `runTest { … }` lambdas (`FooTest$my test$1`), so test code was being mutated. Fixed by adding `*Test$*` / `*Tests$*`; test fakes must also be nested inside the test class rather than top-level in a gated package.
- Data-class getters survive when tests only compare whole instances, and suspend functions carry a `COROUTINE_SUSPENDED` check pitest reads as a conditional — fakes must `yield()` for it to be killable.

**Work**
- `TutorialViewModel` emits a `TutorialCommand` sealed class: `OpenInlineAdd`, `TypeText`, `PickDueDate`, `Submit`, `ToggleComplete(index)`, `DragTo(index)`
- each screen collects the command flow and applies it to its own state — no cross-screen view access
- the phantom hand still needs screen coordinates: introduce a `TutorialAnchor` registry the screen populates. One interface, two implementations — `getLocationOnScreen` today, `onGloballyPositioned` after Phase 5.

**Shape** — ~592 lines become roughly 200 lines of ViewModel command logic (Pitest-gated), 250 lines of view-side applier, 150 lines of hand animation.

**Exit** — `TutorialReplayUiTest` still passes; new `TutorialCommand` tests at 100% mutation score; no `performClick` or `findViewHolderForAdapterPosition` anywhere in `TutorialOverlayController`.

**Risk — medium.** This is real design work, not a mechanical port. It pays for itself even if the migration stops here: the tutorial script becomes unit-testable for the first time.

## Phase 2 — The paper design system ✅ DONE

**Goal** — the vocabulary every later phase is written in.

### Outcome

**Done, and nothing consumes it yet — by design.** Written up in [paper-design-system.md](paper-design-system.md). `ui/paper/` is 11 files: two token objects, one motion object, a theme, six primitives, and a previews file. All three gates stayed green — full suite, coverage report, and Pitest at **368/368 mutations, 100% test strength**, which is the same number as Phase 1 because the gate does not reach `ui`.

Four things worth carrying into Phase 3:

- **Both PNG tiles are gone.** Grain is a 64×64 tile built from a seeded `Random` — byte-identical on every call, so the paper does not shimmer between recompositions. Holes are vector circles rather than a repeated bitmap. Geometry was measured off `tile_paper_hole.png` and then verified against the running app: hole bands land on the same pixel rows (167.5, 503.5, 839.5 …) at 420dpi. The vector punches are slightly crisper than the downscaled PNG; that is the only visible difference.
- **Tokens are `object`s, not `CompositionLocal`s.** The palette is fixed and light-only, so a swappable theme seam would only invite it to be swapped. It also costs nothing in coverage — data-class `equals`/`hashCode` never get exercised.
- **Split the animation from the composable.** `stickyNotePeelAt` / `stickyNoteSettleAt` are pure functions of progress, unit-tested in plain JUnit; `StickyNotePad` only feeds them an `Animatable`. The View version needed a decorative ghost view so the animation would not block a synchronous test assertion — in Compose the peeling sheet is simply a second composable that exists while `peeling` is true. This is the shape to reach for whenever motion needs testing.
- **`PaperTheme` must set `surfaceTint = Color.Transparent`.** It is the Compose equivalent of `elevationOverlayEnabled=false`; without it M3 tints raised surfaces with `colorPrimary` and paper turns grey-blue.

### Two things that did not work

- **`captureToImage()` does not work under Robolectric.** It goes through window capture and waits for a real redraw that never comes — every shot times out after 2 s, with or without `@GraphicsMode(NATIVE)`. Screenshot testing needs a different harness (Paparazzi or Roborazzi) if it is ever wanted. Not pursued.
- **Robolectric-executed code contributes nothing to the JaCoCo report.** This is pre-existing, not new: the whole `ui` package reports 0/1403 lines. Only the pure-Kotlin parts of `ui/paper` (`PaperMotion`, `StickyNoteSheetState`) show coverage. The 100% line+branch claim in `CLAUDE.md` holds for `domain` and `presentation`; it has never held for `ui`. Worth deciding what to do about this before Phase 3 hoists tests down.

Instead of screenshots, verification is `PaperGalleryActivity` — a debug-only activity that renders every preview on a real device. It was used to confirm the surface, both row heights, both ghost rows, the badges, the icon states, and the full peel-and-settle cycle of the sticky pad on the emulator. It is kept, because Phase 3 will want it.

### Deviation from the plan

`InkIcon` was not in the plan's list as a separate primitive but earns its place: it is what keeps tints coming from the palette rather than from `?attr` lookups that no longer exist. `PaperMotion` gained `rowPlacement` (an `IntOffset` spring for `Modifier.animateItem`) and `instant`, both of which Phase 3 needs.

**Work** — a new `ui/paper` package:
- `PaperTheme.kt` — the ink-on-paper palette ported to Kotlin so the theme is toolkit-independent
- `PaperMotion.kt` — named spring specs (`sheetLift`, `sheetSettle`, `rowEnter`, `rowExit`) replacing duration-plus-easing everywhere
- primitives: `PaperSurface` (grain and punched holes drawn via `drawWithCache`, retiring both PNG tiles), `RuledRow`, `StickyNotePad`, `InkIcon`, `CountBadge`, `GhostRow`
- a `@Preview` for every primitive — this is the thing that actually makes design work cheap

**Exit** — previews render every primitive in populated and empty states. No screen consumes them yet.

**Risk — low.** Nothing depends on it. The real risk is over-building: restrict it to primitives the two screens actually need.

## Phase 3 — Screen 2 (pilot)

**Why Screen 2 first** — 270 lines against Screen 1's 625, no dialogs, no date pickers. But it carries the harder motion (the complete/restore cross-section animation), so it is an honest test rather than a soft one.

**Work**
- `TodoListActivity` → `setContent { TodoListScreen(…) }`
- `LazyColumn` + `Modifier.animateItem()` retires `TodoItemAnimator` (160 lines → 0)
- `TodoListAdapter` (313) → `TodoRow` / `InlineAddRow` / `SectionDivider` (~180)
- **drag reorder** — `ItemTouchHelper` has no first-party Compose equivalent. Either hand-roll `detectDragGesturesAfterLongPress` with `LazyListState`, or take a third-party reorderable library. Decide here; it is the one place Compose is currently *worse* than Views.
- tests: `TodoListActivityTest` (611), `TodoListAdapterTest` (400), `TodoListDragReorderTest` (273) hoist into `TodoListViewModel` tests; keep ~25 Compose tests

**Exit** — Screen 2 has no layout XML, no adapter, no ItemAnimator. The app ships. Both screens work.

**Risk — medium**, concentrated entirely in drag reorder.

## Phase 4 — Screen 1

**Work**
- `TodoListsActivity` (625) → `TodoListsScreen`
- `TodoListsAdapter` (347) → row composables; `TodoListsItemAnimator` (89) deleted
- four dialogs (288 lines of XML) → Compose `Dialog`
- the sticky-note pad becomes the Phase 2 `StickyNotePad`, and gains drag-to-peel almost for free
- five test files, ~120 tests, hoist down

**Exit** — `res/layout/` contains only `overlay_tutorial.xml`.

**Risk — medium.** Dialogs and date pickers are fiddly; `MaterialAlertDialogBuilder` has no direct Compose analogue. The tutorial's tap on the date picker's positive button must already be gone — Phase 1's job.

## Phase 5 — Tutorial overlay in Compose

**Work** — `overlay_tutorial.xml` (137 lines) → an overlay composable layered over the screen; phantom hand on `Animatable`; anchors resolved through `onGloballyPositioned`.

**Exit** — `res/layout/` is empty and deleted.

**Risk — low**, given Phase 1.

## Phase 6 — Teardown

**Work**
- drop `appcompat`, `recyclerview`, and the Views `com.google.android.material` dependency
- `AppCompatActivity` → `ComponentActivity`
- `res/values/themes.xml` shrinks to a bare theme for `windowBackground` and splash
- `IconOnlyUiTest` rewritten as a semantics-tree walk (see risks)
- update `CLAUDE.md` and `.claude/agents/ui.md`: the `ui` agent's boundary changes from "never touches layout XML or theme files" to "owns `ui/` and `ui/paper/`; never touches domain, data, or ViewModel"
- measure the APK delta

**Exit** — no View-system dependency left in the graph.

**Risk — low.**

---

## Risks, ranked

1. ~~**AGP 9.3.1 + built-in Kotlin + Compose compiler.**~~ **Retired in Phase 0** — the plugin applies cleanly and all three gates stay green. Replaced by a smaller one: the Compose version is capped by `compileSdk 36.1` until that is bumped, and the bump must move the Pitest `android.jar` path with it.
2. **Drag reorder.** No first-party Compose equivalent to `ItemTouchHelper`. Hand-roll or take a dependency — decide in Phase 3.
3. **`IconOnlyUiTest`.** It currently inflates layouts and walks the View tree for `TextView`s and untagged `ImageView`s. It must become a semantics-tree walk. The invariant matters more than the implementation — do not let it lapse during the migration.
4. **Robolectric + Compose is slower.** Suite time will grow before Phase 3's hoisting shrinks it again.
5. **APK size.** Compose adds roughly 2–3 MB before shrinking; R8 recovers much of it. Measure at Phase 6.
6. **Paper texture as a drawn surface.** Today it is free — it is `android:windowBackground`. Drawn per-frame it is not. Cache with `drawWithCache` and watch overdraw during scroll.

## What stays true throughout

- 100% JaCoCo line+branch and 100% Pitest mutation score on `domain`, `data`, `presentation` — every phase, no exceptions
- the icon-only rule: no static text, no untagged decorative images
- the fixed ink-on-paper palette, light-only, never wallpaper-derived
- every phase merges to `main` green and installable
