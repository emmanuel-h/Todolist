# Inset Item Dividers

## What it does
Draws a 1dp inset divider line between consecutive todo item rows on the items screen (Screen 2), giving visual separation within each section without cluttering the area around the section-header row.

## Architecture
- **Layers**: ui only
- **Key types**:
  - `InsetItemDivider` — `RecyclerView.ItemDecoration` inner class of `TodoListActivity`; draws dividers via `onDraw` and reserves space via `getItemOffsets`
- **Async contract**: none — pure synchronous canvas drawing

## Files
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListActivity.kt` — added `InsetItemDivider` inner class; attached via `recyclerViewInternal.addItemDecoration(InsetItemDivider())`

## Invariants & contracts
- A divider is drawn below position `p` only when both `adapter.getItemViewType(p) == VIEW_TYPE_ITEM` and `adapter.getItemViewType(p + 1) == VIEW_TYPE_ITEM`; this ensures no divider appears immediately above or below the `VIEW_TYPE_DIVIDER` section header and no trailing divider appears after the last item in a section.
- The inset is hardcoded to 52dp (4dp `paddingStart` + 48dp drag-handle column); this value must stay in sync with the drag-handle width defined in `item_todo.xml` — see `drag-handle-alignment.md`.
- Divider color is resolved from `?attr/colorOutlineVariant` at construction time; never hardcode a color value here.
- `dividerHeightPx` is `coerceAtLeast(1)` so the line remains visible on low-density displays.

## UI
- **Screen(s)**: `TodoListActivity`
- **Layout file(s)**: none changed — decoration is applied programmatically; `item_todo_divider.xml` (the section-header row) is unchanged
- **Design decisions**: MD3 inset-divider pattern — the line starts where the item title text starts, skipping the drag-handle column, so the handle gutter stays visually clean. The line is drawn on the `onDraw` pass (below item content) and `getItemOffsets` reserves exactly `dividerHeightPx` bottom space so items do not overlap the line.
