# Completed Section

## What it does
When the user completes a todo item it moves immediately to a dedicated "Completed (N)" section at the bottom of the list, ordered by completion time ascending (most recently completed last). Uncompleting an item removes the timestamp and moves the item to the bottom of the active section with no memory of its original position. The labeled divider is only visible when both sections are non-empty.

## Architecture
- **Layers**: domain, data, presentation, ui
- **Key types**:
  - `Clock` — `fun interface` with `now(): Long`; `SystemClock` is the production singleton returning `System.currentTimeMillis()`
  - `TodoItem.completedAt: Long?` — `null` means active; epoch-ms value records when the item was completed
  - `TodoItemEntity.completedAt: Long?` — nullable Room column mirroring the domain field
  - `TodoItemDao.updateCompleted(id, completedAt: Long?)` — replaces the old boolean-only signature; `null` clears completion
  - `MIGRATION_2_3` — adds `completedAt INTEGER` (nullable) column to `todo_items`
  - `TodoListState.Content` — now holds `activeItems: List<TodoItem>` and `completedItems: List<TodoItem>` instead of a flat `items` list
  - `TodoListViewModel.buildState()` — partitions items into active/completed and sorts completed by `completedAt` ascending
  - `TodoListAdapter.ListRow` — sealed class with `Item`, `Divider`, and `InlineAdd` subtypes; replaces the old flat item model
- **Async contract**: synchronous; `buildState()` partitions and sorts in memory after each repository write, then pushes to `StateFlow`

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/Clock.kt` — `Clock` fun interface + `SystemClock` production implementation
- `app/src/main/java/fr/mandarine/todolist/domain/TodoItem.kt` — added `completedAt: Long?` field (replaces `isCompleted: Boolean` as completion signal)
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemEntity.kt` — added `completedAt: Long?` Room column
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemDao.kt` — `updateCompleted` now accepts `completedAt: Long?`
- `app/src/main/java/fr/mandarine/todolist/data/TodoDatabase.kt` — bumped version 2→3, registered `MIGRATION_2_3`
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoRepository.kt` — injected `Clock`; stores timestamp on complete, clears on uncomplete
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoRepository.kt` — same `Clock` injection and timestamp logic
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListState.kt` — `Content` split into `activeItems` + `completedItems`
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListViewModel.kt` — `buildState()` partitions and sorts items
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListActivity.kt` — `renderState` passes `activeItems`/`completedItems` separately to adapter
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — `ListRow` sealed class, `VIEW_TYPE_DIVIDER`, `DividerViewHolder`, `submitList(activeItems, completedItems)`
- `app/src/main/res/layout/item_todo_divider.xml` — labeled divider row with two flanking lines and centered label using M3 theme attributes
- `app/src/main/res/values/strings.xml` — added `completed_section_header` format string `"Completed (%d)"`

## Invariants & contracts
- `TodoItem.completedAt == null` is the sole signal for an active item; the old `isCompleted: Boolean` field is gone — do not reintroduce it.
- `TodoItemDao.updateCompleted` accepts `Long?`; passing `null` is the correct way to uncomplete an item — do not pass `0L`.
- `MIGRATION_2_3` must remain registered alongside `MIGRATION_1_2` and `MIGRATION_2_3`; removing either migration will crash users upgrading from older database versions.
- `Clock` must be injected into both repository implementations; never call `System.currentTimeMillis()` directly inside a repository or ViewModel.
- `buildState()` in the ViewModel sorts completed items by `completedAt` ascending; uncompleting sets `completedAt` to `null`, so the item has no position memory when re-added to active.
- The divider row is inserted by `TodoListAdapter.submitList` only when both `activeItems` and `completedItems` are non-empty; do not insert it unconditionally.
- `TodoListAdapter.submitList(activeItems, completedItems)` replaces the old single-list overload; callers must not use the old signature.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: `res/layout/item_todo_divider.xml` (new), `res/layout/item_todo.xml`, `res/layout/item_todo_inline_add.xml`
- **Design decisions**: The divider uses two horizontal lines flanking a centered label (M3 `colorOutline` / `colorOnSurfaceVariant`) rather than a plain section header, consistent with M3 list divider patterns. The count in "Completed (N)" reflects the number of completed items passed to the adapter, not a ViewModel counter.
