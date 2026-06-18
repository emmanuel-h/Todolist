# List Item Count Badges

## What it does
Each list row on the lists screen shows two pill-shaped badges displaying the active and completed item counts as a breakdown (e.g. "3 / 2"). Lists with no items show "0 / 0".

## Architecture
- **Layers**: domain, presentation, ui
- **Key types**:
  - `TodoListSummary` — extended with `activeCount: Int` and `completedCount: Int` fields (alongside the existing `allDone: Boolean`)
  - `GetTodoListsWithStatusUseCase` — now also computes `activeCount` and `completedCount` per list via a plain for-loop over items
  - `TodoListsAdapter.ViewHolder` — gains `activeCountBadge` and `completedCountBadge` fields; `bind()` sets each badge text independently
- **Async contract**: synchronous; no change to the existing computed-property contract (→ see `all-done-list-status.md`)

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListSummary.kt` — added `activeCount` and `completedCount` fields
- `app/src/main/java/fr/mandarine/todolist/domain/GetTodoListsWithStatusUseCase.kt` — computes both counts alongside `allDone`
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsAdapter.kt` — `ViewHolder` binds `activeCountBadge` and `completedCountBadge`
- `app/src/main/res/drawable/badge_pill.xml` — pill-shaped shape drawable; tint applied at runtime
- `app/src/main/res/layout/item_todo_list.xml` — replaced single `textListCount` with a horizontal `LinearLayout` containing two `MaterialTextView` pill badges
- `app/src/test/java/fr/mandarine/todolist/domain/GetTodoListsWithStatusUseCaseCountTest.kt` — 8 unit tests for count computation
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsAdapterCountTest.kt` — 10 badge assertion tests

## Invariants & contracts
- `activeCount` + `completedCount` always equals the total item count for that list; never derive one from the other in the UI layer.
- An empty list must report `activeCount = 0` and `completedCount = 0`; no special-casing in the adapter.
- `GetTodoListsWithStatusUseCase` re-fetches items on every invocation; results must not be cached between mutations.
- Badge tints are resolved from the active theme at bind time: `activeCountBadge` uses `colorPrimaryContainer`/`colorOnPrimaryContainer`; `completedCountBadge` uses `colorSecondaryContainer`/`colorOnSecondaryContainer`. Never hardcode colors.

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: `res/layout/item_todo_list.xml`, `res/drawable/badge_pill.xml`
- **Design decisions**: Two distinct pill badges (primary container for active, secondary container for completed) communicate both counts and their semantic difference at a glance without any text labels.
