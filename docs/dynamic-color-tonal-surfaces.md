# Dynamic Color + Tonal Surfaces

> **Partly superseded by [paper-background.md](paper-background.md).** Dynamic color has been removed in favour of a fixed ink-on-paper palette and the app is now light-only. The tonal-surface hierarchy it introduced still stands, remapped onto paper shades. Kept for history.

## What it does
On Android 12+ the app follows the device's wallpaper palette via `DynamicColors`; on API 24–31 it falls back to a brand palette seeded from `#7C3AED` (refined violet). Depth is expressed through tonal surface fills instead of drop shadows: the window background uses `surfaceContainerLowest`, list item cards use `surfaceContainer` at 0dp elevation.

## Architecture
- **Layers**: presentation (Application class, themes, layouts, colors) — no domain or data changes
- **Key types**: `TodoListApplication` — gains `DynamicColors.applyToActivitiesIfAvailable(this)` in `onCreate()`
- **Async contract**: none — purely static theme and layout configuration

## Files
- `app/src/main/java/fr/mandarine/todolist/TodoListApplication.kt` — added `DynamicColors.applyToActivitiesIfAvailable(this)` for Android 12+ wallpaper palette support
- `app/src/main/res/values/colors.xml` — complete palette replacement: all M3 template hex values removed, replaced with values derived from seed `#7C3AED`; full `surfaceContainer*` family (Lowest/Low/Container/High/Highest) added for light; `launcher_background` color added
- `app/src/main/res/values-night/themes.xml` — `colorSurfaceContainer*` role overrides for dark theme with dark-variant colors; `android:colorBackground` maps to `surfaceContainerLowest`
- `app/src/main/res/values/themes.xml` — same five `colorSurfaceContainer*` overrides for light theme; `android:colorBackground` maps to `surfaceContainerLowest`
- `app/src/main/res/layout/item_todo_list.xml` — card style changed from `Widget.Material3.CardView.Elevated` to `Widget.Material3.CardView.Filled`; `app:cardBackgroundColor="?attr/colorSurfaceContainer"`; `cardElevation` reduced from `2dp` to `0dp`
- `app/src/main/res/drawable/ic_launcher_background.xml` — fill replaced from hardcoded `#6750A4` with `@color/launcher_background` (`#7C3AED`)

## Invariants & contracts
- No hex value from the stock M3 demo palette (`#6750A4` family) may exist in `colors.xml`; any new palette must derive from `#7C3AED` or come from the device's dynamic color system.
- The `surfaceContainer*` token family (Lowest/Low/Container/High/Highest) must remain defined in both light and dark themes so that references in `item_todo_inline_add.xml` and `item_todo_list_inline_add.xml` resolve to palette-consistent values.
- `DynamicColors.applyToActivitiesIfAvailable()` must be called at Application scope (not per-Activity) so every activity inherits the wallpaper palette on Android 12+.
- List item cards use `Widget.Material3.CardView.Filled` + `colorSurfaceContainer` + `0dp` elevation; do not reintroduce `Elevated` style or shadow-based depth for in-screen content.
- ~~The tutorial overlay (`overlay_tutorial.xml`) intentionally retains `Widget.Material3.CardView.Elevated` at 6–8dp because it is a floating layer above a scrim, not in-screen content — this is correct and must not be changed to match item cards.~~ **No longer true** — the overlay is Compose and its three surfaces are shadowless paper slips → see [tutorial-overlay-compose.md](tutorial-overlay-compose.md). The app draws no drop shadows anywhere.

## UI
- **Screen(s)**: `TodoListsActivity`, `TodoListActivity` (both inherit theme at Application scope)
- **Layout file(s)**: `res/layout/item_todo_list.xml`
- **Design decisions**: tonal depth replaces shadow depth — the visual hierarchy reads as `surfaceContainerLowest` (window) → `surfaceContainer` (cards) → `surfaceContainerHigh` (inline-add ghost row) with zero elevation anywhere in the list content area
