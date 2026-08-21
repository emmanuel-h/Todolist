# Motion and Haptics

## What it does
Row-level animations play for every item add, complete, restore, and delete on both screens; haptic ticks fire on complete, drag pickup, drag drop, and swipe-confirm threshold. All motion is suppressed when the system animator scale is off, and animation events are also suppressed during the two tutorial steps that drive a phantom hand (`OPEN_LIST`, `COMPLETE_AND_REORDER`).

## Architecture
- **Layers**: domain, presentation, UI
- **Key types**:
  - `AnimationEvent` — sealed class (`ItemAdded(itemId)`, `ItemCompleted(itemId)`, `ItemRestored(itemId)`, `ItemDeleted(itemId)`, `ListAdded`); consumed by item animators to pick the correct animation style
  - `TodoListViewModel.animationEvents: SharedFlow<AnimationEvent>` — emits before each state update so the animator sees the event before `notifyItemChanged` fires
  - `TodoListsViewModel.animationEvents: SharedFlow<AnimationEvent>` — same contract for Screen 1
  - `TutorialViewModel.animationsSuppressed: Boolean` — computed from current `TutorialUiState`; `true` when step is `OPEN_LIST` or `COMPLETE_AND_REORDER`
  - `TodoItemAnimator` — custom `DefaultItemAnimator` for Screen 2; reads `pendingEvent` set by the activity before each state update
  - `TodoListsItemAnimator` — custom `DefaultItemAnimator` for Screen 1; reads `pendingListAdded` flag
- **Async contract**: `SharedFlow<AnimationEvent>` is collected in the activity's lifecycle scope; the activity sets `pendingEvent` on the animator synchronously before calling the adapter's notify method, so no race between event and diff

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/AnimationEvent.kt` — sealed class with five event variants
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListViewModel.kt` — adds `animationEvents` SharedFlow and `applyAndPublishWithEvent` helper; emits on add, complete/restore, delete; no event on edit/reorder/refresh
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListsViewModel.kt` — adds `animationEvents` SharedFlow and `applyAndPublishWithEvent` helper; emits `ListAdded` on createList/submitInlineInput; no event on delete/edit/reorder/refresh
- `app/src/main/java/fr/mandarine/todolist/presentation/TutorialViewModel.kt` — adds `animationsSuppressed` computed property; true only for `OPEN_LIST` and `COMPLETE_AND_REORDER` steps
- `app/src/main/java/fr/mandarine/todolist/ui/TodoItemAnimator.kt` — Screen 2 item animator: `ItemAdded` → slide-up+fade (translationY 16dp→0, alpha 0→1); `ItemCompleted`/`ItemRestored` → expand on destination side (scaleY 0→1, alpha 0→1) + alpha 0→1 fade on source side; divider fades in with the first completing row; removes run before adds (200 ms delay)
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsItemAnimator.kt` — Screen 1 item animator: `ListAdded` → slide-down+fade (translationY -16dp→0, alpha 0→1); all other ops use default animator
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListActivity.kt` — creates and sets `TodoItemAnimator` on the RecyclerView; collects `animationEvents` to set `pendingEvent`; adds `isReducedMotion()` helper; haptic on drag pickup (`LONG_PRESS`) and drag drop (`CONFIRM` API 30+ / `CLOCK_TICK` below)
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — creates and sets `TodoListsItemAnimator`; collects `animationEvents` to set `pendingListAdded`; adds `isReducedMotion()` helper
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — haptic feedback (`CONFIRM` / `CLOCK_TICK`) on the complete toggle button in both directions (completing and restoring)
- `app/src/test/java/fr/mandarine/todolist/AnimationEventTest.kt` — 13 unit tests for `AnimationEvent` variants
- `app/src/test/java/fr/mandarine/todolist/TodoListViewModelAnimationTest.kt` — 16 unit tests covering event emission per operation
- `app/src/test/java/fr/mandarine/todolist/TodoListsViewModelAnimationTest.kt` — 12 unit tests covering event emission per operation
- `app/src/test/java/fr/mandarine/todolist/TutorialViewModelAnimationSuppressionTest.kt` — 8 unit tests covering suppression flag per tutorial step

## Invariants & contracts
- `animationEvents` is a `SharedFlow`; the activity collects it and stores the result in `pendingEvent` on the animator **before** calling the adapter notify. The animator reads and clears `pendingEvent` in `animateChange`/`animateAdd`/`animateRemove`. If no pending event is set, the animator falls back to the default `DefaultItemAnimator` behaviour.
- `isReducedMotion()` gates all custom animation code; if animator scale is 0 the animator still runs its notify cycle but skips the `ViewPropertyAnimator` calls, so the RecyclerView layout remains correct.
- Tutorial suppression (`animationsSuppressed`) gates the animator via a `shouldAnimate` lambda passed at construction time; the lambda checks both reduced-motion and the tutorial flag so both conditions are tested independently.
- `TutorialViewModel.animationsSuppressed` is `true` only for `OPEN_LIST` and `COMPLETE_AND_REORDER` tutorial steps; all other steps (including steps on the lists screen) animate normally.
- Haptics on drag pickup and drop come from the `ItemTouchHelper` callback inside `TodoListActivity`, not from the adapter; they fire unconditionally (haptics ignore the reduced-motion gate by design).
- Haptic on the complete toggle fires in `TodoListAdapter` in both directions — completing (active → completed) and restoring (completed → active) — using the same constant (`CONFIRM` on API 30+, `CLOCK_TICK` below).
- No event is emitted for edit, reorder, or data-refresh operations; those paths continue to use the default animator with no custom motion.
- `ListAdded` is the only `AnimationEvent` variant used by `TodoListsItemAnimator`; the lists screen has no complete, restore, or delete animations beyond the default fade.

## UI
- **Screen(s)**: `TodoListsActivity` (Screen 1), `TodoListActivity` (Screen 2)
- **Layout file(s)**: no layout files changed
- **Design decisions**:
  - Complete/restore is a collapse-in-source + expand-in-destination pattern (no ghost view); the row shrinks at its old position while a fresh row grows at the new position.
  - Item add slides up from the inline-add row (positive translationY, so the row appears to rise into place from below).
  - List add slides down from the toolbar area (negative translationY, so the row descends into place from above).
  - Delete fades out and collapses height; no slide direction.
  - The completed-section divider fades/slides in as part of the same animation sequence as the first row that crosses it.
  - Removes are scheduled 200 ms before adds so the source row finishes collapsing before the destination row expands.
