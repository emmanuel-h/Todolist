package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onLast
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
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.UNDO_SLIP_MILLIS
import fr.mandarine.todolist.ui.paper.PaperTheme
import java.time.LocalDate
import java.util.Locale
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
    private val reordered = mutableListOf<List<String>>()
    private val opened = mutableListOf<String>()
    private var replayed = 0
    private var dueDatesSet = 0

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

    // ── The add line ──────────────────────────────────────────────────────────

    @Test
    fun `should put the add line on the page when a sheet is taken from the pad`() {
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(CREATE_LIST).performClick()
        composeRule.waitForIdle()

        assertTrue(screenState.addRowExpanded)
        addLine().assertIsFocused()
    }

    @Test
    fun `should hide the pad and the replay affordance while the add line is on the page`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(CREATE_LIST).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(REPLAY).assertDoesNotExist()
    }

    @Test
    fun `should not show the add line before a sheet is taken`() {
        render(TodoListsState.Empty)

        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    /**
     * The line being written is named but carries no glyph of its own: the
     * keyboard's own Done commits it. A name is not a glyph on the paper.
     */
    fun `should carry no glyph of its own on the add line`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        assertEquals(listOf(ADD_LIST), descriptions())
    }

    @Test
    fun `should create the list when the name is submitted from the keyboard`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        addLine().performTextReplacement("Work")
        addLine().performImeAction()

        assertEquals(listOf(Triple("Work", null, null)), created)
    }

    @Test
    fun `should leave a fresh caret waiting after a list is created`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        addLine().performTextReplacement("Work")
        addLine().performImeAction()
        composeRule.waitForIdle()

        assertTrue(screenState.addRowExpanded)
        assertEquals("", screenState.addRowText)
        addLine().assertIsFocused()
    }

    @Test
    fun `should not create a list when the name is blank`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        addLine().performTextReplacement("   ")
        addLine().performImeAction()

        assertTrue(created.isEmpty())
        assertTrue(screenState.addRowExpanded)
    }

    @Test
    fun `should not create a list when nothing has been typed`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)

        addLine().performImeAction()

        assertTrue(created.isEmpty())
    }

    @Test
    fun `should take the add line off the page when the pen goes down`() {
        screenState.addRowExpanded = true
        render(TodoListsState.Empty)
        addLine().performTextReplacement("Work")

        screenState.closeAddRow()
        composeRule.waitForIdle()

        assertTrue(created.isEmpty())
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun `should finish the line instead of opening a list tapped while the pen is down`() {
        screenState.addRowExpanded = true
        render(content(active = listOf(summary("1", "Groceries"))))
        addLine().performTextReplacement("Work")

        composeRule.onNodeWithText("Groceries").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(Triple("Work", null, null)), created)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `should only put the pen down when an empty line is on the page`() {
        screenState.addRowExpanded = true
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithText("Groceries").performClick()
        composeRule.waitForIdle()

        assertTrue(created.isEmpty())
        assertTrue(opened.isEmpty())
        assertTrue(!screenState.addRowExpanded)
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

    /**
     * The scrap is spent as the window runs down, but a reader aims at where they
     * saw it land. A reach that narrows with the paper turns a delete they meant to
     * take back into one they cannot, and there is no second way to undo.
     */
    @Test
    fun `should keep the undo within reach of where it landed as its paper is spent`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(summary("1", "Groceries"))))

        tearOffGroceries()
        val landed = slipBounds()
        composeRule.mainClock.advanceTimeBy(UNDO_SLIP_MILLIS * SPENT_NUMERATOR / SPENT_DIVISOR)

        assertEquals(landed, slipBounds())
    }

    @Test
    fun `should still put the list back when the undo is tapped late in its window`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(summary("1", "Groceries"))))

        tearOffGroceries()
        composeRule.mainClock.advanceTimeBy(UNDO_SLIP_MILLIS * SPENT_NUMERATOR / SPENT_DIVISOR)
        composeRule.onNodeWithContentDescription(UNDO).performClick()
        slipSettles()

        assertTrue(deleted.isEmpty())
        composeRule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    // ── The jot in a row's margin ─────────────────────────────────────────────

    @Test
    fun `should open the calendar on a row's own day rather than open the list`() {
        render(content(active = listOf(summary("1", "Groceries", dueDate = DATE))))

        composeRule.onNodeWithContentDescription("Due date ${spelled(DATE)}").performClick()

        assertEquals(DateTarget.Row("1"), screenState.datePickerRequest?.target)
        assertEquals(DateKind.DUE, screenState.datePickerRequest?.kind)
        assertEquals(DATE, screenState.datePickerRequest?.initial)
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun `should reach the jot of a finished list below the divider too`() {
        render(
            content(
                active = listOf(summary("1", "Groceries")),
                done = listOf(summary("2", "Jardin", allDone = true, targetDate = DATE))
            )
        )

        composeRule.onNodeWithContentDescription("Target date ${spelled(DATE)}").performClick()

        assertEquals(DateTarget.Row("2"), screenState.datePickerRequest?.target)
    }

    @Test
    fun `should write the day picked for a row back onto that row's list`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(summary("1", "Groceries", targetDate = DATE))))

        composeRule.onNodeWithContentDescription("Target date ${spelled(DATE)}").performClick()
        composeRule.onNodeWithText(DATE.plusDays(1).dayOfMonth.toString()).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("Groceries"), renamed)
        assertEquals(listOf(DATE.plusDays(1) to null), renamedDates)
        assertNull(screenState.datePickerRequest)
    }

    @Test
    fun `should ask for notifications when a day picked for a row lands under the alarm`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(summary("1", "Groceries", dueDate = DATE))))

        composeRule.onNodeWithContentDescription("Due date ${spelled(DATE)}").performClick()
        composeRule.onNodeWithText(DATE.plusDays(1).dayOfMonth.toString()).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(null to DATE.plusDays(1)), renamedDates)
        assertEquals(1, dueDatesSet)
    }

    @Test
    fun `should still open the list when the row carrying a jot is tapped elsewhere`() {
        render(content(active = listOf(summary("1", "Groceries", dueDate = DATE))))

        composeRule.onNodeWithText("Groceries").performClick()

        assertEquals(listOf("Groceries"), opened)
        assertNull(screenState.datePickerRequest)
    }

    @Test
    fun `should still uncover the edit surface of a row carrying a jot`() {
        render(content(active = listOf(summary("1", "Groceries", dueDate = DATE))))

        composeRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(RenameState.of(TodoList("1", "Groceries", dueDate = DATE)), screenState.rename)
        assertNull(screenState.datePickerRequest)
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    @Test
    fun `should open the edit surface when a list row is swiped start to end`() {
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(RenameState.of(TodoList("1", "Groceries")), screenState.rename)
        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[0].assertIsDisplayed()
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

        assertEquals(DateTarget.Rename, screenState.datePickerRequest?.target)
        assertEquals(DateKind.TARGET, screenState.datePickerRequest?.kind)
    }

    @Test
    fun `should clear the date of a list from the edit surface`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries", targetDate = DATE))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNodeWithContentDescription(CLEAR_TARGET_DATE).performClick()
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf(null to null), renamedDates)
        assertNull(screenState.rename)
    }

    @Test
    fun `should rename the list when the edit sheet is put down`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries"))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Shopping")
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf("Shopping"), renamed)
        assertNull(screenState.rename)
    }

    @Test
    fun `should not rename the list when the new name is blank`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries"))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("   ")
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertTrue(renamed.isEmpty())
        assertNull(screenState.rename)
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

        assertEquals(listOf(listOf("2", "1")), reordered)
    }

    /**
     * A torn-off row is hidden on the page but still held by the repository for
     * the length of its undo slip. The order the page hands down must name only
     * the rows it is showing, or the row the reader moved and the row the
     * repository moves are two different lists.
     */
    @Test
    fun `should never name a torn-off list in the order it hands down`() {
        render(
            content(
                active = listOf(
                    summary("1", "Groceries"),
                    summary("2", "Weekend"),
                    summary("3", "Work")
                )
            )
        )

        perform("Groceries", DELETE_LIST)
        perform("Work", MOVE_UP)

        assertEquals(listOf(listOf("3", "2")), reordered)
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

    // ── The date sheet ────────────────────────────────────────────────────────

    @Test
    fun `should write the circled day onto the line that asked for it`() {
        render(content(active = listOf(summary("1", "Groceries"))))
        openDateSheet(DateKind.DUE)

        composeRule.onNodeWithText("20").performClick()
        composeRule.waitForIdle()

        assertEquals(LocalDate.of(2026, 3, 20), screenState.addRowSelection.dueDate)
        assertNull(screenState.datePickerRequest)
    }

    @Test
    fun `should carry no confirm row on the date sheet`() {
        render(content(active = listOf(summary("1", "Groceries"))))
        openDateSheet(DateKind.DUE)

        composeRule.onNodeWithContentDescription(SAVE).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(CANCEL).assertDoesNotExist()
    }

    @Test
    fun `should ask for notifications the first time a due date is written`() {
        render(content(active = listOf(summary("1", "Groceries"))))
        openDateSheet(DateKind.DUE)

        composeRule.onNodeWithText("20").performClick()
        composeRule.waitForIdle()

        assertEquals(1, dueDatesSet)
    }

    @Test
    fun `should not ask for notifications when the day written is a target`() {
        render(content(active = listOf(summary("1", "Groceries"))))
        openDateSheet(DateKind.TARGET)

        composeRule.onNodeWithText("20").performClick()
        composeRule.waitForIdle()

        assertEquals(0, dueDatesSet)
    }

    @Test
    fun `should ask for notifications when the alarm is rung over a day already written`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries", targetDate = DATE))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].performClick()

        assertEquals(DATE, screenState.rename?.selection?.dueDate)
        assertEquals(1, dueDatesSet)
    }

    @Test
    fun `should not ask for notifications when the alarm is rung on a sheet with no day on it`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries"))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].performClick()

        assertEquals(0, dueDatesSet)
    }

    @Test
    fun `should not ask for notifications when a due date is turned back into a target`() {
        screenState.rename = RenameState.of(TodoList("1", "Groceries", dueDate = DATE))
        render(content(active = listOf(summary("1", "Groceries"))))

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[0].performClick()

        assertEquals(0, dueDatesSet)
    }

    // ── The date on the line being written ────────────────────────────────────

    @Test
    fun `should leave the add line a bare rule until words are written on it`() {
        armAddLine()
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(SET_TARGET_DATE).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(SET_DUE_DATE).assertDoesNotExist()
    }

    @Test
    fun `should open the date marks under the line as soon as it is written on`() {
        armAddLine()
        render(TodoListsState.Empty)

        addLine().performTextReplacement("Work")
        composeRule.waitForIdle()

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[0].assertIsSelected()
    }

    @Test
    fun `should ask for a date from the line being written`() {
        armAddLine(text = "Work")
        render(TodoListsState.Empty)

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[1].performClick()

        assertEquals(DateTarget.AddRow, screenState.datePickerRequest?.target)
        assertEquals(DateKind.TARGET, screenState.datePickerRequest?.kind)
    }

    @Test
    fun `should attach the day circled from the add line to the list that line commits`() {
        armAddLine(text = "Work")
        render(TodoListsState.Empty)

        composeRule.onAllNodesWithContentDescription(SET_TARGET_DATE)[1].performClick()
        composeRule.onNodeWithText("20").performClick()
        addLine().performImeAction()

        assertEquals(listOf(Triple("Work", LocalDate.of(2026, 3, 20), null)), created)
    }

    @Test
    fun `should attach a due date jotted on the add line to the list that line commits`() {
        armAddLine(text = "Work", selection = DateSelection(DateKind.TARGET, DATE))
        render(TodoListsState.Empty)

        composeRule.onAllNodesWithContentDescription(SET_DUE_DATE)[0].performClick()
        addLine().performImeAction()

        assertEquals(listOf(Triple("Work", null, DATE)), created)
        assertEquals(1, dueDatesSet)
    }

    @Test
    fun `should rub the date off the line when the strike-out is tapped`() {
        armAddLine(text = "Work", selection = DateSelection(DateKind.TARGET, DATE))
        render(TodoListsState.Empty)

        composeRule.onNodeWithContentDescription(CLEAR_TARGET_DATE).performClick()
        addLine().performImeAction()

        assertEquals(listOf(Triple("Work", null, null)), created)
    }

    @Test
    fun `should take the date marks off the line once the list is written`() {
        armAddLine(text = "Work", selection = DateSelection(DateKind.DUE, DATE))
        render(TodoListsState.Empty)

        addLine().performImeAction()
        composeRule.waitForIdle()

        assertEquals(DateSelection.None, screenState.addRowSelection)
        composeRule.onNodeWithContentDescription(SET_DUE_DATE).assertDoesNotExist()
    }

    private fun tearOffGroceries() {
        composeRule.onNodeWithText("Groceries").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    private fun slipSettles() {
        composeRule.mainClock.advanceTimeBy(SLIP_SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    private fun addLine(): SemanticsNodeInteraction =
        composeRule.onAllNodes(hasSetTextAction()).onLast()

    private fun armAddLine(text: String = "", selection: DateSelection = DateSelection.None) {
        screenState.animationsEnabled = false
        screenState.addRowExpanded = true
        screenState.addRowText = text
        screenState.addRowSelection = selection
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
            onReorder = { orderedIds -> reordered += orderedIds },
            onReplayTutorial = { replayed += 1 },
            onDueDateSet = { dueDatesSet += 1 }
        )
    }

    private fun openDateSheet(kind: DateKind) {
        composeRule.runOnIdle {
            screenState.animationsEnabled = false
            screenState.datePickerRequest = DatePickerRequest(DateTarget.AddRow, kind, null)
        }
        composeRule.waitForIdle()
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

    private fun summary(
        id: String,
        name: String,
        allDone: Boolean = false,
        targetDate: LocalDate? = null,
        dueDate: LocalDate? = null
    ) = TodoListSummary(
        list = TodoList(id, name, targetDate = targetDate, dueDate = dueDate),
        allDone = allDone,
        dueDateStatus = dueDate?.let { DueDateStatus.FUTURE }
    )

    private fun spelled(date: LocalDate): String =
        formatListDate(date, showYear = true, locale = Locale.getDefault(Locale.Category.FORMAT))

    private fun slipBounds(): Rect =
        composeRule.onNodeWithContentDescription(UNDO).fetchSemanticsNode().boundsInRoot

    private fun texts(): List<String> = collectText(composeRule.onRoot().fetchSemanticsNode())

    private fun collectText(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            .map { it.text }
            .filter { it.isNotBlank() } + node.children.flatMap { collectText(it) }

    private fun descriptions(): List<String> =
        collectDescriptions(composeRule.onRoot().fetchSemanticsNode())

    private fun collectDescriptions(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            node.children.flatMap { collectDescriptions(it) }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 3, 14)
        const val REPLAY = "Replay tutorial"
        const val ADD_LIST = "Add a list"
        const val CREATE_LIST = "Create new list"
        const val UNDO = "Undo delete"
        const val SLIP_SETTLE_MILLIS = UNDO_SLIP_MILLIS + 100L
        const val SPENT_NUMERATOR = 3L
        const val SPENT_DIVISOR = 4L
        const val SET_TARGET_DATE = "Set target date"
        const val CLEAR_TARGET_DATE = "Clear target date"
        const val SET_DUE_DATE = "Set due date"
        const val EDIT_NAME = "Edit list name"
        const val DELETE_LIST = "Delete list"
        const val MOVE_UP = "Move up"
        const val MOVE_DOWN = "Move down"
        const val SAVE = "Save"
        const val CANCEL = "Cancel"
    }
}
