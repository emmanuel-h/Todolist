# Reorder Todo Lists

## What it does
Todo lists on the home screen can be drag-reordered using an always-visible drag handle icon on each row. Dragging starts only from the handle (no long-press). Order is persisted in Room and survives restarts.

## Architecture
- **Layers**: domain, data, presentation, UI
- **Key types**:
  - `TodoList.position: Int` — zero-based sort index; default `0`; appended at bottom on creation
  - `TodoListRepository.reorder(fromIndex, toIndex)` — new interface method that repositions all lists
  - `ReorderTodoListsUseCase` — `invoke(fromIndex, toIndex)`: thin delegating use case
  - `TodoListsViewModel.reorderLists(fromIndex, toIndex)` — launches use case in `viewModelScope`
  - `MIGRATION_4_5` — adds `position INTEGER NOT NULL DEFAULT 0` column to `todo_lists`
  - `TodoListDao.updatePosition(id, position)` — targeted `UPDATE` query; `getAll()` now uses `ORDER BY position ASC`
  - `TodoListsAdapter.moveItem(from, to)` — live visual reorder during drag, before drop is committed
- **Async contract**: `reorder` is a `suspend` function; `reorderLists` in the ViewModel is launched in `viewModelScope`; `moveItem` during drag is synchronous and view-only

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoList.kt` — added `position: Int = 0` field
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListRepository.kt` — added `reorder(fromIndex, toIndex)` to interface
- `app/src/main/java/fr/mandarine/todolist/domain/ReorderTodoListsUseCase.kt` — new thin delegating use case
- `app/src/main/java/fr/mandarine/todolist/domain/CreateTodoListUseCase.kt` — assigns `position = repository.getAll().size` so new lists append at bottom
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListsViewModel.kt` — added `reorderTodoListsUseCase` constructor param and `reorderLists(fromIndex, toIndex)` method
- `app/src/main/java/fr/mandarine/todolist/data/TodoListEntity.kt` — added `position: Int = 0` Room column
- `app/src/main/java/fr/mandarine/todolist/data/TodoListDao.kt` — added `updatePosition(id, position)` query; `getAll()` now uses `ORDER BY position ASC`
- `app/src/main/java/fr/mandarine/todolist/data/TodoDatabase.kt` — bumped version 4→5, added `MIGRATION_4_5`
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoListRepository.kt` — implemented `reorder`; `getAll()` sorts by position
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoListRepository.kt` — implemented `reorder`; maps `position` in `getAll()` and `add()`
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsAdapter.kt` — added `onDragStart` callback, `dragHandle` ImageView in ViewHolder, `moveItem()` method
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — wires `ReorderTodoListsUseCase`, `ItemTouchHelper` with handle-only drag; long-press disabled on card
- `app/src/main/res/layout/item_todo_list.xml` — added `dragHandleList` ImageView (48×48dp, `ic_drag_handle`, colorOnSurfaceVariant); `android:longClickable="false"` on card; 48dp touch targets on all icon buttons

## Invariants & contracts
- `TodoList.position` is the sole ordering mechanism; `getAll()` in both `InMemoryTodoListRepository` and `TodoListDao` must return lists sorted by `position ASC`.
- `CreateTodoListUseCase` sets `position = repository.getAll().size` at insert time so new lists always appear at the bottom; never pass a hardcoded position from the UI.
- `MIGRATION_4_5` must remain registered alongside all prior migrations; removing it will crash users upgrading from earlier database versions.
- `isLongPressDragEnabled = false` on the `ItemTouchHelper` — drag only starts from the handle's `ACTION_DOWN` touch listener, never from a long-press.
- Long-press on a list row does nothing; the pencil icon (→ see `rename-todo-list.md`) is the sole way to rename a list.
- `clearView` is the single point where `viewModel.reorderLists` is called; `moveItem` during drag is visual-only and does not persist.

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: `res/layout/item_todo_list.xml`
- **Design decisions**: Drag handle pattern mirrors the todo-item drag handle from issue #5 (→ see `reorder-todos.md`): same `ic_drag_handle` vector, `?attr/colorOnSurfaceVariant` tint, 48dp touch target. `android:longClickable="false"` on the card removes the long-press-to-rename behaviour introduced in issue #4 — the pencil icon is now the exclusive rename entry point.
