package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The add line is a strip of the page and has to read as one. A sheet is lit at its
 * top, so a sheet only a line tall laid at the foot of the page drew the whole of
 * that light inside its own few millimetres and came out white on cream — which is
 * what the page's ground did there for as long as the strip asked for a ground
 * rather than for a strip.
 *
 * It reads the pixels because the tone at the seam is not in the semantics tree.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PaperStripTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should ground the strip in the tone the page has reached under it`() {
        val step = seamStep { Modifier.paperStrip() }

        assertTrue("the strip stepped $step off the page", abs(step) < SEAM_BUDGET)
    }

    @Test
    fun `should step off the page when the strip is given a sheet of its own`() {
        val step = seamStep { Modifier.paperSheet() }

        assertTrue("a sheet of its own stepped only $step", step > SEAM_BUDGET)
    }

    private fun seamStep(ground: @Composable () -> Modifier): Float {
        var strip = 0
        composeRule.setContent {
            PaperTheme {
                with(LocalDensity.current) { strip = STRIP.roundToPx() }
                Box(modifier = Modifier.fillMaxSize().paperSheet().testTag(PAGE)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(STRIP)
                            .then(ground())
                    )
                }
            }
        }

        val pixels = composeRule.onNodeWithTag(PAGE).captureToImage().toPixelMap()
        val seam = pixels.height - strip
        return pixels.band(seam) - pixels.band(seam - BAND)
    }

    private fun PixelMap.band(top: Int): Float {
        var total = 0f
        var read = 0
        for (y in top until top + BAND) {
            for (x in 0 until width) {
                val pixel = this[x, y]
                total += (pixel.red + pixel.green + pixel.blue) / CHANNELS
                read++
            }
        }
        return total / read
    }

    private companion object {
        const val PAGE = "page"
        const val BAND = 6
        const val CHANNELS = 3f
        const val SEAM_BUDGET = 0.012f
        val STRIP = 56.dp
    }
}
