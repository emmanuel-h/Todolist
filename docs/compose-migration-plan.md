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

## Phase 0 — Compose in the build, nothing rendered

**Goal** — prove AGP 9.3.1 + built-in Kotlin 2.2.10 + the Compose compiler plugin work together, before any UI depends on it.

**Work**
- apply `org.jetbrains.kotlin.plugin.compose` at the Kotlin version (2.2.10)
- `android { buildFeatures { compose = true } }`
- Compose BOM, `ui`, `material3`, `ui-tooling-preview`, `activity-compose`
- test deps: `ui-test-junit4`, `ui-test-manifest`
- render one throwaway `ComposeView`, confirm it draws, delete it before merge

**Exit** — `testDebugUnitTest`, `createDebugUnitTestCoverageReport` and `pitest` all green; APK builds; nothing user-visible changed.

**Risk — highest uncertainty in the plan.** This repo has already eaten one AGP 9.x incompatibility: the Pitest Gradle plugin does not work because AGP 9 no longer registers `JavaPlugin`, which is why `app/build.gradle.kts` drives the Pitest CLI through a hand-rolled `JavaExec` task. Compose plugin + AGP 9 built-in Kotlin is a similarly young combination. Fallback if it fights: apply `org.jetbrains.kotlin.android` explicitly instead of relying on built-in Kotlin.

Do this phase first, alone, and merge it alone. If it fails, the plan needs rethinking and you have spent exactly one PR finding out.

## Phase 1 — The tutorial drives state

**Goal** — sever `TutorialOverlayController` from view internals.

**Work**
- `TutorialViewModel` emits a `TutorialCommand` sealed class: `OpenInlineAdd`, `TypeText`, `PickDueDate`, `Submit`, `ToggleComplete(index)`, `DragTo(index)`
- each screen collects the command flow and applies it to its own state — no cross-screen view access
- the phantom hand still needs screen coordinates: introduce a `TutorialAnchor` registry the screen populates. One interface, two implementations — `getLocationOnScreen` today, `onGloballyPositioned` after Phase 5.

**Shape** — ~592 lines become roughly 200 lines of ViewModel command logic (Pitest-gated), 250 lines of view-side applier, 150 lines of hand animation.

**Exit** — `TutorialReplayUiTest` still passes; new `TutorialCommand` tests at 100% mutation score; no `performClick` or `findViewHolderForAdapterPosition` anywhere in `TutorialOverlayController`.

**Risk — medium.** This is real design work, not a mechanical port. It pays for itself even if the migration stops here: the tutorial script becomes unit-testable for the first time.

## Phase 2 — The paper design system

**Goal** — the vocabulary every later phase is written in.

**Work** — a new `ui/paper/` package:
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

1. **AGP 9.3.1 + built-in Kotlin + Compose compiler.** Genuinely unknown. Phase 0 exists solely to find out cheaply. Precedent: the Pitest Gradle plugin already broke on AGP 9.
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
