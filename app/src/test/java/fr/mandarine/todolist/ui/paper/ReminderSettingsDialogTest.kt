package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.todolists.TodoListsScreen
import fr.mandarine.todolist.ui.todolists.TodoListsScreenState
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bell and the two dialogs it raises: the settings slip and the hour
 * grid. Each test addresses exactly one behaviour so regressions are easy to name.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSettingsDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Bell on the page ──────────────────────────────────────────────────────

    @Test
    fun `should display the bell on the empty lists page`() {
        composeRule.setContent { PaperTheme { ListsScreen() } }

        composeRule.onNodeWithContentDescription(SETTINGS).assertIsDisplayed()
    }

    @Test
    fun `should give the bell a touch target of at least 48 dp`() {
        composeRule.setContent { PaperTheme { ListsScreen() } }

        composeRule.onNodeWithContentDescription(SETTINGS).assertHeightIsAtLeast(TOUCH_FLOOR)
    }

    /**
     * When the add line opens the masthead strip clears; the bell goes with it so
     * the strip does not compete for the top-end corner with the keyboard.
     */
    @Test
    fun `should hide the bell while the add row is open`() {
        composeRule.setContent {
            PaperTheme {
                ListsScreen(addRowOpen = true)
            }
        }

        composeRule.onNodeWithContentDescription(SETTINGS).assertDoesNotExist()
    }

    // ── Settings slip ─────────────────────────────────────────────────────────

    @Test
    fun `should open the settings slip when the bell is pressed`() {
        composeRule.setContent { PaperTheme { ListsScreen() } }

        composeRule.onNodeWithContentDescription(SETTINGS).performClick()

        composeRule.onNodeWithText(REMINDERS).assertIsDisplayed()
    }

    @Test
    fun `should show the done button in the settings slip`() {
        composeRule.setContent {
            PaperTheme {
                ListsScreen(settingsOpen = true)
            }
        }

        composeRule.onNodeWithContentDescription(DONE).assertIsDisplayed()
    }

    @Test
    fun `should close the settings slip when done is pressed`() {
        composeRule.setContent { PaperTheme { ListsScreen() } }

        composeRule.onNodeWithContentDescription(SETTINGS).performClick()
        composeRule.onNodeWithText(REMINDERS).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(DONE).performClick()

        composeRule.onNodeWithText(REMINDERS).assertDoesNotExist()
    }

    // ── Hour grid ─────────────────────────────────────────────────────────────

    @Test
    fun `should open the hour picker when the time row is pressed`() {
        composeRule.setContent {
            PaperTheme {
                ReminderSettingsDialog(
                    reminderTime = DEFAULT_TIME,
                    onSetReminderTime = {},
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithText(PICKER_TITLE).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(TIME_ROW_LABEL, substring = true).performClick()

        composeRule.onNodeWithText(PICKER_TITLE).assertIsDisplayed()
    }

    @Test
    fun `should call onSetReminderTime with hour times 60 when an hour then minute zero is selected`() {
        val calls = mutableListOf<Int>()
        composeRule.setContent {
            PaperTheme {
                ReminderSettingsDialog(
                    reminderTime = DEFAULT_TIME,
                    onSetReminderTime = { calls += it },
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithContentDescription(TIME_ROW_LABEL, substring = true).performClick()
        composeRule.onNodeWithContentDescription(AFTERNOON).performClick()
        composeRule.onNodeWithContentDescription(HOUR_2).performClick()
        composeRule.onNodeWithContentDescription(MINUTE_0).performClick()

        assertEquals(listOf(14 * 60), calls)
    }

    @Test
    fun `should close the clock picker after both hour and minute are selected`() {
        composeRule.setContent {
            PaperTheme {
                ReminderSettingsDialog(
                    reminderTime = DEFAULT_TIME,
                    onSetReminderTime = {},
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithContentDescription(TIME_ROW_LABEL, substring = true).performClick()
        composeRule.onNodeWithText(PICKER_TITLE).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(AFTERNOON).performClick()
        composeRule.onNodeWithContentDescription(HOUR_2).performClick()
        composeRule.onNodeWithText(PICKER_TITLE).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(MINUTE_0).performClick()

        composeRule.onNodeWithText(PICKER_TITLE).assertDoesNotExist()
    }

    @Test
    fun `should call onSetReminderTime with 450 when hour 7 then minute 30 is selected`() {
        val calls = mutableListOf<Int>()
        composeRule.setContent {
            PaperTheme {
                ReminderSettingsDialog(
                    reminderTime = DEFAULT_TIME,
                    onSetReminderTime = { calls += it },
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithContentDescription(TIME_ROW_LABEL, substring = true).performClick()
        composeRule.onNodeWithContentDescription(HOUR_7).performClick()
        composeRule.onNodeWithContentDescription(MINUTE_30).performClick()

        assertEquals(listOf(7 * 60 + 30), calls)
    }

    @Test
    fun `should show the current hour as selected when the clock opens`() {
        composeRule.setContent {
            PaperTheme {
                ReminderSettingsDialog(
                    reminderTime = DEFAULT_TIME,
                    onSetReminderTime = {},
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithContentDescription(TIME_ROW_LABEL, substring = true).performClick()

        composeRule.onNodeWithContentDescription(HOUR_8).assertIsSelected()
    }

    @Test
    fun `should not call onSetReminderTime before any selection is made`() {
        val calls = mutableListOf<Int>()
        composeRule.setContent {
            PaperTheme {
                ReminderSettingsDialog(
                    reminderTime = DEFAULT_TIME,
                    onSetReminderTime = { calls += it },
                    onDismiss = {},
                    animated = false
                )
            }
        }

        composeRule.onNodeWithContentDescription(TIME_ROW_LABEL, substring = true).performClick()
        composeRule.onNodeWithText(PICKER_TITLE).assertIsDisplayed()

        assertEquals(emptyList<Int>(), calls)
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 1, 1)
        val DEFAULT_TIME: LocalTime = LocalTime.of(8, 0)
        val TOUCH_FLOOR = 48.dp
        const val SETTINGS = "Reminder time"
        const val REMINDERS = "Reminders"
        const val DONE = "Done"
        const val PICKER_TITLE = "When shall I remind you?"
        const val TIME_ROW_LABEL = "Every day at"
        const val HOUR_7 = "7"
        const val HOUR_8 = "8"
        const val HOUR_2 = "2"
        const val AFTERNOON = "Afternoon"
        const val MINUTE_0 = "0"
        const val MINUTE_30 = "30"
    }

    @androidx.compose.runtime.Composable
    private fun ListsScreen(
        addRowOpen: Boolean = false,
        settingsOpen: Boolean = false,
        reminderTime: LocalTime = DEFAULT_TIME,
        onSetReminderTime: (Int) -> Unit = {}
    ) {
        val state = remember {
            TodoListsScreenState().also {
                it.addRowExpanded = addRowOpen
                it.settingsOpen = settingsOpen
            }
        }
        TodoListsScreen(
            state = TodoListsState.Empty,
            screenState = state,
            today = TODAY,
            onOpenList = {},
            onCreateList = { _, _, _ -> },
            onRenameList = { _, _, _, _, _ -> },
            onDeleteList = {},
            onReorder = {},
            reminderTime = reminderTime,
            onSetReminderTime = onSetReminderTime
        )
    }
}
