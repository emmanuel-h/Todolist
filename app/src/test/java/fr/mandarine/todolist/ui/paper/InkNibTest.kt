package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

private const val SIDE = 40
private const val CENTRE = 20f
private const val NIB = 6f
private const val REACH = 24
private const val SCAN = 20
private const val NO_BLEED = 0.001f
private const val CORE = 20
private const val A_SHADE = 0.01f

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InkNibTest {

    private val palette = PaperPalette.light

    @Test
    fun `should dry every ink darker than it went on`() {
        InkTone.entries.forEach { tone ->
            val wet = palette.inked(tone)

            assertTrue("$tone", wet.dried().grey() < wet.grey())
        }
    }

    @Test
    fun `should let a mark bleed past the nib that drew it`() {
        val inked = scan { inked(mark(), palette.ink, NIB) }
        val single = scan { drawPath(mark(), palette.ink, style = Stroke(NIB, cap = StrokeCap.Round)) }

        assertTrue(single.darkening(REACH) < NO_BLEED)
        assertTrue(inked.darkening(REACH) > NO_BLEED)
    }

    @Test
    fun `should keep the mark itself the weight the nib was given`() {
        val inked = scan { inked(mark(), palette.ink, NIB) }
        val single = scan { drawPath(mark(), palette.ink, style = Stroke(NIB, cap = StrokeCap.Round)) }

        assertEquals(single.darkening(CORE), inked.darkening(CORE), A_SHADE)
    }

    @Test
    fun `should lay a mark down in more ink than one pass leaves`() {
        val inked = scan { inked(mark(), palette.ink, NIB) }
        val single = scan { drawPath(mark(), palette.ink, style = Stroke(NIB, cap = StrokeCap.Round)) }

        assertTrue(inked.ink() > single.ink())
    }

    @Test
    fun `should draw a fading mark in fading ink`() {
        val full = scan { inked(mark(), palette.ink, NIB) }
        val half = scan { inked(mark(), palette.ink, NIB, alpha = 0.5f) }

        assertTrue(half.ink() < full.ink())
        assertTrue(half.ink() > NO_BLEED)
    }

    private fun mark(): Path = Path().apply {
        moveTo(CENTRE, 0f)
        lineTo(CENTRE, SIDE.toFloat())
    }

    private fun scan(draw: DrawScope.() -> Unit): PixelMap {
        val bitmap = ImageBitmap(SIDE, SIDE)
        val size = Size(SIDE.toFloat(), SIDE.toFloat())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), size) {
            drawRect(Color.White)
            draw()
        }
        return bitmap.toPixelMap()
    }

    private fun PixelMap.darkening(x: Int): Float = 1f - this[x, SCAN].grey()

    private fun PixelMap.ink(): Float = (0 until SIDE).sumOf { darkening(it).toDouble() }.toFloat()

    private fun Color.grey(): Float = (red + green + blue) / 3f
}
