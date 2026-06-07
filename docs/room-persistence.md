# Room Persistence

## What it does
All todo lists and items are stored in a Room SQLite database so data survives app restarts and back-navigation. In-memory repositories remain available for unit tests.

## Architecture
- **Layers**: data (exclusively)
- **Key types**:
  - `TodoDatabase` — Room `RoomDatabase`, version 2, singleton via `getInstance(context)`
  - `TodoListEntity` — Room entity, table `todo_lists`, `@PrimaryKey val id: String`
  - `TodoItemEntity` — Room entity, table `todo_items`, FK to `todo_lists` with `CASCADE` delete, index on `listId`; `completed: Boolean = false`
  - `TodoListDao` — `getAll`, `insert`, `deleteById`
  - `TodoItemDao` — `getAllByListId`, `getById`, `insert`, `updateCompleted(id, completed)`, `deleteAllByListId`
  - `RoomTodoListRepository` — implements `TodoListRepository` using `TodoListDao`
  - `RoomTodoRepository` — implements `TodoRepository` using `TodoItemDao`; `toggle()` reads current state via `getById` then calls `updateCompleted`
  - `MIGRATION_1_2` — adds `completed INTEGER NOT NULL DEFAULT 0` column to `todo_items`
- **Async contract**: synchronous (`allowMainThreadQueries()` enabled); no coroutine or Flow usage in the data layer

## Files
- `app/src/main/java/fr/mandarine/todolist/data/TodoDatabase.kt` — Room database definition, singleton factory, migration
- `app/src/main/java/fr/mandarine/todolist/data/TodoListEntity.kt` — list entity
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemEntity.kt` — item entity with FK and index
- `app/src/main/java/fr/mandarine/todolist/data/TodoListDao.kt` — DAO for lists
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemDao.kt` — DAO for items
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoListRepository.kt` — production repository for lists
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoRepository.kt` — production repository for items
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoListRepository.kt` — test-only in-memory implementation
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoRepository.kt` — test-only in-memory implementation

## Invariants & contracts
- `TodoDatabase` is a singleton; always obtain it via `TodoDatabase.getInstance(context)`.
- `allowMainThreadQueries()` is set for simplicity; do not introduce coroutine-based queries without removing this flag and updating all callers.
- `MIGRATION_1_2` must remain registered; dropping it will crash users upgrading from database version 1.
- `RoomTodoRepository.toggle()` does a read-then-write; this is not atomic — do not introduce concurrent writes without adding a transaction.
- In-memory repositories must not be used in production `Activity` code; they exist solely for unit tests.
- Room's `CASCADE` delete on `TodoItemEntity` is a database-level safety net; `DeleteTodoListUseCase` still calls `deleteAllByListId` explicitly at the domain layer.

## UI
None — this feature has no UI of its own. The repositories are wired into ViewModels inside the `Activity` constructors.
