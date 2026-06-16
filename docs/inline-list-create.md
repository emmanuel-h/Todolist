# Inline List Create

## What it does
Replaces the modal dialog for new-list creation with an ephemeral inline entry row on Screen 1. Tapping the FAB shows a tinted row pinned at the top of the content area; the FAB hides and the empty-state illustration hides while the row is active; confirm or cancel restores both. No modal dialog appears at any point.

## Architecture
- **Layers**: presentation, ui
- **Key types**:
  - `TodoListsViewModel.submitInlineInput(name: String): Boolean` — returns `false` for blank input, delegates to `CreateTodoListUseCase`, returns `true` on success
- **Async contract**: none; `submitInlineInput` is synchronous

## Files
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListsViewModel.kt` — added `submitInlineInput(name: String): Boolean`
- `app/src/main/res/layout/item_todo_list_inline_add.xml` — inline entry row: cancel `ImageButton` + bare `TextInputEditText` + confirm `ImageButton`; `?attr/colorSurfaceContainerHigh` background; 1dp `?attr/colorOutlineVariant` top and bottom dividers; cancel icon tinted `colorPrimary`
- `app/src/main/res/layout/activity_todo_lists.xml` — inline add row included at the top of the content area; `layoutEmptyLists` contains only the checklist icon (no text, per icon-only design principle)
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — replaced modal dialog UX with inline row wiring: FAB tap shows row + hides FAB + hides empty state; confirm/cancel restores FAB and empty-state visibility; removed `showCreateListDialog`, `openCreateDialogForTest`, `typeInCurrentDialogForTest`; added `tapFab`, `typeInInlineRowForTest`, `submitInlineRowForTest`, `cancelInlineRowForTest`, `inlineAddRowInternal`
- `app/src/test/java/fr/mandarine/todolist/presentation/TodoListsViewModelInlineAddTest.kt` — 5 unit tests for `submitInlineInput`
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsInlineAddTest.kt` — 14 tests covering row visibility, FAB visibility, list creation, blank rejection, cancel, empty-state hide/restore
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsActivityTest.kt` — all create-list helpers migrated from dialog to inline row
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsDragReorderTest.kt` — `createListViaDialog` helper updated to use inline row

## Invariants & contracts
- The modal dialog (`dialog_create_list.xml`) and all associated helpers (`openCreateDialogForTest`, `typeInCurrentDialogForTest`, `showCreateListDialog`) are removed; do not reintroduce them.
- The FAB must be hidden while the inline row is focused and must reappear after confirm or cancel; never show both simultaneously.
- The empty-state illustration must be hidden while the inline row is active and must reappear (when the list is still empty) after cancel.
- `submitInlineInput` must return `false` without mutating state when given a blank string; the row stays open in that case.
- Do not add a toolbar (`AppBarLayout` / `MaterialToolbar` / `setSupportActionBar`) to `TodoListsActivity` — Screen 1 has no title bar; icon-only design principle applies.
- Do not add text views to `layoutEmptyLists` — the empty state shows only the checklist icon; icon-only design principle applies.
- The text field hint uses `"…"` (not a word); do not replace it with a label string.
- The inline row background uses `?attr/colorSurfaceContainerHigh`; do not replace it with a hardcoded colour.
- Button layout within the row is fixed as cancel-left, confirm-right; do not swap them.
- `dialog_delete_list.xml` (icon-only delete confirmation) is unaffected — only list creation was changed.

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: `res/layout/activity_todo_lists.xml`, `res/layout/item_todo_list_inline_add.xml`
- **Design decisions**: Row style mirrors the Screen 2 inline-add pattern (→ see `inline-add-ux-polish.md`): `colorSurfaceContainerHigh` background, `colorPrimary` icon tint, 1dp `colorOutlineVariant` dividers above and below. Empty state shows the checklist icon only (icon-only design principle). Text field hint uses `"…"` rather than a word.
