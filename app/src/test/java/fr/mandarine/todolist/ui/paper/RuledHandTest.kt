package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.rememberTextMeasurer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Above scale 1.05 the platform stops converting `sp` linearly and compresses the
 * larger sizes hardest, so one `28.sp` leading resolves taller under a 14sp hand
 * than under a 20sp one. The page has to stay ruled at the writing hand while
 * every other hand is held to that same line.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RuledHandTest {

    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun restoreFontScale() {
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test
    fun `should write the smallest hand on the tallest line when the font scale bends`() {
        val page = writePage()

        assertTrue(
            "expected the margin hand to overshoot the page but wrote ${page.nominal}",
            page.nominal.last() > page.nominal.first()
        )
    }

    @Test
    fun `should hold every hand to one line of the page when the font scale bends`() {
        val page = writePage()

        assertEquals(listOf(page.pitch), page.ruled.distinct())
    }

    @Test
    fun `should rule the page at the writing hand and never at the marginalia`() {
        val page = writePage()

        assertEquals(page.nominal.first(), page.pitch)
        assertTrue(
            "expected ${page.nominal.last()} to overshoot ${page.pitch}",
            page.nominal.last() > page.pitch
        )
    }

    private fun writePage(): Page {
        RuntimeEnvironment.setFontScale(BENT_SCALE)
        var pitch = 0
        var nominal: List<Int> = emptyList()
        var ruled: List<Int> = emptyList()
        composeRule.setContent {
            PaperTheme {
                val measurer = rememberTextMeasurer()
                val hand = LocalRuledHand.current
                pitch = with(LocalDensity.current) { LocalPagePitch.current.roundToPx() }
                nominal = listOf(PaperType.itemLine, PaperType.listLine, PaperType.margin)
                    .map { measurer.measure(SAMPLE, it).size.height }
                ruled = listOf(hand.itemLine, hand.listLine, hand.margin)
                    .map { measurer.measure(SAMPLE, it).size.height }
            }
        }
        composeRule.waitForIdle()
        return Page(pitch, nominal, ruled)
    }

    private class Page(val pitch: Int, val nominal: List<Int>, val ruled: List<Int>)

    private companion object {
        const val SAMPLE = "Ag"
        const val BENT_SCALE = 1.3f
    }
}
