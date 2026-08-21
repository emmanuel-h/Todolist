package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.ui.paper.PaperTheme
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RenameListDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed = 0
    private var dismissed = 0
    private var pickRequested = 0
    private lateinit var state: MutableState<RenameState>

    @Test
    fun `should pre-fill the dialog with the list's current name`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun `should report the edited name`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Shopping")

        assertEquals("Shopping", current().name)
    }

    @Test
    fun `should show the target caption when the dialog opens with no date`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithText(TARGET_CAPTION).assertIsDisplayed()
    }

    @Test
    fun `should show the due caption when the dialog opens on a list with a due date`() {
        render(RenameState.of(TodoList("1", "Groceries", dueDate = DATE)))

        composeRule.onNodeWithText(DUE_CAPTION).assertIsDisplayed()
    }

    @Test
    fun `should switch to the due caption when the alarm toggle is tapped`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].performClick()

        composeRule.onNodeWithText(DUE_CAPTION).assertIsDisplayed()
    }

    @Test
    fun `should switch back to the target caption when the calendar toggle is tapped`() {
        render(RenameState.of(TodoList("1", "Groceries", dueDate = DATE)))

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[0].performClick()

        composeRule.onNodeWithText(TARGET_CAPTION).assertIsDisplayed()
    }

    @Test
    fun `should carry the date across when the kind is switched`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].performClick()

        assertEquals(DATE, current().selection.dueDate)
        assertNull(current().selection.targetDate)
    }

    @Test
    fun `should show the picked date in the date box`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        val formatted = formatListDate(DATE, showYear = true, locale = Locale.getDefault())
        composeRule.onNodeWithText(formatted).assertIsDisplayed()
    }

    @Test
    fun `should offer no clear affordance while the list has no date`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithContentDescription(CLEAR_TARGET).assertDoesNotExist()
    }

    @Test
    fun `should clear the date when the clear affordance is tapped`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        composeRule.onNodeWithContentDescription(CLEAR_TARGET).performClick()

        assertNull(current().selection.date)
        assertEquals(DateKind.TARGET, current().selection.kind)
    }

    @Test
    fun `should keep the due kind when a due date is cleared`() {
        render(RenameState.of(TodoList("1", "Groceries", dueDate = DATE)))

        composeRule.onNodeWithContentDescription(CLEAR_DUE).performClick()

        assertEquals(DateKind.DUE, current().selection.kind)
        composeRule.onNodeWithText(DUE_CAPTION).assertIsDisplayed()
    }

    @Test
    fun `should ask for a date when the date box is tapped`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[1].performClick()

        assertEquals(1, pickRequested)
    }

    @Test
    fun `should confirm the rename`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithContentDescription(SAVE_NAME).performClick()

        assertEquals(1, confirmed)
    }

    @Test
    fun `should dismiss without renaming`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithContentDescription(CANCEL).performClick()

        assertEquals(1, dismissed)
        assertEquals(0, confirmed)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun render(initial: RenameState) {
        state = mutableStateOf(initial)
        composeRule.setContent { PaperTheme { Dialog() } }
    }

    private fun current(): RenameState = state.value

    @Composable
    private fun Dialog() {
        val current = state.value
        RenameListDialog(
            state = current,
            onNameChange = { state.value = current.copy(name = it) },
            onKindChange = {
                state.value = current.copy(selection = current.selection.withKind(it))
            },
            onPickDate = { pickRequested += 1 },
            onClearDate = { state.value = current.copy(selection = current.selection.cleared()) },
            onDismiss = { dismissed += 1 },
            onConfirm = { confirmed += 1 }
        )
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 3, 14)
        const val TARGET_CAPTION = "To do on this day"
        const val DUE_CAPTION = "Finish before this day"
        const val SET_TARGET_DATE = "Set target date"
        const val SET_DUE_DATE = "Set due date"
        const val CLEAR_TARGET = "Clear target date"
        const val CLEAR_DUE = "Clear due date"
        const val SAVE_NAME = "Save list name"
        const val CANCEL = "Cancel"
    }
}
