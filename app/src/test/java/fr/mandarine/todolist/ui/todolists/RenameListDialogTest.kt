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
    private val pickRequested = mutableListOf<DateKind>()
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
    fun `should write no caption until a kind is pressed`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        composeRule.onNodeWithText(TARGET_CAPTION).assertDoesNotExist()
        composeRule.onNodeWithText(DUE_CAPTION).assertDoesNotExist()
    }

    /**
     * The calendar and the alarm are the one pair a glyph cannot tell apart, so
     * choosing one says in words which was chosen. It is the same sentence the
     * tutorial teaches with, so the reader who skipped the tour is told the same
     * thing in the same voice.
     */
    @Test
    fun `should say which kind was chosen when a kind is pressed`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        composeRule.onNodeWithContentDescription(SET_DUE_DATE).performClick()

        composeRule.onNodeWithText(DUE_CAPTION).assertIsDisplayed()
    }

    @Test
    fun `should carry no confirm row on the sheet`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithContentDescription(SAVE_NAME).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(CANCEL).assertDoesNotExist()
    }

    /**
     * A ring means a day. With no day written there is nothing for a ring to mean,
     * so a sheet opened on a dateless list must not look like it already carries
     * a target date.
     */
    @Test
    fun `should ring neither mark when the sheet opens on a list with no date`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithContentDescription(SET_TARGET_DATE).assertIsNotSelected()
        composeRule.onNodeWithContentDescription(SET_DUE_DATE).assertIsNotSelected()
    }

    @Test
    fun `should ring the alarm when the sheet opens on a list with a due date`() {
        render(RenameState.of(TodoList("1", "Groceries", dueDate = DATE)))

        composeRule.onNodeWithContentDescription(CLEAR_DUE).assertIsSelected()
        composeRule.onNodeWithContentDescription(SET_TARGET_DATE).assertIsNotSelected()
    }

    @Test
    fun `should ask for a day rather than ring a kind while the list has no date`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithContentDescription(SET_DUE_DATE).performClick()

        assertEquals(listOf(DateKind.DUE), pickRequested)
        assertNull(current().selection.date)
    }

    @Test
    fun `should switch back to a target date when the calendar toggle is tapped`() {
        render(RenameState.of(TodoList("1", "Groceries", dueDate = DATE)))

        composeRule.onNodeWithContentDescription(SET_TARGET_DATE).performClick()

        assertEquals(DateKind.TARGET, current().selection.kind)
    }

    @Test
    fun `should carry the date across when the kind is switched`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        composeRule.onNodeWithContentDescription(SET_DUE_DATE).performClick()

        assertEquals(DATE, current().selection.dueDate)
        assertNull(current().selection.targetDate)
    }

    @Test
    fun `should write the picked date out on the sheet`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        val formatted = formatListDate(DATE, showYear = true, locale = Locale.getDefault())
        composeRule.onNodeWithText(formatted).assertIsDisplayed()
    }

    /**
     * The placeholder that used to sit here said only that the reader had not done
     * anything yet, and next to two glyphs it read as stray punctuation.
     */
    @Test
    fun `should write nothing beside the marks while the list has no date`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithText(HINT).assertDoesNotExist()
    }

    @Test
    fun `should offer no clear affordance while the list has no date`() {
        render(RenameState.of(TodoList("1", "Groceries")))

        composeRule.onNodeWithContentDescription(CLEAR_TARGET).assertDoesNotExist()
    }

    @Test
    fun `should rub the date out when the ringed mark is pressed again`() {
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
    fun `should ask for a day when the date already written is pressed`() {
        render(RenameState.of(TodoList("1", "Groceries", targetDate = DATE)))

        composeRule.onNodeWithContentDescription(writtenTarget()).performClick()

        assertEquals(listOf(DateKind.TARGET), pickRequested)
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

    private fun writtenTarget(): String =
        "Target date " + formatListDate(DATE, showYear = true, locale = Locale.getDefault())

    @Composable
    private fun Dialog() {
        val current = state.value
        RenameListDialog(
            state = current,
            onNameChange = { state.value = current.copy(name = it) },
            onKindChange = {
                state.value = current.copy(selection = current.selection.withKind(it))
            },
            onPickDate = { pickRequested += it },
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
