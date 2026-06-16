# Empty-State Icon Fix

## What it does
Replaces the broken `ic_checklist.xml` vector drawable — which combined three unrelated paths (clipboard outline, standalone checkmark, redirect arrow) all hardcoded white — with a coherent two-path Material `assignment_turned_in`-style clipboard-with-checkmark icon tinted via `?attr/colorOnSurfaceVariant`.

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel changes)
- **Key types**: none introduced or changed
- **Async contract**: none

## Files
- `app/src/main/res/drawable/ic_checklist.xml` — redrawn as a coherent two-path clipboard-with-checkmark vector; vector-level `android:tint="?attr/colorOnSurfaceVariant"`; path fill kept as `@android:color/white` (overridden at render time by the vector tint and the `app:tint` on each ImageView)

## Invariants & contracts
- `ic_checklist.xml` must remain a two-path shape: one path for the large checkmark, one for the clipboard body with centre pin.
- The vector-level `android:tint` must stay `?attr/colorOnSurfaceVariant`; do not hardcode a colour or remove the tint attribute — the icon is used on both light and dark surfaces.
- Individual path `android:fillColor` values are `@android:color/white` acting as a base; the effective colour is always driven by the tint, never by the path fill directly.
- Both empty-state ImageViews (in `activity_todo_lists.xml` and `activity_todo_list.xml`) already declare `app:tint="?attr/colorOnSurfaceVariant"` — do not remove those attributes when editing layouts.
- No layout or ViewModel changes accompany this fix; → see [UI Polish](ui-polish.md) for the empty-state wiring and → see [Icon-Only UI](icon-only-ui.md) for the 120dp sizing and text-free empty state contract.

## UI
- **Screen(s)**: `TodoListsActivity`, `TodoListActivity` (empty-state view only)
- **Layout file(s)**: none changed — `res/drawable/ic_checklist.xml` only
- **Design decisions**: A single shared vector is used on both empty-state screens for visual consistency; the tint attribute approach (rather than hardcoded path colours) ensures correct rendering in both light and dark mode without a night-override drawable.
