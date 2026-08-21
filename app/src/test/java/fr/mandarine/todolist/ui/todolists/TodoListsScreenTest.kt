package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListsState
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

        assertEquals(listOf("Groceries", "0", "0", "1", "Old", "0", "0"), texts())
    }

    @Test
    fun `should not show the divider when every list is still active`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        assertEquals(listOf("Groceries", "0", "0"), texts())
    }

    @Test
    fun `should not show the divider when every list is done`() {
        render(content(done = listOf(summary("1", "Old", allDone = true))))

        assertEquals(listOf("Old", "0", "0"), texts())
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
    fun `should arm the delete on the tapped row without deleting it`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithContentDescription(DELETE_LIST).performClick()

        assertEquals("1", screenState.confirmingDeleteListId)
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun `should delete the list when the confirm ring is tapped`() {
        screenState.confirmingDeleteListId = "1"
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithContentDescription(CONFIRM_DELETE).performClick()

        assertEquals(listOf("1"), deleted)
        assertNull(screenState.confirmingDeleteListId)
    }

    @Test
    fun `should keep the list when the delete is cancelled`() {
        screenState.confirmingDeleteListId = "1"
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithContentDescription(CANCEL).performClick()

        assertTrue(deleted.isEmpty())
        assertNull(screenState.confirmingDeleteListId)
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    @Test
    fun `should open the rename dialog on the list's own name`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithContentDescription(EDIT_LIST).performClick()

        assertEquals("Groceries", screenState.rename?.name)
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

    // ── Drag ──────────────────────────────────────────────────────────────────

    @Test
    fun `should stage a new order while a row is dragged down`() {
        render(content(active = listOf(summary("1", "Groceries"), summary("2", "Weekend"))))

        composeRule.onAllNodesWithContentDescription(DRAG_HANDLE)[0].performTouchInput {
            down(center)
            moveBy(Offset(0f, 400f))
        }
        composeRule.mainClock.advanceTimeBy(500L)

        assertEquals(listOf("2", "1"), screenState.previewOrder)
    }

    @Test
    fun `should commit the reorder when the dragged row is released`() {
        render(content(active = listOf(summary("1", "Groceries"), summary("2", "Weekend"))))

        composeRule.onAllNodesWithContentDescription(DRAG_HANDLE)[0].performTouchInput {
            down(center)
            moveBy(Offset(0f, 400f))
            up()
        }

        assertEquals(listOf(0 to 1), reordered)
    }

    @Test
    fun `should not commit a reorder when the row is released where it started`() {
        render(content(active = listOf(summary("1", "Groceries"), summary("2", "Weekend"))))

        composeRule.onAllNodesWithContentDescription(DRAG_HANDLE)[0].performTouchInput {
            down(center)
            moveBy(Offset(0f, 4f))
            up()
        }

        assertTrue(reordered.isEmpty())
        assertNull(screenState.previewOrder)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
            onRenameList = { _, name, _, _ -> renamed += name },
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
        const val CONFIRM_DELETE = "Delete"
        const val DELETE_LIST = "Delete list"
        const val EDIT_LIST = "Edit list name"
        const val SAVE_NAME = "Save list name"
        const val SET_TARGET_DATE = "Set target date"
        const val SET_DUE_DATE = "Set due date"
        const val DRAG_HANDLE = "Drag to reorder"
    }
}
