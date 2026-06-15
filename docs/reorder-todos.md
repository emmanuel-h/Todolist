# Reorder Active Todo Items

## What it does
Active todo items can be drag-reordered within the active section using an always-visible drag handle icon. Dragging is blocked once the item reaches the last active row — it cannot cross into or past the completed section. Explicit position is persisted in Room and survives restarts and back-navigation.

## Architecture
- **Layers**: domain, data, presentation, UI
- **Key types**:
  - `TodoItem.position: Int` — zero-based sort index within the list; default `0`; only meaningful for active items
  - `TodoRepository.reorder(listId, fromIndex, toIndex)` — interface method that repositions active items within the list
  - `ReorderTodosUseCase` — `invoke(listId, fromIndex, toIndex)`: thin delegating use case
  - `TodoListViewModel.reorderTodos(fromIndex, toIndex)` — calls use case then rebuilds state
  - `MIGRATION_3_4` — adds `position INTEGER NOT NULL DEFAULT 0` column to `todo_items`
  - `TodoItemDao.updatePosition(id, position)` — targeted `UPDATE` query; `getAllByListId` now includes `ORDER BY position ASC`
  - `TodoListAdapter.activeItemCount()` — returns count of active (non-completed) rows only
  - `TodoListAdapter.moveItem(from, to)` — performs live visual reorder in the adapter list during drag, before the drop is committed
- **Async contract**: `reorder` is a `suspend` function; `reorderTodos` in the ViewModel is launched in `viewModelScope`; the adapter's live `moveItem` is synchronous and view-only

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoItem.kt` — added `position: Int = 0` field
- `app/src/main/java/fr/mandarine/todolist/domain/TodoRepository.kt` — added `reorder(listId, fromIndex, toIndex)` to interface
- `app/src/main/java/fr/mandarine/todolist/domain/ReorderTodosUseCase.kt` — new thin delegating use case
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemEntity.kt` — added `position: Int = 0` Room column
- `app/src/main/java/fr/mandarine/todolist/data/TodoItemDao.kt` — added `updatePosition(id, position)` query; `getAllByListId` now uses `ORDER BY position ASC`
- `app/src/main/java/fr/mandarine/todolist/data/TodoDatabase.kt` — bumped version 3→4, registered `MIGRATION_3_4`
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoRepository.kt` — implemented `reorder`; `getAllByListId` sorts by position
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoRepository.kt` — implemented `reorder`; maps `position` in `getAllByListId` and `add`
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListViewModel.kt` — added `reorderTodosUseCase` constructor param and `reorderTodos(fromIndex, toIndex)` method
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListActivity.kt` — passes `ReorderTodosUseCase` to ViewModel; attaches `ItemTouchHelper`; `onMove` clamps target to `[0, activeCount-1]`; `clearView` commits reorder to ViewModel on drop
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — exposes `dragHandle` view in `ItemViewHolder`; `VISIBLE` on active rows, `GONE` on completed rows; touch listener on handle calls `onStartDrag`; added `activeItemCount()` and `moveItem(from, to)`
- `app/src/main/res/drawable/ic_drag_handle.xml` — 24dp vector (two horizontal bars) for the drag handle icon
- `app/src/main/res/layout/item_todo.xml` — `ImageView` drag handle added as first (leftmost) child; 48×48dp touch target, 12dp internal padding, tinted `?attr/colorOnSurfaceVariant`; row start padding reduced from 16dp to 4dp
- `app/src/main/res/values/strings.xml` — added `drag_handle` string for handle `contentDescription`
- `app/src/test/java/fr/mandarine/todolist/domain/ReorderTodosUseCaseTest.kt` — 3 unit tests for use case
- `app/src/test/java/fr/mandarine/todolist/data/InMemoryTodoRepositoryReorderTest.kt` — 10 tests including out-of-order position sort test
- `app/src/test/java/fr/mandarine/todolist/data/RoomTodoRepositoryReorderTest.kt` — 8 tests including out-of-order position sort test
- `app/src/test/java/fr/mandarine/todolist/data/TodoItemEntityPositionTest.kt` — 3 tests for the new entity column
- `app/src/test/java/fr/mandarine/todolist/data/TodoDatabaseMigrationTest.kt` — added `MIGRATION_3_4` test
- `app/src/test/java/fr/mandarine/todolist/presentation/TodoListViewModelReorderTest.kt` — 4 ViewModel reorder tests
- `app/src/test/java/fr/mandarine/todolist/presentation/TodoListViewModelTest.kt` — updated to supply `reorderTodosUseCase` to ViewModel constructor
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListDragReorderTest.kt` — 10 Robolectric tests covering handle visibility, `activeItemCount`, `moveItem`, `ItemTouchHelper` attachment, and boundary enforcement

## Invariants & contracts
- `TodoItem.position` is only meaningful for active items (`completedAt == null`); completed items are ordered by `completedAt`, not `position`.
- `TodoRepository.reorder` operates only on active items for the given `listId`; it never touches completed items' positions.
- `getAllByListId` in both `InMemoryTodoRepository` and `TodoItemDao` returns items sorted by `position ASC`; the two implementations must remain consistent.
- `MIGRATION_3_4` must remain registered alongside `MIGRATION_1_2` and `MIGRATION_2_3`; removing it will crash users upgrading from earlier database versions.
- `isLongPressDragEnabled = false` on the `ItemTouchHelper` — drag only starts from the handle's `ACTION_DOWN` touch listener, never from a long-press.
- `getMovementFlags` in the `ItemTouchHelper` callback returns 0 for completed item rows, dividers, and inline-add rows; only active item rows are draggable.
- The target position is always clamped to `[0, activeItemCount() - 1]` in `onMove`; items cannot be dropped into or below the completed section.
- `clearView` is the single point where `viewModel.reorderTodos` is called; `moveItem` during drag is visual-only and does not persist.
- Drag handle is `GONE` on completed rows; after an item is uncompleted it returns to the active section and its handle becomes `VISIBLE` again.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: `res/layout/item_todo.xml`
- **Design decisions**: Handle uses `?attr/colorOnSurfaceVariant` tint to match M3 surface hierarchy. The 48dp touch target with 12dp internal padding meets M3 minimum touch-target guidelines. Row start padding reduced from 16dp to 4dp to accommodate the handle without increasing total row width. Drag boundary enforcement is purely in `ItemTouchHelper.onMove` via position clamping — no domain or ViewModel logic is needed for the boundary. `onSwiped` is a no-op override; swipe-to-delete remains handled by the existing mechanism (→ see edit-delete-todo.md).
