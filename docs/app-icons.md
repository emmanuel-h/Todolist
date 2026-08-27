# App Icons

## What it does
Replaces the default Android Studio green-robot launcher icon with the app's own: a sticky card on paper with a blue tick through it. The icon renders correctly in light, dark, and themed-icon (Android 13+) modes at all densities — and, since #44, renders the *same* in light and dark, because a launcher only rasterises it once.

> **Repainted (2026-08-25) and un-nighted (2026-08-27).** The brand purple `#6750A4` is gone
> from the project — `SPEC.md` forbids it. The icon is now a sticky card on paper with a blue
> tick, and its background is `@color/launcher_paper`, a colour that deliberately has **no**
> `values-night` twin. It used to be `@color/paper`, which does have one: a launcher rasterises
> an adaptive icon once and keeps the result, so the icon held whichever mode it happened to be
> cached in and turned up dark on a light home screen ([#44](https://github.com/emmanuel-h/Todolist/issues/44)).
> The structure below — background / foreground / monochrome layers, the safe zone, the round
> raster rule — is still accurate.

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel code touched)
- **Key types**: no new types; changes are confined to drawable XML and mipmap raster resources
- **Async contract**: none

## Files
- `app/src/main/res/drawable/ic_launcher_background.xml` — flat `@color/launcher_paper` (`#FAF5EA`) rectangle on a 108×108 viewport
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — a sticky card on a transparent 108×108 canvas: a `#A99872` back edge, an `#E0D3B6` face, a `#D9CCAD` head strip, and a `#2E5AA8` tick stroked across it; all artwork confined to viewport coords 30–78, inside the 66 dp safe zone
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
- `store-assets/ic_launcher_play_store.png` — 512×512 Play Store high-res icon, the 108 dp canvas rendered 1:1 onto 512 px with the background full-bleed; stored outside `app/src/main/res/` so it is not bundled into the APK. It is generated from the same paths as the two layer drawables and must be redrawn whenever they change — it was still carrying the retired purple long after the layers had been repainted.

## Invariants & contracts
- `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` were left unchanged; both already declare `<background>`, `<foreground>`, and `<monochrome>`. The `<monochrome>` layer is its own file, `@drawable/ic_launcher_monochrome` — a bare tick stroked in solid black on transparency, which is what Android 13+ themed-icon tinting needs; the colour foreground would tint to a solid block.
- Round rasters must differ from their square counterparts at every density bucket. `minSdk` is 24, so API 24–25 devices consume these rasters directly — they must carry a true circular crop (corner alpha 0, centre alpha 255), not a copy of the square file.
- All foreground artwork must stay within viewport coords 21–87 (the 66 dp safe zone) to avoid clipping by circle, squircle, or rounded-square launcher masks.
- **`@color/launcher_paper` must never gain a `values-night` counterpart.** It is a separate resource from `@color/paper` for exactly that reason: the window behind the app follows the lamp and the launcher's sheet cannot, because the launcher caches what it rasterised. Keep its value equal to the light `@color/paper` and to the paper in the rasters.
- The Play Store asset lives in `store-assets/` and must never be moved into `app/src/main/res/`; APK size would increase unnecessarily.

## UI
- **Screen(s)**: launcher (system-level, not an Activity)
- **Layout file(s)**: `res/drawable/ic_launcher_background.xml`, `res/drawable/ic_launcher_foreground.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml`, `res/mipmap-anydpi-v26/ic_launcher_round.xml`
- **Design decisions**: A flat paper background (no gradient) is used so the icon looks identical in light and dark mode without needing a night-override drawable — and, since 2026-08-27, from a colour that cannot acquire one by accident. The checklist motif (rows 1–2 ticked, row 3 open ring) mirrors the existing `drawable/ic_checklist.xml` used for empty states (→ see `ui-polish.md`), keeping the visual language consistent. `fillType="evenOdd"` makes the checkmark a cutout revealing the background colour rather than a filled overlay, which eliminates the need for a separate check-mark colour and works correctly with Android 13+ monochrome tinting.
