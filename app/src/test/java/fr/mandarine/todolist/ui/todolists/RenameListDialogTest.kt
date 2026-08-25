package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
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
    fun `should write no caption beside the date toggles`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        composeRule.onNodeWithText(TARGET_CAPTION).assertDoesNotExist()
        composeRule.onNodeWithText(DUE_CAPTION).assertDoesNotExist()
    }

    @Test
    fun `should carry no confirm row on the sheet`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithContentDescription(SAVE_NAME).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(CANCEL).assertDoesNotExist()
    }

    @Test
    fun `should mark the calendar toggle when the dialog opens with no date`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[0].assertIsSelected()
        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].assertIsNotSelected()
    }

    @Test
    fun `should mark the alarm toggle when the dialog opens on a list with a due date`() {
        render(RenameState.of(TodoList("1", "Groceries", dueDate = DATE)))

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].assertIsSelected()
        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[0].assertIsNotSelected()
    }

    @Test
    fun `should switch to a due date when the alarm toggle is tapped`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].performClick()

        assertEquals(DateKind.DUE, current().selection.kind)
    }

    @Test
    fun `should switch back to a target date when the calendar toggle is tapped`() {
        render(RenameState.of(TodoList("1", "Groceries", dueDate = DATE)))

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[0].performClick()

        assertEquals(DateKind.TARGET, current().selection.kind)
    }

    @Test
    fun `should carry the date across when the kind is switched`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].performClick()

        assertEquals(DATE, current().selection.dueDate)
        assertNull(current().selection.targetDate)
    }

    @Test
    fun `should write the picked date out on the sheet`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        val formatted = formatListDate(DATE, showYear = true, locale = Locale.getDefault())
        composeRule.onNodeWithText(formatted).assertIsDisplayed()
    }

    @Test
    fun `should leave the date line as a bare hint while the list has no date`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithText(HINT).assertIsDisplayed()
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
    }

    @Test
    fun `should ask for a date when the written date is tapped`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[1].performClick()

        assertEquals(1, pickRequested)
    }

    @Test
    fun `should keep the writing when the keyboard finishes the line`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Shopping")
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun `should throw the sheet away rather than commit a blank name`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("   ")
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
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
        const val HINT = "…"
    }
}
