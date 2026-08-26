package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SHEET_SIDE = 240
private const val SHEET_DENSITY = 2f
private const val SAMPLE_STEP = 3
private const val SHEET_SPAN_BUDGET = 0.075f
private const val GRAIN_BUDGET = 0.04f

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PaperSheetRenderTest {

    private val palette = PaperPalette.light

    @Test
    fun `should never leave the sheet a flat fill`() {
        val sheet = drawSheet(palette.paper)

        val tones = mutableSetOf<Long>()
        for (y in 0 until sheet.height step SAMPLE_STEP) {
            for (x in 0 until sheet.width step SAMPLE_STEP) {
                tones += sheet[x, y].value.toLong()
            }
        }

        assertTrue("sheet drew only $tones", tones.size > 8)
    }

    @Test
    fun `should light the top of the sheet above its far corner`() {
        val sheet = drawSheet(palette.paper)

        val top = sheet.grey(sheet.width / 2, 1)
        val corner = sheet.grey(sheet.width - 2, sheet.height - 2)

        assertTrue("top $top corner $corner", corner < top)
    }

    @Test
    fun `should keep the lit top and the far corner within one page of each other`() {
        val sheet = drawSheet(palette.paper)

        val lit = palette.paperSheet.grey()
        var darkest = lit
        for (y in 0 until sheet.height step SAMPLE_STEP) {
            for (x in 0 until sheet.width step SAMPLE_STEP) {
                darkest = minOf(darkest, sheet.grey(x, y))
            }
        }

        assertTrue("darkened by ${lit - darkest}", lit - darkest < SHEET_SPAN_BUDGET)
    }

    @Test
    fun `should let the grain alone darken far less than the whole sheet does`() {
        val flat = drawSheet(tone = palette.paperSheet, vignette = Color.Transparent)

        val lit = palette.paperSheet.grey()
        var darkest = lit
        for (y in 0 until flat.height step SAMPLE_STEP) {
            for (x in 0 until flat.width step SAMPLE_STEP) {
                darkest = minOf(darkest, flat.grey(x, y))
            }
        }

        assertTrue("grain darkened by ${lit - darkest}", lit - darkest < GRAIN_BUDGET)
    }

    @Test
    fun `should keep the sheet opaque so nothing behind it shows through`() {
        val sheet = drawSheet(palette.paper)

        assertEquals(1f, sheet[0, 0].alpha, 0f)
        assertEquals(1f, sheet[sheet.width - 1, sheet.height - 1].alpha, 0f)
    }

    @Test
    fun `should carry the grain onto a sticky sheet in its own colour`() {
        val sheet = drawSheet(tone = palette.stickyNote, lit = palette.stickyNote)

        val tones = mutableSetOf<Long>()
        for (y in 0 until sheet.height step SAMPLE_STEP) {
            for (x in 0 until sheet.width step SAMPLE_STEP) {
                tones += sheet[x, y].value.toLong()
            }
        }

        assertTrue("sticky sheet drew only $tones", tones.size > 8)
        assertTrue(sheet.grey(1, 1) <= palette.stickyNote.grey())
    }

    @Test
    fun `should lay the grain on a night sheet as light rather than as shade`() {
        val night = PaperPalette.night
        val sheet = drawSheet(
            tone = night.paperSheet,
            lit = night.paperSheet,
            vignette = Color.Transparent
        )

        val lit = night.paperSheet.grey()
        var brightest = lit
        for (y in 0 until sheet.height step SAMPLE_STEP) {
            for (x in 0 until sheet.width step SAMPLE_STEP) {
                brightest = maxOf(brightest, sheet.grey(x, y))
            }
        }

        assertTrue("grain lit by ${brightest - lit}", brightest > lit)
        assertTrue("grain lit by ${brightest - lit}", brightest - lit < GRAIN_BUDGET)
    }

    @Test
    fun `should never leave a night sheet a flat fill`() {
        val night = PaperPalette.night
        val sheet = drawSheet(tone = night.paper, lit = night.paperSheet, vignette = night.vignette)

        val tones = mutableSetOf<Long>()
        for (y in 0 until sheet.height step SAMPLE_STEP) {
            for (x in 0 until sheet.width step SAMPLE_STEP) {
                tones += sheet[x, y].value.toLong()
            }
        }

        assertTrue("night sheet drew only $tones", tones.size > 8)
    }

    /**
     * The pad takes the grain of the room rather than carrying daylight into the
     * dark with it: shade by daylight, light by lamplight, exactly as the page it
     * lies on does. Keeping it grained in shade at night is what went with keeping
     * it bright, and both are gone.
     */
    @Test
    fun `should grain a sticky note in shade by daylight`() {
        val sheet = drawSheet(
            tone = palette.stickyNote,
            lit = palette.stickyNote,
            vignette = Color.Transparent
        )

        val lit = palette.stickyNote.grey()
        var brightest = lit
        for (y in 0 until sheet.height step SAMPLE_STEP) {
            for (x in 0 until sheet.width step SAMPLE_STEP) {
                brightest = maxOf(brightest, sheet.grey(x, y))
            }
        }

        assertEquals(lit, brightest, 0f)
    }

    @Test
    fun `should grain a sticky note in light by lamplight`() {
        val night = PaperPalette.night
        val sheet = drawSheet(
            tone = night.stickyNote,
            lit = night.stickyNote,
            vignette = Color.Transparent
        )

        val lit = night.stickyNote.grey()
        var brightest = lit
        for (y in 0 until sheet.height step SAMPLE_STEP) {
            for (x in 0 until sheet.width step SAMPLE_STEP) {
                brightest = maxOf(brightest, sheet.grey(x, y))
            }
        }

        assertTrue("grain lit by ${brightest - lit}", brightest > lit)
        assertTrue("grain lit by ${brightest - lit}", brightest - lit < GRAIN_BUDGET)
    }

    private fun drawSheet(
        tone: Color,
        lit: Color = palette.paperSheet,
        vignette: Color = palette.vignette
    ): PixelMap {
        val bitmap = ImageBitmap(SHEET_SIDE, SHEET_SIDE)
        val size = Size(SHEET_SIDE.toFloat(), SHEET_SIDE.toFloat())
        val grain = paperGrainOn(tone)
        CanvasDrawScope().draw(
            Density(SHEET_DENSITY),
            LayoutDirection.Ltr,
            Canvas(bitmap),
            size
        ) {
            drawPaperSheet(
                paperSheetBrushes(
                    tile = paperGrainTile(SHEET_DENSITY, grain),
                    lit = lit,
                    tone = tone,
                    vignette = vignette,
                    size = size,
                    grain = grain
                )
            )
        }
        return bitmap.toPixelMap()
    }
}

private fun PixelMap.grey(x: Int, y: Int): Float = this[x, y].grey()

private fun Color.grey(): Float = (red + green + blue) / 3f
