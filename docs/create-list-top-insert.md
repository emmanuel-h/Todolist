# Create List — Insert at Top

## What it does
Newly created todo lists are inserted at position 0 (top of the home screen list), pushing all existing lists down by one position. Drag-to-reorder remains the only way to change order after creation.

## Architecture
- **Layers**: domain, data
- **Key types**:
  - `TodoListRepository.shiftAllPositionsUp()` — new interface method; increments every list's position by 1
  - `CreateTodoListUseCase` — changed: calls `shiftAllPositionsUp()` then `add()` with `position = 0` (previously appended at bottom using `repository.getAll().size`)
  - `TodoListDao.incrementAllPositions()` — new Room query: `UPDATE todo_lists SET position = position + 1`
  - `InMemoryTodoListRepository.shiftAllPositionsUp()` — iterates entries, increments each position by 1
  - `RoomTodoListRepository.shiftAllPositionsUp()` — delegates to `dao.incrementAllPositions()`
- **Async contract**: `shiftAllPositionsUp()` is a `suspend` function; `CreateTodoListUseCase.invoke()` calls it with `suspend` chaining; no Flow involved

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListRepository.kt` — added `shiftAllPositionsUp()` to interface
- `app/src/main/java/fr/mandarine/todolist/domain/CreateTodoListUseCase.kt` — replaced bottom-append logic with shift-then-insert-at-0
- `app/src/main/java/fr/mandarine/todolist/data/TodoListDao.kt` — added `incrementAllPositions()` Room query
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoListRepository.kt` — implemented `shiftAllPositionsUp()`
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoListRepository.kt` — implemented `shiftAllPositionsUp()` via DAO
- `app/src/test/java/fr/mandarine/todolist/domain/CreateTodoListUseCaseTopInsertTest.kt` — 4 tests for top-insert behaviour
- `app/src/test/java/fr/mandarine/todolist/data/InMemoryTodoListRepositoryShiftTest.kt` — 5 tests for in-memory shift
- `app/src/test/java/fr/mandarine/todolist/data/RoomTodoListRepositoryShiftTest.kt` — 1 test for Room delegation
- `app/src/test/java/fr/mandarine/todolist/data/TodoListDaoIncrementTest.kt` — 3 tests for the DAO query

## Invariants & contracts
- `shiftAllPositionsUp()` must be called before `add()` inside `CreateTodoListUseCase`; reversing the order would assign position 0 to the new list before shifting, causing a position collision.
- The new list is always created with `position = 0`; never derive position from `repository.getAll().size` (that was the old behaviour).
- No automatic reordering occurs on any other event (open, complete, rename, delete); position only changes on create (this feature) and drag-reorder (→ see `reorder-todo-lists.md`).
- `TodoListRepository.shiftAllPositionsUp()` must increment all existing entries; partial shifts would corrupt ordering.
- The `reorder-todo-lists.md` invariant that `getAll()` returns lists sorted by `position ASC` is unchanged and still required.
