# Lists Screen Watermark Icon

## What it does
Applies the same dual-layer background watermark treatment to the lists screen that the items screen already has: a permanent 200dp/alpha=0.15 `ic_format_list_bulleted` icon sits behind the RecyclerView at all times, and a second 120dp instance appears centred over the empty state. The icon is visually distinct from the `ic_checklist` used on the items screen.

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel changes)
- **Key types**: none introduced or changed
- **Async contract**: none

## Files
- `app/src/main/res/drawable/ic_format_list_bulleted.xml` — new single-path Material `format_list_bulleted` vector (bullet dots + three horizontal lines); vector-level `android:tint="?attr/colorOnSurfaceVariant"`; path fill `@android:color/white` overridden at render time by the tint
- `app/src/main/res/layout/activity_todo_lists.xml` — added permanent watermark `ImageView` (id `imageWatermark`, 200dp, `alpha="0.15"`) behind the RecyclerView inside the shared `FrameLayout`; updated the empty-state `ImageView` from `ic_checklist` to `ic_format_list_bulleted` at 120dp

## Invariants & contracts
- `imageWatermark` must always be visible regardless of list content — it is a permanent background decoration, not an empty-state element. Only `layoutEmptyLists` is toggled on empty/non-empty state.
- `ic_format_list_bulleted` is used exclusively on the lists screen; `ic_checklist` remains exclusively on the items screen. Do not swap or share these icons between screens.
- Both `ImageView`s in the lists layout must declare `app:tint="?attr/colorOnSurfaceVariant"` and `android:importantForAccessibility="no"`.
- The vector-level `android:tint` on `ic_format_list_bulleted.xml` must stay `?attr/colorOnSurfaceVariant`; do not hardcode a colour.

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: `res/layout/activity_todo_lists.xml`
- **Design decisions**: Mirrors the dual-layer pattern established in `activity_todo_list.xml` (→ see [UI Polish](ui-polish.md)). Using a semantically different icon (`format_list_bulleted` for a list-of-lists vs `ic_checklist` for individual items) reinforces the screen hierarchy at a glance without adding any text label.
