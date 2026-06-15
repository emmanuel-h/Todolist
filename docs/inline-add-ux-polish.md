# Inline-Add Row UX Polish

## What it does
Improves discoverability and visual consistency of the todo-list inline-add row: a 1dp top divider gives the ghost row a clear affordance, ghost-text alpha is raised from 45% to 70% for legibility, the expanded row no longer jumps in width when focused, and empty-state copy is updated to "Type above to start."

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel changes)
- **Key types**: no new types
- **Async contract**: none

## Files
- `app/src/main/res/layout/item_todo_inline_add.xml` — added 1dp `colorOutlineVariant` top divider; raised ghost text alpha to 0.70; removed `TextInputLayout` wrapper from expanded row, replaced with bare `TextInputEditText` (`background="@null"`, zero start/end padding) to eliminate width jump on focus
- `app/src/main/res/values/strings.xml` — `empty_todos_subheadline` updated to "Type above to start."; removed dead resources `empty_lists_message` and `empty_todos_message`

## Invariants & contracts
- The top divider on the inline-add row uses `?attr/colorOutlineVariant` to match the completed-section divider — do not replace it with a hardcoded colour.
- The expanded row must not use a `TextInputLayout` wrapper; keeping the bare `TextInputEditText` with `background="@null"` and zero padding is what prevents the width jump.
- Ghost text alpha must stay at 0.70 or above; do not lower it back toward 0.45.
- `empty_lists_message` and `empty_todos_message` have been removed — do not reintroduce them.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: `res/layout/item_todo_inline_add.xml`
- **Design decisions**: Option D (top divider only, no border or icon) was chosen as the minimal affordance that matches the existing completed-section visual language. Ghost row leading spacer (`@dimen/inline_add_leading_spacer`) and the expanded row's `paddingStart` are kept in sync so the text baselines align precisely across both states.
