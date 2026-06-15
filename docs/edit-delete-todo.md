# Edit and Delete Todo

## What it does
The user can delete any todo item immediately (no confirmation) by tapping the red delete button, or rename it inline by tapping the pencil edit button — the title becomes an editable field in place, confirmed with IME Done or focus loss. Double-tapping the title text also enters inline edit mode. Blank titles discard the edit and revert to the original text. The toggle button shows a check icon for active items and an undo arrow for completed items.

## Architecture
- **Layers**: domain, data, presentation, ui
- **Key types**:
  - `DeleteTodoUseCase` — `invoke(todoId)`: delegates `repository.delete(todoId)`
  - `EditTodoUseCase` — `invoke(todoId, newTitle)`: validates non-blank title with `require()`, then delegates `repository.updateTitle(todoId, newTitle)`
  - `TodoRepository.delete(todoId)` / `TodoRepository.updateTitle(todoId, title)` — new interface methods (→ see `todo-list-screen.md`)
  - `TodoListViewModel.deleteTodo(todoId)` / `TodoListViewModel.editTodo(todoId, newTitle)` — call use cases then rebuild state
  - `TodoListAdapter.ItemViewHolder` — holds `editTitleInline: TextInputEditText` (hidden by default); toggled visible on edit; `onDelete` and `onEdit` callbacks wired from activity
- **Async contract**: synchronous; each mutating call rebuilds `StateFlow` immediately after the repository write

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoRepository.kt` — added `delete(todoId)` and `updateTitle(todoId, title)`
- `app/src/main/java/fr/mandarine/todolist/domain/DeleteTodoUseCase.kt` — thin delegating use case
- `app/src/main/java/fr/mandarine/todolist/domain/EditTodoUseCase.kt` — validates non-blank title, delegates to repository
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemDao.kt` — added `deleteById(id)` and `updateTitle(id, title)` Room queries
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoRepository.kt` — implemented `delete` and `updateTitle` via DAO
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoRepository.kt` — implemented `delete` and `updateTitle` in-memory
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListViewModel.kt` — added `deleteTodo` and `editTodo` methods
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — inline edit flow: tapping [✎] hides `textTitle`, shows `editTitleInline`; IME Done or focus loss commits or reverts; `ic_undo` icon on toggle button for completed items; double-tap gesture on `textTitle` only
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListActivity.kt` — wires `onDelete` and `onEdit` callbacks directly to ViewModel; removed AlertDialog edit flow
- `app/src/main/res/layout/item_todo.xml` — added hidden `editTitleInline` (`TextInputEditText`); `?attr/colorError` tint on delete button
- `app/src/main/res/layout/dialog_edit_item.xml` — retained in repo but no longer wired to any user flow
- `app/src/main/res/drawable/ic_check.xml` — check icon for complete toggle button
- `app/src/main/res/drawable/ic_undo.xml` — undo/restore icon for uncomplete toggle button
- `app/src/main/res/drawable/ic_edit.xml` — pencil icon for edit button
- `app/src/main/res/values/strings.xml` — added `item_edit`, `item_delete`, `edit_item_title`, `save` strings

## Invariants & contracts
- `EditTodoUseCase` validates the title with `require(title.isNotBlank())`; callers must never pass a blank title — the adapter silently discards blank input before invoking the callback.
- The inline `editTitleInline` field is hidden by default and made visible only during an active edit session; restoring visibility of `textTitle` and hiding `editTitleInline` must happen on both commit and revert paths.
- Double-tap detection is attached exclusively to `textTitle`; the button strip is excluded from the gesture zone.
- The delete button tint is `?attr/colorError`; do not use a hardcoded red colour.
- The toggle button icon switches between `ic_check` (active item) and `ic_undo` (completed item) based on whether `completedAt == null`; no third state exists.
- `dialog_edit_item.xml` must not be wired to new call sites (→ see `ui-polish.md` for the same rule about `dialog_add_item.xml`).
- Delete is immediate with no confirmation; do not add an undo snackbar or dialog without a spec update.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: `res/layout/item_todo.xml`
- **Design decisions**: Inline editing avoids a dialog modal, keeping the interaction lightweight. The `editTitleInline` field is toggled visible in-place so the item row does not shift height. `colorError` on the delete button provides a universal affordance for irreversible actions without custom theming. The double-tap zone is intentionally limited to the title text to prevent accidental edit triggers from tapping action buttons.
