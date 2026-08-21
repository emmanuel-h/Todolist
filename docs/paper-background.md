# Paper Background

## What it does
Turns the whole app into one continuous sheet of ruled loose-leaf paper: warm tone, fibre grain, a punched-hole column down the left gutter, and a hairline ruling under every row on both screens. The app is light-only and its palette is a fixed ink-on-paper set — chrome no longer follows the device wallpaper.

## Architecture
- **Layers**: ui + resources only — no domain, data, or presentation logic changed.
- **Key resources**:
  - `drawable/bg_paper.xml` — layer-list used as `android:windowBackground`: paper tone → grain tile → hole tile
  - `drawable/row_rule.xml` — 1dp bottom rule inset by the gutter; the background of every row on both screens
  - `drawable/badge_pill.xml` — outlined chip (transparent fill, 1dp stroke, tinted via `backgroundTint`)
  - `drawable/fab_paper_outline.xml` — the ＋ chip's ink border, applied as the FAB's `android:foreground`
  - `drawable-nodpi/tile_paper_grain.png` — 64×64 procedurally generated noise, tiled and tinted
  - `drawable-xxhdpi/tile_paper_hole.png` — 40dp × 128dp tile with one punched circle, tiled down the gutter

## Files
- `res/values/colors.xml` — the whole ink-on-paper palette (`paper*`, `ink*`, `pencil`, `paper_rule`)
- `res/values/themes.xml` — `Theme.Material3.Light.NoActionBar` base, M3 roles mapped to the palette, `elevationOverlayEnabled=false`, `ShapeAppearance.Paper.Chip`, `ThemeOverlay.Paper.Dialog`, `MaterialAlertDialog.Paper`
- `res/values/dimens.xml` — `paper_gutter_width` (40dp), `paper_rule_height`, `paper_section_inset`, `paper_toolbar_inset`
- `res/layout/activity_todo_list.xml` — transparent app bar, toolbar inset past the margin, watermark removed
- `res/layout/activity_todo_lists.xml` — watermark removed, paper-chip FAB, pencil-toned replay button
- `res/layout/item_todo.xml`, `item_todo_inline_add.xml`, `item_todo_list.xml`, `item_todo_list_inline_add.xml` — ruled rows at the gutter, ink/pencil icon tints
- `res/layout/item_todo_divider.xml` — section header inset to clear the gutter
- `ui/TodoListActivity.kt`, `ui/TodoListsActivity.kt` — `InsetItemDivider` and the watermark fields/alpha logic removed
- `ui/TodoListsAdapter.kt` — row is no longer a `MaterialCardView`; `applyAllDoneStyle` only sets strikethrough + alpha
- `TodoListApplication.kt` — `DynamicColors.applyToActivitiesIfAvailable()` removed
- Deleted: `res/values-night/`

## Invariants & contracts
- **Light-only.** No `values-night/`, a `Light` theme parent, and no `DayNight` overlay. Re-introducing `DynamicColors` would re-introduce a `DayNight` overlay and with it dark mode — do not add it back without also forcing `AppCompatDelegate.MODE_NIGHT_NO`.
- **The palette is fixed, not derived.** Paper texture must not take a device hue. Only the launcher icon is exempt; it stays brand violet (`docs/app-icons.md`).
- `elevationOverlayEnabled` must stay `false`. The M3 elevation overlay tints raised surfaces with `colorPrimary`, which turns paper grey-blue on dialogs and the FAB.
- Ruling is per row, never per page. A page-wide tiled ruling would drift off the baseline of a taller row (a list row with both dates is ~96dp). The cost is that ruling stops where content stops.
- Row content must start at or after `@dimen/paper_gutter_width`; anything drawn in the gutter collides with the hole punches. This is why the toolbar carries `paper_toolbar_inset`.
- **No margin rule.** The red vertical rules were removed: real to-do lists are written on plain ruled paper, and a coloured line down the gutter read as decoration competing with the holes. Do not reintroduce one.
- Both screens share one row grammar. A change to the item row's leading structure must be mirrored in the list row, and vice versa.
- No background illustrations. `IconOnlyUiTest` fails on any static text or any `ImageView` without a `contentDescription` in either activity layout.
- `tile_paper_hole.png` lives in `drawable-xxhdpi/` so its 40dp × 128dp size is density-correct; `tile_paper_grain.png` lives in `drawable-nodpi/` so grain stays at 1:1 pixels.

## UI
- **Screen(s)**: `TodoListsActivity`, `TodoListActivity`, and every dialog
- **Design decisions**:
  - Two tiny PNG tiles are unavoidable: `<bitmap android:tileMode="repeat">` needs a real bitmap, and neither repeating hole punches nor fibre grain can be a stretched vector. Everything else is XML shapes.
  - The lists screen dropped its cards. Cards on paper read as floating chrome, and with dynamic color gone there was no tonal step left to justify them; ruled rows put both screens in the same rhythm.
  - A fully-completed list loses its `colorSecondaryContainer` fill and keeps only strikethrough — the paper equivalent of crossing a line out.
  - The ＋ stays a `FloatingActionButton` rather than becoming a ghost row: the tutorial scripts a tap on it and four test classes assert on the type. It is restyled, not replaced. Its outline is a `foreground` drawable because `FloatingActionButton` has no stroke support on API 21+ (`app:borderWidth` is a no-op there).
  - The `InsetItemDivider` `RecyclerView.ItemDecoration` was deleted — it drew a second, differently-inset line under every pair of item rows, doubling with the ruling. See [inset-item-dividers.md](inset-item-dividers.md), now superseded.
