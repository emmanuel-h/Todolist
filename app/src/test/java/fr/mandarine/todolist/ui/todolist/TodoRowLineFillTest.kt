package fr.mandarine.todolist.ui.todolist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The page's hand breaks lines `Balanced`, which evens the lines out rather than
 * filling the first. That is right for prose and wrong for writing on a rule with
 * marks after it: the first line stops short and the gap before the pencil reads as
 * rule the writing was not allowed to use.
 *
 * #81 measured this on the page of lists and fixed it there with `fillingTheLine`.
 * The items page kept the balanced break, so a long item stopped a third of the way
 * short of its own controls. Nothing failed when that fix was not carried across,
 * which is what this is for.
 *
 * It reads the pixels because the wrap point is not in the semantics tree: what is
 * being asserted is where the ink stops on the first rule.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TodoRowLineFillTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should fill the first rule up to the controls before wrapping`() {
        val filled = firstLineFill()

        assertTrue(
            "the first line stopped at ${(filled * PERCENT).toInt()}% of the rule it had",
            filled > FILLED_ENOUGH
        )
    }

    private fun firstLineFill(): Float {
        var trailing = 0
        var leading = 0
        var pitch = 0
        composeRule.setContent {
            PaperTheme {
                with(LocalDensity.current) {
                    trailing = (PaperDimens.rowEndPadding + PaperDimens.rowGlyphButton * 2).roundToPx()
                    leading = (PaperDimens.gutter + PaperDimens.iconButton).roundToPx()
                    pitch = LocalPagePitch.current.roundToPx()
                }
                Box(Modifier.fillMaxWidth().testTag(ROW)) {
                    TodoRow(
                        item = TodoItem("1", LONG_TITLE, "list-1"),
                        checked = false,
                        editing = false,
                        onToggle = {},
                        onEditRequested = {},
                        onEditCommitted = {},
                        onEditDismissed = {},
                        onDeleteRequested = {},
                        animated = false
                    )
                }
            }
        }

        val pixels = composeRule.onNodeWithTag(ROW).captureToImage().toPixelMap()
        val controlsLeft = pixels.width - trailing
        var written = leading
        for (y in 0 until minOf(pitch, pixels.height)) {
            for (x in leading until minOf(controlsLeft, pixels.width)) {
                if (pixels[x, y].luminance() < INK) written = maxOf(written, x)
            }
        }
        return (written - leading).toFloat() / (controlsLeft - leading)
    }

    private companion object {
        const val ROW = "row"
        const val INK = 0.45f
        const val PERCENT = 100
        const val FILLED_ENOUGH = 0.9f
        const val LONG_TITLE = "Cdsc fef wef wef ewfe wef wef wef wef wef wef wef w"
    }
}
