package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val WORDS = "words"
private const val PAGE = "page"
private const val DP_TOLERANCE = 1f

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaperDeskTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should give a phone the whole window to write on`() {
        assertEquals(PageFit.FillsTheWindow, pageFitFor(WindowSizeClass(411, 914)))
    }

    @Test
    fun `should keep margins once the window is wider than a phone`() {
        assertEquals(PageFit.KeepsMargins, pageFitFor(WindowSizeClass(600, 480)))
    }

    @Test
    fun `should lay the page on a desk once the window is wider than any sheet`() {
        assertEquals(PageFit.LiesOnADesk, pageFitFor(WindowSizeClass(840, 480)))
    }

    @Test
    fun `should lay a phone in landscape on a desk rather than pushing its page aside`() {
        assertEquals(PageFit.LiesOnADesk, pageFitFor(WindowSizeClass(914, 411)))
    }

    @Test
    fun `should rule a phone page with the narrow margin`() {
        assertEquals(PaperDimens.gutter, gutterFor(PageFit.FillsTheWindow))
    }

    @Test
    fun `should widen the margin with the sheet once there is room for it`() {
        assertEquals(PaperDimens.wideGutter, gutterFor(PageFit.KeepsMargins))
        assertEquals(PaperDimens.wideGutter, gutterFor(PageFit.LiesOnADesk))
    }

    @Test
    fun `should indent a ruled row by the margin the page is ruled with`() {
        val indent = rowIndent(PaperDimens.wideGutter)

        assertEquals(PaperDimens.wideGutter.value, indent.value, DP_TOLERANCE)
    }

    @Test
    fun `should indent a ruled row by the narrow margin on a phone page`() {
        val indent = rowIndent(PaperDimens.gutter)

        assertEquals(PaperDimens.gutter.value, indent.value, DP_TOLERANCE)
    }

    @Test
    fun `should never let the page grow wider than a sheet whatever the desk`() {
        composeRule.setContent {
            PaperTheme {
                Box(modifier = Modifier.requiredWidth(1000.dp)) {
                    Box(modifier = Modifier.pageFrame().testTag(PAGE))
                }
            }
        }

        composeRule.onNodeWithTag(PAGE).assertWidthIsEqualTo(PaperDimens.pageWidth)
    }

    @Test
    fun `should read the page fit off the window the theme is shown in`() {
        var fit: PageFit? = null
        var gutter: Dp? = null
        composeRule.setContent {
            PaperTheme {
                fit = LocalPageFit.current
                gutter = LocalPaperGutter.current
            }
        }

        assertEquals(PageFit.FillsTheWindow, fit)
        assertEquals(PaperDimens.gutter, gutter)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w1000dp-h800dp")
    fun `should lay the page on a desk when the window it is shown in is one`() {
        var fit: PageFit? = null
        var gutter: Dp? = null
        composeRule.setContent {
            PaperTheme {
                fit = LocalPageFit.current
                gutter = LocalPaperGutter.current
            }
        }

        assertEquals(PageFit.LiesOnADesk, fit)
        assertEquals(PaperDimens.wideGutter, gutter)
    }

    private fun rowIndent(gutter: Dp): Dp {
        composeRule.setContent {
            PaperTheme {
                CompositionLocalProvider(LocalPaperGutter provides gutter) {
                    RuledRow { Text(text = WORDS, modifier = Modifier.testTag(WORDS)) }
                }
            }
        }
        return composeRule.onNodeWithTag(WORDS).getUnclippedBoundsInRoot().left
    }
}
