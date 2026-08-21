# Compose Teardown

## What it does
The last phase of the Compose migration removes the View system from the app entirely: no `appcompat`, no `recyclerview`, no Views `com.google.android.material`, no `AppCompatActivity`, and a theme that does nothing but dress the window for the frames before the first composition.

Nothing changed on screen. The release APK lost **37%**.

## Architecture
- **Activities are `ComponentActivity`.** Both already called `setContent` and owned no views; `AppCompatActivity` was giving them vector-drawable compat they do not need at minSdk 24, and a theme requirement they no longer meet.
- **The theme is `android:Theme.Material.Light.NoActionBar`** — the platform one, not the library's. Six lines: window background, system-bar transparency, light bars.
- **The palette lives in Kotlin.** `PaperInk` was already the source of truth; `res/values/colors.xml` was a second copy nothing read. It now holds two colours: the one the window is painted before Compose draws, and the launcher background.

## Files
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — three dependencies and their version refs removed
- `ui/TodoListActivity.kt`, `ui/TodoListsActivity.kt` — `AppCompatActivity` → `ComponentActivity`
- `res/values/themes.xml` — 69 lines → 16, all of it the window
- `res/values/colors.xml` — 25 colours → 2
- `res/values/strings.xml` — 40 strings → 27
- `res/values/dimens.xml` — 5 dimensions → 1 (`list_horizontal_inset`, the only one that varies by screen width)
- five vector icons — dropped `android:tint="?attr/colorControlNormal"`, an attribute that no longer resolves and that `InkIcon` overrides anyway

**Deleted**: `res/drawable/bg_paper.xml`, `res/drawable-nodpi/tile_paper_grain.png`, `res/drawable-xxhdpi/tile_paper_hole.png`, `res/drawable/ic_format_list_bulleted.xml`, `res/values/attrs.xml`, and the three orphaned dialog styles.

## Invariants & contracts
- **Every icon is tinted by its caller.** `InkIcon` and `InkIconButton` both default to a palette colour, so no drawable needs a tint of its own. A vector added without one would draw white on paper — that is why the `?attr` tints could be deleted rather than translated.
- **The window background is flat paper.** `PaperSurface` has drawn the grain and hole punches since phase 2; the window only shows for the frames before the first composition. Keeping the tiled layer-list meant two PNGs and a full-screen textured layer under every screen for no visible gain.
- **`res/values/` is not the design system.** Colours, dimensions and motion specs are `PaperInk`, `PaperDimens` and `PaperMotion`. The one exception is `list_horizontal_inset`, which exists in `values-sw600dp`/`values-sw720dp` because only resource qualifiers can vary it by screen width.
- **Light-only is now structural.** With Views Material gone there is no `DayNight` overlay to re-introduce and no `DynamicColors` to call. `PaperTheme` defines one colour scheme and reads no system setting.
- **System bars are set explicitly.** The Material3 parent used to supply `windowDrawsSystemBarBackgrounds` and a navigation-bar colour; the platform parent does not, so the theme states both. Without them the app would look edge-to-edge on API 35+ and framed on everything older.

## UI
- **Screen(s)**: both, unchanged. Verified against the phase 5 screenshots — grain, hole punches, ruling, row metrics, icon tints, the paper rename sheet and the Material date picker are all identical.
- **Design decisions**:
  - **No visual change was intended and none shipped.** The one delta is a frame or two of untextured paper at cold start, before `PaperSurface` draws.
  - The APK went from 2,686,277 to 1,690,684 bytes — **−995,593, −37.1%** — and from 724 files to 130. `resources.arsc` alone fell from 716K to 224K: most of what went was Material's styles and attributes, not code.
