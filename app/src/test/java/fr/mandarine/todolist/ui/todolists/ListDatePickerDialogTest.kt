package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import fr.mandarine.todolist.ui.paper.PaperTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The calendar sheet is open to write a day of one kind. With no day on it yet the
 * kind it is asking for is ringed in pencil, so the reader can see which glyph they
 * pressed; the other stays bare.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListDatePickerDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(initial: LocalDate?, kind: DateKind) {
        composeRule.setContent {
            PaperTheme {
                var asked by remember { mutableStateOf(kind) }
                ListDatePickerDialog(
                    listName = "Groceries",
                    initial = initial,
                    today = TODAY,
                    kind = asked,
                    animated = false,
                    onDismiss = {},
                    onPicked = {},
                    onKindAsked = { asked = it },
                    onKindChange = { asked = it },
                    onCleared = {}
                )
            }
        }
    }

    @Test
    fun `should ring the calendar when the sheet asks for a target day with no day yet`() {
        render(initial = null, kind = DateKind.TARGET)

        composeRule.onNodeWithContentDescription(SET_TARGET_DATE).assertIsSelected()
        composeRule.onNodeWithContentDescription(SET_DUE_DATE).assertIsNotSelected()
    }

    @Test
    fun `should ring the alarm when the sheet asks for a due day with no day yet`() {
        render(initial = null, kind = DateKind.DUE)

        composeRule.onNodeWithContentDescription(SET_DUE_DATE).assertIsSelected()
        composeRule.onNodeWithContentDescription(SET_TARGET_DATE).assertIsNotSelected()
    }

    @Test
    fun `should move the ring to the alarm when it is pressed on a sheet with no day yet`() {
        render(initial = null, kind = DateKind.TARGET)

        composeRule.onNodeWithContentDescription(SET_DUE_DATE).performClick()

        composeRule.onNodeWithContentDescription(SET_DUE_DATE).assertIsSelected()
        composeRule.onNodeWithContentDescription(SET_TARGET_DATE).assertIsNotSelected()
    }

    @Test
    fun `should ring only the kind of the day once a day is written`() {
        render(initial = TODAY, kind = DateKind.DUE)

        composeRule.onNodeWithContentDescription(CLEAR_DUE_DATE).assertIsSelected()
        composeRule.onNodeWithContentDescription(SET_TARGET_DATE).assertIsNotSelected()
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 3)
        const val SET_TARGET_DATE = "Set target date"
        const val SET_DUE_DATE = "Set due date"
        const val CLEAR_DUE_DATE = "Clear due date"
    }
}
