package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RuledPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should rule the page at the line height the hand writes on`() {
        assertEquals(56.dp, pitchesFor(PaperType.itemLine).single())
    }

    @Test
    fun `should widen the ruling when the type is set on a wider line`() {
        val pitches = pitchesFor(PaperType.itemLine, PaperType.base.copy(lineHeight = 80.sp))

        assertEquals(80.dp, pitches[1])
        assertTrue("expected wider ruling but was ${pitches[1]}", pitches[1] > pitches[0])
    }

    @Test
    fun `should never rule tighter than the touch floor`() {
        assertEquals(48.dp, pitchesFor(PaperType.base.copy(lineHeight = 20.sp)).single())
    }

    @Test
    fun `should widen the ruling with the readers font scale so the hand keeps its line`() {
        val pitches = pitchesAtFontScales(1f, 1.5f)

        assertTrue("plain ${pitches[0]} enlarged ${pitches[1]}", pitches[1] > pitches[0])
    }

    @Test
    fun `should seat the first rule at the foot of the head margin`() {
        val offset = firstRuleOffset(headPx = 56f, scrolledPx = 0, thickness = 1f, pitchPx = 56f)

        assertEquals(55f, offset, 0f)
    }

    @Test
    fun `should slide the rules by the scroll so the page moves with the ink`() {
        val offset = firstRuleOffset(headPx = 56f, scrolledPx = 20, thickness = 1f, pitchPx = 56f)

        assertEquals(35f, offset, 0f)
    }

    @Test
    fun `should wrap the first rule back onto the page once a whole line scrolls away`() {
        val offset = firstRuleOffset(headPx = 0f, scrolledPx = 0, thickness = 1f, pitchPx = 56f)

        assertEquals(55f, offset, 0f)
    }

    @Test
    fun `should leave the head margin unruled and double the rule that ends it`() {
        assertEquals(listOf(79, 82, 135, 191, 247, 303, 359), ruledRows(scrolled = 0))
    }

    @Test
    fun `should slide the head rules up with the page they are printed on`() {
        assertEquals(listOf(59, 62, 115, 171, 227, 283, 339, 395), ruledRows(scrolled = 20))
    }

    @Test
    fun `should rule to the top of the page once the head margin has scrolled away`() {
        assertEquals(
            listOf(23, 79, 135, 191, 247, 303, 359),
            ruledRows(scrolled = 0, headVisible = false)
        )
    }

    @Test
    fun `should start the ruling at the gutter so the margin stays clear`() {
        val page = drawRules(scrolled = 0, headVisible = true)

        assertEquals(0f, page[GUTTER - 1, 79].alpha, 0f)
        assertEquals(1f, page[GUTTER, 79].alpha, 0f)
    }

    @Test
    fun `should slide the head rule away with the page it is printed on`() {
        val head = headRuleOffset(headPx = 80f, scrolledPx = 20, thickness = 1f)

        assertEquals(59f, head, 0f)
    }

    @Test
    fun `should hand the ruling back to the wrapped grid once the head margin is gone`() {
        val head = headRuleOffset(headPx = 80f, scrolledPx = 56, thickness = 1f)
        val wrapped = firstRuleOffset(headPx = 80f, scrolledPx = 0, thickness = 1f, pitchPx = 56f)

        assertEquals(wrapped, head, 0f)
    }

    @Test
    fun `should start the page scroll at zero so the first rule seats on the head margin`() {
        val listState = LazyListState()
        composeRule.setContent {
            PaperTheme {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.height(200.dp),
                    contentPadding = PaddingValues(top = 40.dp)
                ) {
                    items(10) { Box(Modifier.height(56.dp)) }
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(0, listState.firstVisibleItemScrollOffset)
    }

    @Test
    fun `should hold the page to a readable width on a wide desk`() {
        composeRule.setContent {
            PaperTheme {
                Box(Modifier.requiredWidth(1000.dp)) {
                    Box(Modifier.pageFrame().testTag(PAGE))
                }
            }
        }

        composeRule.onNodeWithTag(PAGE).assertWidthIsEqualTo(PaperDimens.pageWidth)
    }

    @Test
    fun `should let the page fill a desk narrower than its widest measure`() {
        composeRule.setContent {
            PaperTheme {
                Box(Modifier.requiredWidth(320.dp)) {
                    Box(Modifier.pageFrame().testTag(PAGE))
                }
            }
        }

        composeRule.onNodeWithTag(PAGE).assertWidthIsEqualTo(320.dp)
    }

    @Test
    fun `should round a row up to whole lines so the page stays ruled`() {
        composeRule.setContent {
            PaperTheme {
                Box(Modifier.testTag(SNAPPED).pitchHeight(56.dp).height(60.dp))
            }
        }

        composeRule.onNodeWithTag(SNAPPED).assertHeightIsEqualTo(112.dp)
    }

    @Test
    fun `should give a ruled row exactly one line of the page`() {
        composeRule.setContent {
            PaperTheme {
                RuledRow(modifier = Modifier.testTag(ROW)) { Text("Lait") }
            }
        }

        composeRule.onNodeWithTag(ROW).assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun `should skip exactly one line where the completed section starts`() {
        composeRule.setContent {
            PaperTheme { SectionSkip(completedCount = 3, modifier = Modifier.testTag(SKIP)) }
        }

        composeRule.onNodeWithTag(SKIP).assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun `should press the paper instead of rippling`() {
        val captured = mutableListOf<Any>()
        composeRule.setContent {
            PaperTheme { captured += LocalIndication.current }
        }
        composeRule.waitForIdle()

        assertSame(PaperIndication, captured.single())
    }

    private fun ruledRows(scrolled: Int, headVisible: Boolean = true): List<Int> {
        val page = drawRules(scrolled, headVisible)
        return (0 until PAGE_HEIGHT).filter { y -> page[PAGE_WIDTH / 2, y].alpha > 0f }
    }

    private fun drawRules(scrolled: Int, headVisible: Boolean): PixelMap {
        val bitmap = ImageBitmap(PAGE_WIDTH, PAGE_HEIGHT)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(bitmap),
            Size(PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat())
        ) {
            drawPageRules(
                ruling = PageRuling(
                    color = Color.Black,
                    start = GUTTER.toFloat(),
                    head = 80f,
                    pitch = 56f,
                    thickness = 1f,
                    gap = 3f
                ),
                scrolled = scrolled,
                headVisible = headVisible
            )
        }
        return bitmap.toPixelMap()
    }

    private fun pitchesAtFontScales(vararg scales: Float): List<Dp> {
        val captured = mutableListOf<Dp>()
        composeRule.setContent {
            scales.forEach { scale ->
                CompositionLocalProvider(LocalDensity provides Density(2f, scale)) {
                    captured += pagePitch()
                }
            }
        }
        composeRule.waitForIdle()
        return captured.toList()
    }

    private fun pitchesFor(vararg styles: TextStyle): List<Dp> {
        val captured = mutableListOf<Dp>()
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                styles.forEach { style -> captured += pagePitch(style) }
            }
        }
        composeRule.waitForIdle()
        return captured.toList()
    }

    private companion object {
        const val SNAPPED = "snapped"
        const val ROW = "row"
        const val SKIP = "skip"
        const val PAGE = "page"
        const val PAGE_WIDTH = 200
        const val PAGE_HEIGHT = 400
        const val GUTTER = 40
    }
}
