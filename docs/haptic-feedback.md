# Haptic Feedback

## What it does
Three moments buzz: picking a row up by its drag handle, dropping it somewhere new, and crossing an item between the active and completed sections. Nothing else in the app produces haptics.

The complete/restore tick lived on the toggle button in `TodoListAdapter` and was lost when that adapter was deleted in migration phase 3 ([items-screen-compose.md](items-screen-compose.md)); the drag pickup and drop ticks survived in `ReorderHandle`. This restores the third and moves all three onto one pair of helpers.

## Architecture
- **Layers**: UI only. Haptics are feedback on a gesture, so they fire at the call site of the gesture, not from the ViewModel — nothing in `presentation/` knows about them.
- **Key types**:
  - `View.performPickUpFeedback()` — `HapticFeedbackConstants.LONG_PRESS`
  - `View.performConfirmFeedback()` — `HapticFeedbackConstants.CONFIRM` on API 30+, `CLOCK_TICK` below
- Both are extensions on `View` rather than calls through `LocalHapticFeedback`. `HapticFeedbackType.Confirm` does exist in this Compose version, but it resolves to `HapticFeedbackConstants.CONFIRM`, which the platform only knows from API 30 — below that the call is a silent no-op, and minSdk here is 24. The fallback has to be spelled out, so the constant is chosen at the call site.

## Files
- `ui/paper/PaperHaptics.kt` — the two helpers, the only place the constants and the API level check appear
- `ui/reorder/ReorderHandle.kt` — `performPickUpFeedback()` on `onDragStart`, `performConfirmFeedback()` on `onDragEnd` when the drag actually moved the row
- `ui/todolist/TodoListScreen.kt` — wraps the screen's `onToggle` in `performConfirmFeedback()` once, so the toggle button and the double-tap on the row title both buzz, in both directions
- `app/src/test/.../ui/paper/PaperHapticsTest.kt` — the constant each helper fires, including the pre-API-30 fallback under `@Config(sdk = [29])`
- `app/src/test/.../ui/todolist/TodoListScreenTest.kt` — complete, restore, double-tap, and a delete tap asserting silence; all read `ShadowView.lastHapticFeedbackPerformed()` on `LocalView.current`

## Invariants & contracts
- The toggle haptic is wrapped around the screen's single `onToggle` lambda, not put inside `TodoRow` — `TodoRow` has two paths to the same action and would otherwise need the call twice.
- Haptics ignore the reduced-motion gate (`TodoListScreenState.animationsEnabled`) by design; an animator scale of 0 silences motion, not touch feedback.
- The tutorial's phantom hand calls `viewModel.toggleTodo` directly from `TodoListActivity`, so a scripted toggle does not buzz. Under the View system it went through `performClick` and did.
- The lists screen has no complete action; its only haptics are pickup and drop, through the same `ReorderHandle`.
