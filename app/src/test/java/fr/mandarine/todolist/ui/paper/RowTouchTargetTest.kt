package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.listmeta.DateJot
import fr.mandarine.todolist.ui.todolists.DateKind
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A rule is smaller than a finger, so every control written on one has to be one
 * rule tall to the layout and a whole touch target to the hand. The two pull
 * against each other: flooring the height instead pushed the row onto a second
 * rule of writing it did not have, and every row on the page grew by half again.
 *
 * These pin both halves at once. Weakening either one is the regression.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RowTouchTargetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should give a control on a rule a whole finger to be pressed by`() {
        composeRule.setContent {
            PaperTheme {
                RuledRow {
                    Text("Buy bread")
                    InkIconButton(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = DELETE,
                        onClick = {},
                        seat = IconSeat.OnRule,
                        foot = GlyphFoot.trash
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(DELETE).assertHeightIsAtLeast(TOUCH_TARGET)
    }

    @Test
    fun `should give the completion ring a whole finger to be pressed by`() {
        composeRule.setContent {
            PaperTheme {
                RuledRow {
                    InkRing(
                        checked = false,
                        onToggle = {},
                        seed = 1,
                        contentDescription = TICK,
                        stateDescription = "not done"
                    )
                    Text("Buy bread")
                }
            }
        }

        composeRule.onNodeWithContentDescription(TICK).assertHeightIsAtLeast(TOUCH_TARGET)
    }

    /**
     * The whole point of the pressable overflow: the control reaches into the blank
     * rule the row already carries, and the row does not grow to meet it.
     */
    @Test
    fun `should keep a one-line row two rules tall however tall its controls are`() {
        var pitch = 0.dp
        composeRule.setContent {
            PaperTheme {
                pitch = LocalPagePitch.current
                Column {
                    RuledRow(modifier = Modifier.testTag(ROW)) {
                        Text("Buy bread")
                        InkIconButton(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = DELETE,
                            onClick = {},
                            seat = IconSeat.OnRule,
                            foot = GlyphFoot.trash
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag(ROW).assertHeightIsEqualTo(pitch * 2)
    }

    /**
     * The jot is a control too — it opens the calendar — and it was the one on-rule
     * control with only its own line of text to be pressed by, so a thumb aiming at
     * it and landing a little low opened the list instead.
     *
     * Asserted by pressing rather than by measuring: the jot says the date and
     * answers the tap on one node, so the node that carries the description is the
     * outer one and still measures a rule. What grew is the area that answers.
     */
    @Test
    fun `should answer a press below the jot's own line`() {
        var rewritten = false
        composeRule.setContent {
            PaperTheme {
                Box(Modifier.testTag(ROW).height(TOUCH_TARGET)) {
                    DateJot(
                        date = LocalDate.of(2026, 3, 15),
                        kind = DateKind.DUE,
                        showYear = false,
                        tint = LocalPaperPalette.current.inked(InkTone.Margin),
                        onRewrite = { rewritten = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag(ROW).performTouchInput {
            click(Offset(left + 1f, bottom - 1f))
        }
        composeRule.waitForIdle()

        assertTrue(rewritten)
    }

    @Test
    fun `should keep a row with a jot two rules tall`() {
        var pitch = 0.dp
        composeRule.setContent {
            PaperTheme {
                pitch = LocalPagePitch.current
                Column {
                    RuledRow(modifier = Modifier.testTag(ROW)) {
                        Text("Groceries")
                        DateJot(
                            date = LocalDate.of(2026, 3, 15),
                            kind = DateKind.DUE,
                            showYear = false,
                            tint = LocalPaperPalette.current.inked(InkTone.Margin),
                            onRewrite = {}
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag(ROW).assertHeightIsEqualTo(pitch * 2)
    }

    private companion object {
        val TOUCH_TARGET = 48.dp
        const val DELETE = "delete"
        const val TICK = "tick"
        const val ROW = "row"
    }
}
