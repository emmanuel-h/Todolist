package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.UNDO_SLIP_MILLIS
import fr.mandarine.todolist.ui.paper.PaperTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val screenState = TodoListsScreenState()
    private val created = mutableListOf<Triple<String, LocalDate?, LocalDate?>>()
    private val renamed = mutableListOf<String>()
    private val renamedDates = mutableListOf<Pair<LocalDate?, LocalDate?>>()
    private val deleted = mutableListOf<String>()
    private val reordered = mutableListOf<Pair<Int, Int>>()
    private val opened = mutableListOf<String>()
    private var replayed = 0

    // ── The page at rest ──────────────────────────────────────────────────────

    @Test
    fun `should show every active list`() {
        render(content(active = listOf(summary("1", "Groceries"), summary("2", "Weekend"))))

        composeRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeRule.onNodeWithText("Weekend").assertIsDisplayed()
    }

    @Test
    fun `should show the divider with a count when both sections have lists`() {
        render(
            content(
                active = listOf(summary("1", "Groceries")),
                done = listOf(summary("2", "Old", allDone = true))
            )
        )

        assertEquals(listOf("Groceries", "1", "Old"), texts())
    }

    @Test
    fun `should not show the divider when every list is still active`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        assertEquals(listOf("Groceries"), texts())
    }

    @Test
    fun `should not show the divider when every list is done`() {
        render(content(done = listOf(summary("1", "Old", allDone = true))))

        assertEquals(listOf("Old"), texts())
    }

    @Test
    fun `should place active lists before done ones`() {
        render(
            content(
                active = listOf(summary("1", "Groceries")),
                done = listOf(summary("2", "Old", allDone = true))
            )
        )

        val names = texts().filter { it.toIntOrNull() == null }
        assertEquals(listOf("Groceries", "Old"), names)
    }

    @Test
    fun `should render the staged order instead of the repository order during a drag`() {
        screenState.previewOrder = listOf("2", "1")
        render(content(active = listOf(summary("1", "Groceries"), summary("2", "Weekend"))))

        val names = texts().filter { it.toIntOrNull() == null }
        assertEquals(listOf("Weekend", "Groceries"), names)
    }

    @Test
    fun `should open the tapped list`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithText("Groceries").performClick()

        assertEquals(listOf("Groceries"), opened)
    }

    @Test
    fun `should replay the tutorial when the replay affordance is tapped`() {
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(REPLAY).performClick()

        assertEquals(1, replayed)
    }

    // ── The create row ────────────────────────────────────────────────────────

    @Test
    fun `should show the create row when a sheet is taken from the pad`() {
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(CREATE_LIST).performClick()

        assertTrue(screenState.addRowExpanded)
        composeRule.onNodeWithContentDescription(SUBMIT).assertIsDisplayed()
    }

    @Test
    fun `should hide the pad and the replay affordance while the create row is open`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(CREATE_LIST).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(REPLAY).assertDoesNotExist()
    }

    @Test
    fun `should not show the create row before a sheet is taken`() {
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(SUBMIT).assertDoesNotExist()
    }

    @Test
    fun `should create the list when a name is submitted`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Work")
        composeRule.onNodeWithContentDescription(SUBMIT).performClick()

        assertEquals(listOf(Triple("Work", null, null)), created)
    }

    @Test
    fun `should close the create row after a list is created`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Work")
        composeRule.onNodeWithContentDescription(SUBMIT).performClick()

        assertEquals(false, screenState.addRowExpanded)
    }

    @Test
    fun `should create the list when the name is submitted from the keyboard`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Work")
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf(Triple("Work", null, null)), created)
    }

    @Test
    fun `should not create a list when the name is blank`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("   ")
        composeRule.onNodeWithContentDescription(SUBMIT).performClick()

        assertTrue(created.isEmpty())
        assertTrue(screenState.addRowExpanded)
    }

    @Test
    fun `should not create a list when nothing has been typed`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(SUBMIT).performClick()

        assertTrue(created.isEmpty())
    }

    @Test
    fun `should abandon the typed name when the create row is cancelled`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Work")
        composeRule.onNodeWithContentDescription(CANCEL).performClick()

        assertTrue(created.isEmpty())
        assertEquals(false, screenState.addRowExpanded)
        assertEquals("", screenState.addRowText)
    }

    @Test
    fun `should create the list with the target date picked in the create row`() {
        screenState.addRowExpanded = true
        screenState.addRowSelection = DateSelection(DateKind.TARGET, DATE)
        render(TodoListsState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Work")
        composeRule.onNodeWithContentDescription(SUBMIT).performClick()

        assertEquals(listOf(Triple("Work", DATE, null)), created)
    }

    @Test
    fun `should create the list with the due date picked in the create row`() {
        screenState.addRowExpanded = true
        screenState.addRowSelection = DateSelection(DateKind.DUE, DATE)
        render(TodoListsState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Work")
        composeRule.onNodeWithContentDescription(SUBMIT).performClick()

        assertEquals(listOf(Triple("Work", null, DATE)), created)
    }

    @Test
    fun `should ask for a target date when the calendar affordance is tapped`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[0].performClick()

        assertEquals(
            DatePickerRequest(DateTarget.ADD_ROW, DateKind.TARGET, null),
            screenState.datePickerRequest
        )
    }

    @Test
    fun `should ask for a due date when the alarm affordance is tapped`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].performClick()

        assertEquals(
            DatePickerRequest(DateTarget.ADD_ROW, DateKind.DUE, null),
            screenState.datePickerRequest
        )
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    fun `should take the list off the page without writing the delete through yet`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(summary("1", "Groceries"))))

        tearOffGroceries()

        assertTrue(deleted.isEmpty())
        composeRule.onNodeWithText("Groceries").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(UNDO).assertIsDisplayed()
    }

    @Test
    fun `should write the delete through once the undo slip has settled away`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(summary("1", "Groceries"))))

        tearOffGroceries()
        slipSettles()

        assertEquals(listOf("1"), deleted)
        composeRule.onNodeWithContentDescription(UNDO).assertDoesNotExist()
    }

    @Test
    fun `should put the list back when the undo slip is tapped`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(summary("1", "Groceries"))))

        tearOffGroceries()
        composeRule.onNodeWithContentDescription(UNDO).performClick()
        slipSettles()

        assertTrue(deleted.isEmpty())
        composeRule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun `should offer no undo at all while nothing has been torn off`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithContentDescription(UNDO).assertDoesNotExist()
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    @Test
    fun `should open the edit surface when a list row is swiped start to end`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(RenameState.of(TodoList("1", "Groceries")), screenState.rename)
        composeRule.onNodeWithContentDescription(SAVE_NAME).assertIsDisplayed()
    }

    @Test
    fun `should open the edit surface of a finished list swiped start to end`() {
        render(content(done = listOf(summary("1", "Groceries", allDone = true))))

        composeRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(RenameState.of(TodoList("1", "Groceries")), screenState.rename)
    }

    @Test
    fun `should set a target date from the edit surface`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries"))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[1].performClick()

        assertEquals(DateTarget.RENAME, screenState.datePickerRequest?.target)
        assertEquals(DateKind.TARGET, screenState.datePickerRequest?.kind)
    }

    @Test
    fun `should clear the date of a list from the edit surface`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries", targetDate = DATE))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithContentDescription(CLEAR_TARGET_DATE).performClick()
        composeRule.onNodeWithContentDescription(SAVE_NAME).performClick()

        assertEquals(listOf(null to null), renamedDates)
        assertNull(screenState.rename)
    }

    @Test
    fun `should rename the list when the dialog is confirmed`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries"))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Shopping")
        composeRule.onNodeWithContentDescription(SAVE_NAME).performClick()

        assertEquals(listOf("Shopping"), renamed)
        assertNull(screenState.rename)
    }

    @Test
    fun `should not rename the list when the new name is blank`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries"))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("   ")
        composeRule.onNodeWithContentDescription(SAVE_NAME).performClick()

        assertTrue(renamed.isEmpty())
    }

    // ── Reaching the gestures without one ─────────────────────────────────────

    @Test
    fun `should name every verb a list row answers to`() {
        render(content(active = listOf(summary("1", "Groceries"), summary("2", "Weekend"))))

        assertEquals(
            listOf(EDIT_NAME, DELETE_LIST, MOVE_DOWN),
            verbsOf("Groceries").map { it.label }
        )
    }

    @Test
    fun `should offer no way to move a list past either end of the page`() {
        render(content(active = listOf(summary("1", "Groceries"), summary("2", "Weekend"))))

        assertEquals(listOf(MOVE_DOWN), moveVerbsOf("Groceries"))
        assertEquals(listOf(MOVE_UP), moveVerbsOf("Weekend"))
    }

    @Test
    fun `should leave a finished list nothing to reorder`() {
        render(content(done = listOf(summary("1", "Groceries", allDone = true))))

        assertEquals(listOf(EDIT_NAME, DELETE_LIST), verbsOf("Groceries").map { it.label })
    }

    @Test
    fun `should open the name editor on a list asked to be renamed without a gesture`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        perform("Groceries", EDIT_NAME)

        assertEquals(RenameState.of(TodoList("1", "Groceries")), screenState.rename)
    }

    @Test
    fun `should tear a list off when it is asked to be deleted without a gesture`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(summary("1", "Groceries"))))

        perform("Groceries", DELETE_LIST)

        composeRule.onNodeWithText("Groceries").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(UNDO).assertIsDisplayed()
    }

    @Test
    fun `should reorder a list asked to move without a gesture`() {
        render(content(active = listOf(summary("1", "Groceries"), summary("2", "Weekend"))))

        perform("Weekend", MOVE_UP)

        assertEquals(listOf(1 to 0), reordered)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun verbsOf(text: String): List<CustomAccessibilityAction> =
        composeRule.onNodeWithText(text).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.CustomActions)
            .orEmpty()

    private fun moveVerbsOf(text: String): List<String> =
        verbsOf(text).map { it.label }.filter { it == MOVE_UP || it == MOVE_DOWN }

    private fun perform(text: String, label: String) {
        verbsOf(text).first { it.label == label }.action()
        composeRule.waitForIdle()
    }

    private fun tearOffGroceries() {
        composeRule.onNodeWithText("Groceries").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    private fun slipSettles() {
        composeRule.mainClock.advanceTimeBy(SLIP_SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    private fun render(state: TodoListsState) {
        composeRule.setContent { PaperTheme { Screen(state) } }
    }

    @Composable
    private fun Screen(state: TodoListsState) {
        TodoListsScreen(
            state = state,
            screenState = screenState,
            today = DATE,
            onOpenList = { opened += it.name },
            onCreateList = { name, target, due -> created += Triple(name, target, due) },
            onRenameList = { _, name, target, due ->
                renamed += name
                renamedDates += target to due
            },
            onDeleteList = { deleted += it },
            onReorder = { from, to -> reordered += from to to },
            onReplayTutorial = { replayed += 1 }
        )
    }

    private fun content(
        active: List<TodoListSummary> = emptyList(),
        done: List<TodoListSummary> = emptyList()
    ): TodoListsState =
        if (active.isEmpty() && done.isEmpty()) {
            TodoListsState.Empty
        } else {
            TodoListsState.Content(active, done)
        }

    private fun summary(id: String, name: String, allDone: Boolean = false) =
        TodoListSummary(list = TodoList(id, name), allDone = allDone)

    private fun texts(): List<String> = collectText(composeRule.onRoot().fetchSemanticsNode())

    private fun collectText(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            .map { it.text }
            .filter { it.isNotBlank() } + node.children.flatMap { collectText(it) }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 3, 14)
        const val REPLAY = "Replay tutorial"
        const val CREATE_LIST = "Create new list"
        const val SUBMIT = "Create list"
        const val CANCEL = "Cancel"
        const val UNDO = "Undo delete"
        const val SLIP_SETTLE_MILLIS = UNDO_SLIP_MILLIS + 100L
        const val SAVE_NAME = "Save list name"
        const val SET_TARGET_DATE = "Set target date"
        const val CLEAR_TARGET_DATE = "Clear target date"
        const val SET_DUE_DATE = "Set due date"
        const val EDIT_NAME = "Edit list name"
        const val DELETE_LIST = "Delete list"
        const val MOVE_UP = "Move up"
        const val MOVE_DOWN = "Move down"
    }
}
