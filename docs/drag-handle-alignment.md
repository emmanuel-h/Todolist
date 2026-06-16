# Drag Handle Alignment Fix (Completed Rows)

## What it does
Completed item text and action icons are horizontally aligned with those in the active section. The drag handle space is preserved on completed rows even though the handle icon is not visible, so neither column shifts.

## Architecture
- **Layers**: UI only (no domain, data, or ViewModel changes)
- **Key types**: `TodoListAdapter` — binding logic for `ItemViewHolder`
- **Async contract**: none

## Files
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — `View.GONE` replaced with `View.INVISIBLE` on the drag handle for completed rows, so the 48dp reserved space is kept
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListDragReorderTest.kt` — updated `should hide drag handle on completed item row` assertion from `GONE` to `INVISIBLE`; added `should keep drag handle space on completed row to align with active rows` verifying both visibility state and identical handle width across active and completed rows

## Invariants & contracts
- The drag handle on completed rows must remain `INVISIBLE`, not `GONE` — reverting to `GONE` collapses the 48dp space and shifts the completed section text 48dp left of the active section.
- No touch listener is registered on the handle in the completed-item branch, so the `INVISIBLE` handle is inert and cannot initiate a drag.
- `getMovementFlags` in the `ItemTouchHelper` callback still returns 0 for completed rows (→ see reorder-todos.md); `INVISIBLE` on the view does not re-enable dragging.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: `res/layout/item_todo.xml` (unchanged — fix is entirely in the adapter binding)
- **Design decisions**: `INVISIBLE` rather than any layout change (e.g. a separate completed-row layout) keeps the item layout file single and avoids duplicating the row structure.
