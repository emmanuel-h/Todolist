# Paper Design System

## What it does
Ports the ink-on-paper look into Jetpack Compose as a small package of tokens, spring motion specs, and previewable primitives. It is the vocabulary phases 3–5 of the [Compose migration](compose-migration-plan.md) are written in. **No screen consumes it yet** — the app still runs entirely on Views.

## Architecture
- **Layers**: `ui/paper/` only — no domain, data, or presentation logic touched. The Pitest gate (`domain`, `data`, `presentation`) is untouched: 368/368 mutations, 100%.
- **Tokens are objects, not `CompositionLocal`s.** `PaperInk` and `PaperDimens` are Kotlin `object`s read directly by every primitive. The palette is fixed and light-only by design ([paper-background.md](paper-background.md)), so making it swappable would only invite it to be swapped.

## Files
- `ui/paper/PaperInk.kt` — the palette from `res/values/colors.xml` as `Color` constants
- `ui/paper/PaperDimens.kt` — the gutter, ruling, row heights, icon sizes, hole geometry, sticky-note sizes
- `ui/paper/PaperMotion.kt` — named springs: `sheetLift`, `sheetSettle`, `rowEnter`, `rowExit`, `rowPlacement`, `instant`
- `ui/paper/PaperTheme.kt` — `lightColorScheme` mapping the palette onto the M3 roles, mirroring `res/values/themes.xml`
- `ui/paper/PaperSurface.kt` — paper tone, fibre grain, punched-hole gutter, drawn in one `drawWithCache`
- `ui/paper/RuledRow.kt` — the shared row grammar, plus a reusable `Modifier.paperRule()`
- `ui/paper/GhostRow.kt` — the "＋ …" add affordance
- `ui/paper/InkIcon.kt` — `InkIcon` and `InkIconButton`, tint always taken from the palette
- `ui/paper/CountBadge.kt` — the outlined pill with a leading glyph and a bare number
- `ui/paper/StickyNoteSheetState.kt` — pure interpolation for the peel and settle, no Compose types
- `ui/paper/StickyNotePad.kt` — the three-sheet pad, peel on tap, settle on return
- `ui/paper/PaperPreviews.kt` — one `@Preview` per primitive, in populated and empty states
- `src/debug/java/…/PaperGalleryActivity.kt` + `src/debug/AndroidManifest.xml` — the same previews on a real device
- Tests: `PaperMotionTest`, `StickyNoteSheetStateTest` (pure JUnit), `PaperPrimitivesTest`, `PaperPreviewsTest` (Robolectric + Compose)

## Invariants & contracts
- **The palette stays fixed.** `PaperInk` is an `object`; there is no seam for wallpaper-derived or dark-mode colours. `PaperTheme` sets `surfaceTint = Color.Transparent`, the Compose equivalent of `elevationOverlayEnabled=false` — without it M3 tints raised surfaces with `colorPrimary` and paper goes grey-blue.
- **Both PNG tiles are retired here.** Grain is a 64×64 tile generated from a seeded `Random`, so it is byte-identical on every call and the paper never shimmers between recompositions. Holes are drawn as vector circles rather than a tiled bitmap: 5.7dp radius, 15.8dp from the left edge, 128dp apart, first centre at 64dp — the geometry measured off `drawable-xxhdpi/tile_paper_hole.png`. Verified against the running app: hole bands land on the same pixel rows (167.5, 503.5, …) at 420dpi.
- **One `PaperSurface` per screen.** Nesting them restarts the hole sequence and the punches stop lining up. The debug gallery does exactly this, on purpose, and it shows.
- **Ruling is per row.** `Modifier.paperRule()` draws under the row it is applied to, inset by the gutter — the same rule as the View implementation, and for the same reason (a page-wide ruling drifts off the baseline of a taller row).
- **Motion is springs, never duration-plus-easing.** Anything that moves takes a spec from `PaperMotion`. The one thing lost in translation is that the View peel accelerated out via `AccelerateInterpolator`; a spring cannot, so the flying sheet decelerates instead.
- **Icon-only holds.** The only text any primitive renders is a `CountBadge` number and the `GhostRow` hint, which is `@string/add_item_ghost_hint` — "…".

## UI
- **Screen(s)**: none yet. `PaperGalleryActivity` (debug builds only) renders every preview on device.
- **Design decisions**:
  - `StickyNotePad` splits the animation from the composable: `stickyNotePeelAt` / `stickyNoteSettleAt` are pure functions of progress, unit-tested without Robolectric, and the composable only feeds them an `Animatable`. The View version needed a decorative ghost view to keep the animation from blocking a synchronous test assertion; here the peeling sheet is just a second composable that exists while `peeling` is true.
  - `InkIconButton` fades its own tint when disabled. `IconButton` normally handles this through `LocalContentColor`, which an explicit `tint` overrides — so a disabled button would have looked enabled.
  - Primitives take a `Painter` rather than a drawable id wherever the caller might vary it, so `@Preview` and tests can pass any icon.
  - `PaperSurface` applies `fillMaxSize()` before the caller's modifier, so it fills a screen by default but a preview can still pin it to a fixed height.
