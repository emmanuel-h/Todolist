package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The per-list reminder clock: rub-out visibility, pick callbacks, and dismiss.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderClockTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Rub-out button ────────────────────────────────────────────────────────

    @Test
    fun `should show the rub out button when onRubOut is provided`() {
        composeRule.setContent {
            PaperTheme {
                ReminderClockPickerDialog(
                    currentMinuteOfDay = 8 * 60,
                    onMinuteOfDayPicked = {},
                    onDismiss = {},
                    animated = false,
                    onRubOut = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription(RUB_OUT).assertIsDisplayed()
    }

    @Test
    fun `should not show the rub out button when onRubOut is null`() {
        composeRule.setContent {
            PaperTheme {
                ReminderClockPickerDialog(
                    currentMinuteOfDay = 8 * 60,
                    onMinuteOfDayPicked = {},
                    onDismiss = {},
                    animated = false,
                    onRubOut = null
                )
            }
        }

        composeRule.onNodeWithContentDescription(RUB_OUT).assertDoesNotExist()
    }

    @Test
    fun `should call onRubOut when the rub out button is pressed`() {
        var rubOutCalled = false
        composeRule.setContent {
            PaperTheme {
                ReminderClockPickerDialog(
                    currentMinuteOfDay = 8 * 60,
                    onMinuteOfDayPicked = {},
                    onDismiss = {},
                    animated = false,
                    onRubOut = { rubOutCalled = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription(RUB_OUT).performClick()

        assert(rubOutCalled) { "Expected onRubOut to be called" }
    }

    // ── Pick callbacks ────────────────────────────────────────────────────────

    @Test
    fun `should call onMinuteOfDayPicked with correct value when hour then minute are selected`() {
        val picks = mutableListOf<Int>()
        composeRule.setContent {
            PaperTheme {
                ReminderClockPickerDialog(
                    currentMinuteOfDay = 8 * 60,
                    onMinuteOfDayPicked = { picks += it },
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithContentDescription(AFTERNOON).performClick()
        composeRule.onNodeWithContentDescription(HOUR_3).performClick()
        composeRule.onNodeWithContentDescription(MINUTE_0).performClick()

        assertEquals(listOf(15 * 60), picks)
    }

    @Test
    fun `should not call onMinuteOfDayPicked before both hour and minute are selected`() {
        val picks = mutableListOf<Int>()
        composeRule.setContent {
            PaperTheme {
                ReminderClockPickerDialog(
                    currentMinuteOfDay = 8 * 60,
                    onMinuteOfDayPicked = { picks += it },
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithContentDescription(HOUR_8).performClick()

        assertEquals(emptyList<Int>(), picks)
    }

    @Test
    fun `should show the picker title`() {
        composeRule.setContent {
            PaperTheme {
                ReminderClockPickerDialog(
                    currentMinuteOfDay = 8 * 60,
                    onMinuteOfDayPicked = {},
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithText(PICKER_TITLE).assertIsDisplayed()
    }

    @Test
    fun `should pre-ring the current hour`() {
        composeRule.setContent {
            PaperTheme {
                ReminderClockPickerDialog(
                    currentMinuteOfDay = 8 * 60,
                    onMinuteOfDayPicked = {},
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithContentDescription(HOUR_8).assertIsSelected()
    }

    private companion object {
        const val RUB_OUT = "Rub out"
        const val PICKER_TITLE = "When shall I remind you?"
        const val AFTERNOON = "Afternoon"
        const val HOUR_3 = "3"
        const val HOUR_8 = "8"
        const val MINUTE_0 = "0"
    }
}
