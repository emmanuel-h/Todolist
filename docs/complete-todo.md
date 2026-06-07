# Complete Todo

## What it does
The user taps a checkbox on any todo item to toggle its completed state; completed items are shown with strikethrough text and 50% alpha. The state is persisted in Room and survives navigation and restarts.

## Architecture
- **Layers**: domain, data, presentation, ui
- **Key types**:
  - `TodoItem.isCompleted: Boolean` — default `false`; the single source of truth for completion
  - `ToggleTodoUseCase` — `invoke(todoId: String)`; delegates to `TodoRepository.toggle()`
  - `TodoRepository.toggle(todoId)` — contract for flipping the completed flag
  - `RoomTodoRepository.toggle()` — fetches current entity via `getById`, then calls `updateCompleted` with the inverted value
  - `TodoItemDao.updateCompleted(id, completed)` — targeted `UPDATE` query; does not replace the full row
  - `MIGRATION_1_2` — adds the `completed` column to existing databases (→ see `room-persistence.md`)
  - `TodoListViewModel.toggleTodo(todoId)` — calls use case then refreshes `state`
- **Async contract**: synchronous; `toggleTodo` updates `StateFlow` synchronously after the repository write

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoItem.kt` — `isCompleted` field
- `app/src/main/java/fr/mandarine/todolist/domain/TodoRepository.kt` — `toggle(todoId)` in interface
- `app/src/main/java/fr/mandarine/todolist/domain/ToggleTodoUseCase.kt` — thin delegating use case
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemEntity.kt` — `completed: Boolean` persisted column
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemDao.kt` — `updateCompleted` and `getById` queries
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoRepository.kt` — read-then-write toggle implementation
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListViewModel.kt` — `toggleTodo` method
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — strikethrough + alpha rendering for completed items

## Invariants & contracts
- `ToggleTodoUseCase` does not accept a boolean target state; it always inverts — never call it twice in succession expecting a no-op.
- `RoomTodoRepository.toggle()` is a read-then-write; the two operations are not in a transaction — avoid concurrent toggles on the same item.
- The completed flag must be mapped from `TodoItemEntity.completed` to `TodoItem.isCompleted` on every read; do not short-circuit this mapping.
- Strikethrough and alpha are purely visual — they are applied in the adapter based on `isCompleted` and must not alter the underlying data.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: `res/layout/item_todo.xml`
- **Design decisions**: Completed items use `STRIKE_THRU_TEXT_FLAG` on the title `TextView` and `alpha = 0.5f`; no separate "completed section" — items stay in their original position in the list.
