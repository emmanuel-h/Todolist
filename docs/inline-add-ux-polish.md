# Inline-Add Row UX Polish

## What it does
Improves discoverability and visual consistency of the todo-list inline-add row: a 1dp top divider gives the ghost row a clear affordance, ghost-text alpha is raised from 45% to 70% for legibility, the expanded row no longer jumps in width when focused, and empty-state copy is updated to "Type above to start." The add row is now pinned permanently below the toolbar rather than being the last item in the RecyclerView.

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel changes)
- **Key types**: no new types
- **Async contract**: none

## Files
- `app/src/main/res/layout/item_todo_inline_add.xml` — added 1dp `colorOutlineVariant` top divider; raised ghost text alpha to 0.70; removed `TextInputLayout` wrapper from expanded row, replaced with bare `TextInputEditText` (`background="@null"`, zero start/end padding) to eliminate width jump on focus
- `app/src/main/res/layout/activity_todo_list.xml` — replaced flat `FrameLayout` with a vertical `LinearLayout`; inline-add row pinned via `<include id="@+id/inlineAddRow">` immediately below the `AppBarLayout`; `RecyclerView` + empty-state `FrameLayout` fill the remaining space below
- `app/src/main/res/values/strings.xml` — `empty_todos_subheadline` updated to "Type above to start."; removed dead resources `empty_lists_message` and `empty_todos_message`
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListActivity.kt` — `wireInlineAddRow()` wires ghost/expanded row, editText, submit button, and focus/IME logic directly in the Activity; exposes `inlineAddEditTextInternal` for tests
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — removed `ListRow.InlineAdd`, `VIEW_TYPE_ADD`, `AddInputViewHolder`, and `onSubmit` constructor param; `buildRows` no longer appends an add row; adapter `itemCount` reflects only todo items and the optional divider
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListActivityTest.kt` — helpers updated to use `activity.inlineAddEditTextInternal`; `itemCount` assertions corrected (empty=0, one=1, three=3); added tests confirming pinned row is always visible and accessible
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListAdapterTest.kt` — removed all `VIEW_TYPE_ADD` / `AddInputViewHolder` / `onSubmit` tests; `itemCount` expectations corrected; `onSubmit` removed from adapter constructor call
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListDragReorderTest.kt` — `addItem` helper updated to use `activity.inlineAddEditTextInternal` instead of `rv.getChildAt(rv.childCount - 1)`
- `app/src/test/java/fr/mandarine/todolist/ui/IconOnlyUiTest.kt` — removed `onSubmit` from `TodoListAdapter` constructor call

## Invariants & contracts
- The top divider on the inline-add row uses `?attr/colorOutlineVariant` to match the completed-section divider — do not replace it with a hardcoded colour.
- The expanded row must not use a `TextInputLayout` wrapper; keeping the bare `TextInputEditText` with `background="@null"` and zero padding is what prevents the width jump.
- Ghost text alpha must stay at 0.70 or above; do not lower it back toward 0.45.
- `empty_lists_message` and `empty_todos_message` have been removed — do not reintroduce them.
- The inline-add row must never be a RecyclerView item — it is always a static view pinned at the top of the content area. Do not re-add `ListRow.InlineAdd` or `VIEW_TYPE_ADD` to the adapter.
- `TodoListAdapter` does not take an `onSubmit` parameter. Submission is handled entirely in `TodoListActivity.wireInlineAddRow()`.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: `res/layout/item_todo_inline_add.xml`, `res/layout/activity_todo_list.xml`
- **Design decisions**: Option A (top anchor) — the add row is pinned permanently below the toolbar. Active items and completed items both scroll beneath it in the RecyclerView. Option D visual style (top divider only, no border or icon) retained from the previous polish pass. Ghost row leading spacer (`@dimen/inline_add_leading_spacer`) and the expanded row's `paddingStart` are kept in sync so the text baselines align precisely across both states.
