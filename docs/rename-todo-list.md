# Rename Todo List

## What it does
The user can rename any list from the home screen by tapping the dimmed pencil icon to the left of the list name, which opens a pre-filled rename dialog. Confirming with a non-blank name persists the change; confirming with a blank name or cancelling dismisses the dialog and leaves the original name unchanged.

## Architecture
- **Layers**: domain, data, presentation, ui
- **Key types**:
  - `EditTodoListUseCase` — validates name is non-blank with `require()`, delegates to `TodoListRepository.updateName(todoListId, name)`
  - `TodoListRepository.updateName(todoListId, name)` — new method on the repository interface
  - `TodoListsViewModel.editList(todoListId, newName)` — blank name → no-op; otherwise invokes use case then refreshes state
- **Async contract**: synchronous; repository write happens inline, state is recomputed after the call

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListRepository.kt` — added `updateName(todoListId, name)`
- `app/src/main/java/fr/mandarine/todolist/domain/EditTodoListUseCase.kt` — new use case; enforces non-blank name
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListsViewModel.kt` — added `EditTodoListUseCase` constructor param and `editList()` method
- `app/src/main/java/fr/mandarine/todolist/data/TodoListDao.kt` — added `updateName(id, name)` Room `@Query`
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoListRepository.kt` — implemented `updateName` via index lookup and `copy()`
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoListRepository.kt` — implemented `updateName` delegating to DAO
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — wires `EditTodoListUseCase` into ViewModel construction; `showRenameDialog()` pre-fills name, calls `viewModel.editList()` on confirm, discards on blank/cancel
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsAdapter.kt` — added `onRenameClick` callback; only `btnEditList` click invokes it
- `app/src/main/res/layout/item_todo_list.xml` — added `btnEditList` (pencil, 38% alpha) to the left of the list name; delete button remains on the right
- `app/src/main/res/layout/dialog_rename_list.xml` — new rename dialog with pre-fillable `TextInputEditText` and icon-only confirm/cancel buttons
- `app/src/main/res/values/strings.xml` — added `edit_list` and `rename_list` string resources

## Invariants & contracts
- `EditTodoListUseCase` validates the name with `require(name.isNotBlank())`; `TodoListsViewModel.editList()` additionally guards with a blank-name no-op so the use case is never called with a blank string.
- Only `btnEditList` (the pencil icon) opens the rename dialog. Tapping the list name text and long-pressing the row do NOT trigger rename.
- The rename dialog pre-fills the current list name on every open; do not leave the field empty.
- A blank confirmation (user clears the field and taps Save) must discard the edit and dismiss the dialog — the original name must remain unchanged.
- `TodoList.id` is generated upstream by `CreateTodoListUseCase`; `editList()` takes an existing id from the repository, never a new one.
- The pencil icon alpha is 38% to appear dimmed; do not use full opacity or a different tint colour.

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: `res/layout/item_todo_list.xml`, `res/layout/dialog_rename_list.xml`
- **Design decisions**: The pencil icon is placed to the left of the list name and dimmed (38% alpha) so it is discoverable but visually subordinate to the list name. Dialog uses icon-only confirm/cancel buttons consistent with the icon-only UI direction (→ see `icon-only-ui.md`). Blank-name confirm silently discards rather than showing a validation error, keeping the interaction lightweight.
