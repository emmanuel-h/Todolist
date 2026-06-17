# All-Done List Status

## What it does
When every item in a list is completed (and the list is non-empty), the row on the lists screen receives a `colorSecondaryContainer` card background and strikethrough on the list name. When any item is incomplete — or the list is empty — the row reverts to `colorSurface` with no strikethrough.

## Architecture
- **Layers**: domain, presentation, ui
- **Key types**:
  - `TodoListSummary(list: TodoList, allDone: Boolean)` — domain model pairing a list with its computed status
  - `GetTodoListsWithStatusUseCase` — computes `allDone` per list: non-empty and every `TodoItem.isCompleted` is `true`
  - `TodoListsState.Content(summaries: List<TodoListSummary>)` — replaces the former `List<TodoList>` payload
  - `TodoListsViewModel` — now injects `GetTodoListsWithStatusUseCase` instead of `GetTodoListsUseCase`
  - `TodoListsAdapter` — consumes `List<TodoListSummary>`; `applyAllDoneStyle(allDone)` sets card background and paint flags
- **Async contract**: synchronous; `state` remains a plain computed property re-evaluated on each access (→ see `multiple-lists.md`)

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListSummary.kt` — new domain model
- `app/src/main/java/fr/mandarine/todolist/domain/GetTodoListsWithStatusUseCase.kt` — computes allDone flag per list
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListsState.kt` — `Content` now holds `List<TodoListSummary>`
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListsViewModel.kt` — injects `GetTodoListsWithStatusUseCase`
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsAdapter.kt` — `applyAllDoneStyle()` resolves theme attributes and toggles paint flags
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — passes `summaries` from `Content` state to adapter

## Invariants & contracts
- `allDone` is `false` for an empty list — a list with no items must never show the all-done treatment.
- `GetTodoListsWithStatusUseCase` calls `todoRepository.getAllByListId` for every list on each invocation; do not cache results between mutations.
- Colors are resolved from the active theme via `TypedValue` at bind time — never hardcode color values for the all-done state.
- `TodoListsState.Content.summaries` replaces the old `lists: List<TodoList>` field; any code consuming `Content` must use `.summaries`, not `.lists`.

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: `res/layout/item_todo_list.xml`
- **Design decisions**: Background tint uses `?attr/colorSecondaryContainer` resolved via `TypedValue`; normal background uses `?attr/colorSurface`. Strikethrough is toggled with `Paint.STRIKE_THRU_TEXT_FLAG` on the name `TextView`. No badge or checkmark icon is added.
