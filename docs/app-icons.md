# App Icons

## What it does
Replaces the default Android Studio green-robot launcher icon with a custom checklist icon using the app's brand purple (`#6750A4`) background and white foreground artwork. The icon renders correctly in light, dark, and themed-icon (Android 13+) modes at all densities.

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel code touched)
- **Key types**: no new types; changes are confined to drawable XML and mipmap raster resources
- **Async contract**: none

## Files
- `app/src/main/res/drawable/ic_launcher_background.xml` — flat `#6750A4` rectangle on a 108×108 viewport; replaces the template green-with-grid background
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — three-row checklist artwork on a transparent 108×108 canvas; rows 1 and 2 use `fillType="evenOdd"` check-circle paths (checkmark is a transparent cutout exposing the purple background); row 3 is an evenOdd donut (outer r=6, inner r=3); all artwork confined to viewport coords 21–87 (the 66 dp safe zone)
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp` — 48×48 px square raster
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp` — 72×72 px square raster
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp` — 96×96 px square raster
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` — 144×144 px square raster
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` — 192×192 px square raster
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp` — 48×48 px round raster (genuine circular crop: transparent outside inscribed circle)
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp` — 72×72 px round raster
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp` — 96×96 px round raster
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp` — 144×144 px round raster
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp` — 192×192 px round raster
- `store-assets/ic_launcher_play_store.png` — 512×512 RGBA Play Store high-res icon; stored outside `app/src/main/res/` so it is not bundled into the APK

## Invariants & contracts
- `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` were left unchanged; both already declare `<background>`, `<foreground>`, and `<monochrome>`. The `<monochrome>` layer reuses `@drawable/ic_launcher_foreground` (artwork on transparency), so Android 13+ themed-icon tinting works without a separate file.
- Round rasters must differ from their square counterparts at every density bucket. `minSdk` is 24, so API 24–25 devices consume these rasters directly — they must carry a true circular crop (corner alpha 0, centre alpha 255), not a copy of the square file.
- All foreground artwork must stay within viewport coords 21–87 (the 66 dp safe zone) to avoid clipping by circle, squircle, or rounded-square launcher masks.
- The background colour `#6750A4` is `md_theme_light_primary` / `colorPrimary`; do not introduce a separate colour value — reference or hardcode the same hex to avoid a divergence if the theme token changes.
- The Play Store asset lives in `store-assets/` and must never be moved into `app/src/main/res/`; APK size would increase unnecessarily.

## UI
- **Screen(s)**: launcher (system-level, not an Activity)
- **Layout file(s)**: `res/drawable/ic_launcher_background.xml`, `res/drawable/ic_launcher_foreground.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml`, `res/mipmap-anydpi-v26/ic_launcher_round.xml`
- **Design decisions**: Flat brand-purple background (no gradient) is used so the icon looks identical in light and dark mode without needing a night-override drawable. The checklist motif (rows 1–2 ticked, row 3 open ring) mirrors the existing `drawable/ic_checklist.xml` used for empty states (→ see `ui-polish.md`), keeping the visual language consistent. `fillType="evenOdd"` makes the checkmark a cutout revealing the background colour rather than a filled overlay, which eliminates the need for a separate check-mark colour and works correctly with Android 13+ monochrome tinting.
