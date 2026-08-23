# Paper Design System

## What it does
Ports the ink-on-paper look into Jetpack Compose as a small package of tokens, spring motion specs, and previewable primitives. It is the vocabulary phases 3–5 of the [Compose migration](compose-migration-plan.md) are written in. Both screens now consume it — see [items-screen-compose.md](items-screen-compose.md) and [lists-screen-compose.md](lists-screen-compose.md).

## Architecture
- **Layers**: `ui/paper/` only — no domain, data, or presentation logic touched. The Pitest gate (`domain`, `data`, `presentation`) is untouched: 368/368 mutations, 100%.
- **Tokens are objects, not `CompositionLocal`s.** `PaperInk` and `PaperDimens` are Kotlin `object`s read directly by every primitive. The palette is fixed and light-only by design ([paper-background.md](paper-background.md)), so making it swappable would only invite it to be swapped.

## Files
- `ui/paper/PaperInk.kt` — the palette from `res/values/colors.xml` as `Color` constants
- `ui/paper/PaperDimens.kt` — the gutter, ruling, row heights, icon sizes, sticky-note sizes
- `ui/paper/PaperMotion.kt` — named springs: `sheetLift`, `sheetSettle`, `rowEnter`, `rowExit`, `rowPlacement`, `instant`
- `ui/paper/PaperTheme.kt` — `lightColorScheme` mapping the palette onto the M3 roles, mirroring `res/values/themes.xml`
- `ui/paper/PaperSurface.kt` — paper tone and fibre grain, drawn in one `drawWithCache`
- `ui/paper/RuledRow.kt` — the shared row grammar, plus a reusable `Modifier.paperRule()`
- `ui/paper/GhostRow.kt` — the "＋ …" add affordance
- `ui/paper/InkIcon.kt` — `InkIcon` and `InkIconButton`, tint always taken from the palette
- `ui/paper/CountBadge.kt` — the outlined pill with a leading glyph and a bare number
- `ui/paper/SectionDivider.kt` — the 1:3 rules with a bare count between them
- `ui/paper/PaperDialog.kt` — a shadowless sheet with square corners and a hairline edge
- `ui/paper/StickyNoteSheetState.kt` — pure interpolation for the peel and settle, no Compose types
- `ui/paper/StickyNotePad.kt` — the three-sheet pad, peel and settle both driven by `taken`
- `ui/paper/PaperPreviews.kt` — one `@Preview` per primitive, in populated and empty states
- `src/debug/java/…/PaperGalleryActivity.kt` + `src/debug/AndroidManifest.xml` — the same previews on a real device
- Tests: `PaperMotionTest`, `StickyNoteSheetStateTest` (pure JUnit), `PaperPrimitivesTest`, `PaperPreviewsTest` (Robolectric + Compose)

## Invariants & contracts
- **The palette stays fixed.** `PaperInk` is an `object`; there is no seam for wallpaper-derived or dark-mode colours. `PaperTheme` sets `surfaceTint = Color.Transparent`, the Compose equivalent of `elevationOverlayEnabled=false` — without it M3 tints raised surfaces with `colorPrimary` and paper goes grey-blue.
- **Both PNG tiles are retired here.** Grain is a 64×64 tile generated from a seeded `Random`, so it is byte-identical on every call and the paper never shimmers between recompositions.
- **No punched holes.** The gutter column of circles that the View background carried was drawn here as vector circles for one release, then dropped: it read as decoration down the left edge and the gutter is now bare paper. `PaperInk.hole` and the hole geometry in `PaperDimens` are gone with it — do not reintroduce them.
- **Ruling is per row.** `Modifier.paperRule()` draws under the row it is applied to, inset by the gutter — the same rule as the View implementation, and for the same reason (a page-wide ruling drifts off the baseline of a taller row).
- **Motion is springs, never duration-plus-easing.** Anything that moves takes a spec from `PaperMotion`. The one thing lost in translation is that the View peel accelerated out via `AccelerateInterpolator`; a spring cannot, so the flying sheet decelerates instead.
- **Icon-only holds.** The only text any primitive renders is a `CountBadge` number, a `SectionDivider` count, and the `GhostRow` hint, which is `@string/add_item_ghost_hint` — "…".
- **`PaperDialog` draws no elevation.** A Material dialog surface always does, and the paper design has no drop shadows; a hairline rule is the edge instead.
- **The pad peels because it was taken, not because it was tapped.** Phase 4 moved the peel into the same `taken` transition that drives the settle, so the tutorial gets the animation for free by setting state. The under-sheets hide while `taken`, which is why the pad has to stay composed rather than be removed — removing it would cancel the peel mid-flight.

## UI
- **Screen(s)**: both. `PaperGalleryActivity` (debug builds only) renders every preview on device.
- **Design decisions**:
  - `StickyNotePad` splits the animation from the composable: `stickyNotePeelAt` / `stickyNoteSettleAt` are pure functions of progress, unit-tested without Robolectric, and the composable only feeds them an `Animatable`. The View version needed a decorative ghost view to keep the animation from blocking a synchronous test assertion; here the peeling sheet is just a second composable that exists while `peeling` is true.
  - `InkIconButton` fades its own tint when disabled. `IconButton` normally handles this through `LocalContentColor`, which an explicit `tint` overrides — so a disabled button would have looked enabled.
  - Primitives take a `Painter` rather than a drawable id wherever the caller might vary it, so `@Preview` and tests can pass any icon.
  - `PaperSurface` applies `fillMaxSize()` before the caller's modifier, so it fills a screen by default but a preview can still pin it to a fixed height.
