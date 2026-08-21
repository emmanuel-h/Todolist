# First-Launch Tutorial

## What it does
On the very first launch of `TodoListsActivity` a full-screen phantom-hand overlay plays a scripted five-scene tour on the real screens with real data: creates a demo list ("🛒 Groceries"), sets a due date for tomorrow via the real `DatePickerDialog`, previews the notification banner, opens the list and adds items, exercises complete/restore/drag-reorder, then returns to the lists screen and deletes the demo list before fading out. A bottom-center floating pill (5 progress dots + ✕ skip) and the back gesture cancel at any point. The tutorial never repeats; the demo list is always cleaned up on skip, finish, or mid-demo kill/restart. A dimmed replay button on the lists screen lets users re-watch at any time without resetting the seen flag.

## Architecture
- **Layers**: domain, data, presentation, UI
- **Key types**:
  - `TutorialStateRepository` — interface: `seenFlag: Flow<Boolean>`, `pendingDemoListId: Flow<String?>`, `markSeen()`, `saveDemoListId(id)`, `clearDemoListId()`
  - `TutorialStep` — enum with five scenes: `CREATE_LIST`, `SET_DUE_DATE`, `ADD_ITEMS`, `INTERACT_ITEMS`, `DELETE_LIST`
  - `TutorialScript` — ordered step list; `defaultScript()` returns the canonical sequence
  - `ShouldRunTutorialUseCase` — returns `true` if the seen flag has never been set
  - `StartTutorialUseCase` — persists the seen flag immediately (crash safety) before the overlay appears
  - `SaveDemoListIdUseCase` — persists the demo list id so a killed-mid-demo leftover can be found
  - `FinishTutorialUseCase` — clears the persisted demo list id after clean deletion
  - `CleanupAbandonedTutorialUseCase` — called on every launch; finds and deletes any leftover demo list by id
  - `TutorialUiState` — sealed class: `Hidden`, `ReadyToStart`, `Active(step: TutorialStep)`, `Dismissed`
  - `TutorialViewModel` — `initialize()` runs cleanup then gate check; `onDemoListCreated(id)`, `advanceStep()`, `skip()`; `replay()` transitions `Hidden`/`Dismissed` → `ReadyToStart`, no-op when already `ReadyToStart` or `Active`
  - **Permission sequencing** — `TodoListsActivity` requests `POST_NOTIFICATIONS` (API 33+) only when `TutorialUiState` reaches `Dismissed`, so the system permission dialog never overlaps the tour
  - `SharedPreferencesTutorialStateRepository` — data-layer implementation backed by `SharedPreferences`
  - `TutorialAnchor` / `TutorialAction` — domain sealed classes naming *what the hand points at* and *what the screen should do*, with no reference to views
  - `TutorialStage` — interface each screen implements: `boundsOf(anchor)`, `perform(action)`, `awaitDemoListId()`, `bannerContent()`. This is the seam that lets the script run against any UI toolkit; the activities are the View-system implementations
  - `TutorialOverlay` — interface for the phantom-hand choreography: `glideTo`, `tap`, `grip`, `release`, caption and banner
  - `TutorialDirector` — the five scenes as `suspend` functions, in the presentation layer. Owns all pacing and ordering; calls only `TutorialStage` and `TutorialOverlay`, never a view
  - `TutorialOverlayController` — attaches/detaches the overlay view from the `DecorView`, renders the hand/caption/banner as the `TutorialOverlay` implementation, and hands each `TutorialUiState` to a `TutorialDirector`
- **Async contract**: `TutorialStateRepository` methods are `suspend`; `TutorialViewModel` collects state as `StateFlow<TutorialUiState>`; scene functions in `TutorialOverlayController` are `suspend` and run sequentially inside a coroutine scope tied to the activity lifecycle

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TutorialStateRepository.kt` — repository interface (seen flag + pending demo list id)
- `app/src/main/java/fr/mandarine/todolist/domain/TutorialStep.kt` — five-value enum for tutorial scenes
- `app/src/main/java/fr/mandarine/todolist/domain/TutorialScript.kt` — ordered step list and `defaultScript()` factory
- `app/src/main/java/fr/mandarine/todolist/domain/ShouldRunTutorialUseCase.kt` — gate check use case
- `app/src/main/java/fr/mandarine/todolist/domain/StartTutorialUseCase.kt` — persists seen flag before overlay appears
- `app/src/main/java/fr/mandarine/todolist/domain/SaveDemoListIdUseCase.kt` — persists demo list id for crash recovery
- `app/src/main/java/fr/mandarine/todolist/domain/FinishTutorialUseCase.kt` — clears demo list id after clean deletion
- `app/src/main/java/fr/mandarine/todolist/domain/CleanupAbandonedTutorialUseCase.kt` — deletes leftover demo list on every launch
- `app/src/main/java/fr/mandarine/todolist/data/SharedPreferencesTutorialStateRepository.kt` — SharedPreferences-backed implementation
- `app/src/main/java/fr/mandarine/todolist/presentation/TutorialUiState.kt` — sealed class for overlay states
- `app/src/main/java/fr/mandarine/todolist/presentation/TutorialViewModel.kt` — drives tutorial lifecycle; wires cleanup, gate, step advancement, skip, and replay
- `app/src/main/java/fr/mandarine/todolist/domain/TutorialAnchor.kt` — sealed class of hand targets
- `app/src/main/java/fr/mandarine/todolist/domain/TutorialAction.kt` — sealed class of screen actions
- `app/src/main/java/fr/mandarine/todolist/domain/TutorialScreen.kt` — `LISTS` / `ITEMS`
- `app/src/main/java/fr/mandarine/todolist/presentation/TutorialStage.kt` — stage interface plus `TutorialBounds` and `TutorialBannerContent`
- `app/src/main/java/fr/mandarine/todolist/presentation/TutorialOverlay.kt` — hand/caption/banner interface and `TutorialCaption`
- `app/src/main/java/fr/mandarine/todolist/presentation/TutorialDirector.kt` — the five scenes, all pacing, no views
- `app/src/main/java/fr/mandarine/todolist/ui/TutorialOverlayController.kt` — attaches overlay to DecorView; renders the hand as `TutorialOverlay`; delegates scenes to `TutorialDirector`
- `app/src/main/java/fr/mandarine/todolist/ui/TutorialStageSupport.kt` — `tutorialBounds()`, `hideTutorialKeyboard()`, `typeTutorialText()` shared by both stages
- `app/src/test/java/fr/mandarine/todolist/presentation/TutorialDirectorTest.kt` — 24 tests driving every scene against fake stage/overlay on virtual time
- `app/src/main/res/layout/overlay_tutorial.xml` — full-screen touch-intercepting overlay: notification banner card, phantom hand cursor ImageView, bottom-center floating elevated pill (5 progress dots + ✕ skip button)
- `app/src/main/res/drawable/ic_replay.xml` — circular-arrow icon for the replay button
- `app/src/main/res/drawable/ic_tab_right.xml` — 12dp arrow-to-limit glyph for due-date rows
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — wires `TutorialOverlayController`; exposes FAB and due-date picker; hosts `btnReplayTutorial` (dimmed 38% alpha, top-right); hides replay button while inline create row is open; registers `OnBackPressedCallback` (active only during tutorial)
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListActivity.kt` — wires `TutorialOverlayController`; exposes inline-add bar
- `app/src/main/res/layout/item_todo_list.xml` — adds `iconDueDateLimit` (id), the `ic_tab_right` view between the alarm icon and due-date text; tinted identically to the rest of the due-date line
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsAdapter.kt` — applies three-tier tint to `iconDueDateLimit` alongside the alarm icon and date text
- `app/src/main/java/fr/mandarine/todolist/AppContainer.kt` — holds `tutorialStateRepository` and shared `tutorialViewModel`
- `app/src/test/java/fr/mandarine/todolist/ui/TutorialReplayUiTest.kt` — 3 Robolectric tests covering replay visibility, tap behaviour, and hide-while-row-open invariant

## Invariants & contracts
- The soft keyboard is hidden (`hideKeyboard()` → `hideSoftInputFromWindow`) immediately after every phantom typing beat, so it never covers the caption pill or later choreography. It must hide the IME **without clearing focus** — clearing focus would collapse the items-screen inline add row (`InlineAddViewHolder`'s focus listener) and break scene 3.
- The seen flag is written by `StartTutorialUseCase` **before** the overlay appears; a crash during the tour will never cause the tutorial to re-run.
- The demo list id is written by `SaveDemoListIdUseCase` immediately after the demo list is created; `CleanupAbandonedTutorialUseCase` runs on every launch to delete any leftover.
- Skip and finish both delete the demo list and call `FinishTutorialUseCase` to clear the persisted id.
- `CleanupAbandonedTutorialUseCase` is called in `TutorialViewModel.initialize()`, which runs on every `TodoListsActivity` launch — not only the first.
- `handleState()` re-attaches the overlay (`attachToActivity`) when `ReadyToStart` arrives and the overlay was detached by a previously completed or skipped run — without this, a same-session replay plays with no hand, dots, or caption pill.
- `replay()` transitions `Hidden` or `Dismissed` → `ReadyToStart`; it is a no-op when state is already `ReadyToStart` or `Active`. It does NOT touch the seen flag — the automatic first-launch tutorial still fires exactly once ever.
- `btnReplayTutorial` must be hidden (`GONE`) while the inline create row is open (it would overlap the row's submit button) and restored to `VISIBLE` when the row closes.
- Progress dots in the skip pill: `ReadyToStart` = 1 dot filled; `Active(step)` = `step.ordinal + 1` dots filled (out of 5). Dots must never exceed 5 regardless of enum size.
- Scene 2 (`SET_DUE_DATE`): extended with two beats that teach the target/due distinction — beat 1 hovers over the calendar icon and shows `tutorialCaptionPill` (anchored just below the inline create row) with "📅 " + `date_kind_target_caption`; beat 2 moves to the alarm icon and switches the caption to "⏰ " + `date_kind_due_caption`, then the existing `DatePickerDialog` choreography picks tomorrow; the caption pill fades out after the date is picked. → see `date-kind-wording.md` for the caption pill implementation and the scoped exception to the icon-only rule.
- The notification banner rests below the status-bar inset so it is never clipped by the system bar.
- `iconDueDateLimit` (`ic_tab_right`) is rendered **only** on due-date lines, never on target-date lines.
- Demo strings ("🛒 Groceries", "🍎 Apples", "🥖 Bread") and the emoji prefixes in the scene 2 caption ("📅 ", "⏰ ") are Kotlin literals in `TutorialOverlayController`; they must never be placed in string resources (preserves the icon-only-UI rule — → see `icon-only-ui.md`). The caption body text itself (`date_kind_target_caption`, `date_kind_due_caption`) is a deliberate, scoped exception → see `date-kind-wording.md`.
- The overlay's views (hand cursor, banner) carry `importantForAccessibility="no"`; only the skip button is accessible.
- The skip button uses `@string/cancel` as its `contentDescription`; it must carry no visible text label.
- Drag in scene 4 is faked via `adapter.moveItem()` followed by `viewModel.reorderTodos()` — no `ItemTouchHelper` simulation. Expressed as `MoveActiveItem` then `CommitReorder`.
- **The director must never touch a view.** `TutorialDirector` may only call `TutorialStage` and `TutorialOverlay`. Every `findViewById`, ViewHolder lookup and adapter-position calculation belongs in the activity implementing `TutorialStage` for its own screen — that is what makes the tutorial survive the Compose migration (→ `compose-migration-plan.md`).
- Anchors and actions address rows **semantically** (`ActiveItemToggle(0)`, `CompletedItemToggle(0)`), never by adapter position. `TodoListActivity` alone knows that completed rows sit at `activeItemCount() + 2 + index` — active rows, then the inline-add row, then the divider.
- `TutorialStage.perform` returns `false` when the target is absent. The director treats that as fatal for the scene where the original code returned early (create row, open list, delete), and as skippable where it did not (the items ghost row).
- The `OnBackPressedCallback` in both activities is enabled **only** while the tutorial is active (`TutorialUiState.Active`); it must be removed in `onDestroy` to avoid leaks.

## UI
- **Screen(s)**: `TodoListsActivity`, `TodoListActivity`
- **Layout file(s)**: `res/layout/overlay_tutorial.xml`, `res/layout/item_todo_list.xml`
- **Design decisions**:
  - The skip control is a bottom-center floating elevated pill (not a mini FAB); it contains 5 progress dots and a ✕ button side-by-side. On the items screen (`TodoListActivity`) the pill has 88dp bottom margin so it floats above the pinned inline add bar.
  - The replay button (`btnReplayTutorial`) is a circular-arrow icon pinned top-right of the lists screen, dimmed to 38% alpha to signal secondary affordance. The RecyclerView top padding is raised so list rows clear it.
  - `iconDueDateLimit` (`ic_tab_right`, 12dp) sits between the alarm icon and date text on every due-date row and receives the same three-tier `colorPrimary`/`colorWarning`/`colorError` tint as the rest of the line.
  - Overlay attaches to `DecorView` so it sits above all app chrome including the toolbar. Phantom hand tint uses `?attr/colorPrimary` at 50% alpha.
