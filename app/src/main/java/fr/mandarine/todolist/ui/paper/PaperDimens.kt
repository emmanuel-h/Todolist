package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Every fixed measurement in the app, in one place.
 *
 * There is no `res/values/dimens.xml` in this project — the design system is Kotlin
 * objects, not resources. `object` gives one shared instance and `@Immutable`
 * promises Compose that its values never change, which lets Compose skip
 * recomposing anything whose only input is this.
 *
 * `dp` is *density-independent pixels*: 1dp is one pixel on a 160-dpi screen and
 * scales with the display, so these numbers are physical sizes rather than pixel
 * counts. Text sizes use `sp` instead, which additionally follows the reader's font
 * scale setting — that is why no text size appears here; they live in `PaperType`.
 *
 * What is *not* here: anything that varies with the light in the room (see
 * `PaperPalette`), anything that varies with the page (see `LocalPagePitch` and
 * `LocalPaperGutter`, provided by `PaperTheme`), and anything about timing (see
 * `PaperMotion`).
 */
@Immutable
object PaperDimens {

    val gutter = 40.dp
    val wideGutter = 100.dp
    val rule = 1.dp
    val pageWidth = 640.dp
    val rowEndPadding = 4.dp
    val keyboardSeam = 12.dp

    val touchTarget = 48.dp
    val iconButton = 48.dp
    val iconGlyph = 24.dp
    val rowGlyph = 18.dp
    /**
     * A row control is drawn tight to its glyph so the writing keeps the line.
     * Three of these sit at the end of every row, so every dp of box around the
     * mark is a dp the name does not get — and the name is the thing the reader
     * came to read. The box is narrower than the 48dp square the guidance asks
     * for; the **height** is not, and on a row that is the axis a thumb misses
     * on. A near miss to either side lands on the row itself, which opens the
     * list rather than doing nothing.
     */
    val rowGlyphButton = 34.dp
    val jotGlyph = 14.dp
    val marginColumn = 32.dp

    val stickyPad = 72.dp
    val stickySheet = 56.dp
    val stickyCorner = 1.dp
    val stickyPeelTravelX = 12.dp
    val stickyPeelTravelY = 28.dp

    const val GRAIN_TILE_PIXELS = 512
}
