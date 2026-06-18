# Add Item — Insert at Bottom

## What it does
Newly created todo items are appended at the bottom of the active section (position = count of existing active items). The inline-add input bar is pinned at the bottom of the screen, below the RecyclerView. When the list is empty the checklist illustration icon is centred in the FrameLayout behind the RecyclerView and remains visible through the transparent RecyclerView.

## Architecture
- **Layers**: domain, ui
- **Key types**:
  - `AddTodoUseCase` — changed: queries active item count from `repository.getAllByListId`, assigns `position = activeCount` before calling `repository.add`
  - `TodoItem.position` — already existed; now set to `activeCount` at creation instead of defaulting to `0`
- **Async contract**: none — synchronous; position is computed from a list snapshot before `repository.add` is called

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/AddTodoUseCase.kt` — replaced direct `TodoItem` creation with position-aware creation: `filter { !it.isCompleted }.size` determines the new item's position
- `app/src/test/java/fr/mandarine/todolist/domain/AddTodoUseCaseTest.kt` — added 4 tests for bottom-insert position assignment and `getAllByListId` call ordering
- `app/src/test/java/fr/mandarine/todolist/domain/AddTodoUseCaseBottomInsertTest.kt` — dedicated test class (5 tests) mirroring the `CreateTodoListUseCaseTopInsertTest` pattern
- `app/src/main/res/layout/activity_todo_list.xml` — moved `<include id="inlineAddRow">` from above the FrameLayout to below it inside the vertical `LinearLayout`, pinning the add row at the bottom of the screen
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListActivityTest.kt` — test renamed from "pinned above recycler view" to "pinned below recycler view"
- `docs/SPEC.md` — corrected "Create a list" and "Reorder lists" entries: lists insert at top, items insert at bottom

## Invariants & contracts
- `AddTodoUseCase` calls `repository.getAllByListId(listId)` before `repository.add(item)`; reversing this order would assign an incorrect position.
- Only active (non-completed) items are counted when computing position; completed items are irrelevant to the active section's ordering.
- The new item's position equals the count of active items before insertion, making it the last active item after insertion.
- `repository.getAllByListId` returns items sorted by `position ASC`; the new item's position guarantees it appears after all existing active items.
- No migration is needed: `TodoItem.position` already existed as of database version 4.
- The inline-add row must remain the last child of the `LinearLayout` in `activity_todo_list.xml`; placing it before the FrameLayout would move it above the list.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: `res/layout/activity_todo_list.xml`
- **Design decisions**: the `<include id="inlineAddRow">` sits as the last child of the vertical `LinearLayout` (after the `FrameLayout` that holds the RecyclerView + watermark + empty-state icon), so it is always anchored to the bottom of the screen. The empty-state checklist icon is centred inside the same `FrameLayout` and shows through the RecyclerView when the list is empty — no separate visibility toggle is required.
