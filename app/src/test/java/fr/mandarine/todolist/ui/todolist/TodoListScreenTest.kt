package fr.mandarine.todolist.ui.todolist

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListState
import java.time.LocalDate
import java.util.Locale
import fr.mandarine.todolist.ui.UNDO_SLIP_MILLIS
import fr.mandarine.todolist.ui.paper.PaperPalette
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.DateSelection
import fr.mandarine.todolist.ui.todolists.formatListDate
import fr.mandarine.todolist.ui.paper.PaperTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val screenState = TodoListScreenState()
    private val toggled = mutableListOf<String>()
    private val deleted = mutableListOf<String>()
    private val edited = mutableListOf<Pair<String, String>>()
    private val submitted = mutableListOf<String>()
    private val reordered = mutableListOf<List<String>>()
    private val renamed = mutableListOf<String>()
    private val datesWritten = mutableListOf<DateSelection>()
    private var backPressed = 0
    private var hostView: View? = null

    @Test
    fun `should rewrite the name of the list when the head line is tapped`() {
        render(content(active = listOf(item("1", "Apples"))), listName = "Groceries")

        composeRule.onNodeWithText("Groceries").performClick()
        editField().performTextReplacement("Provisions")
        editField().performImeAction()

        assertEquals(listOf("Provisions"), renamed)
    }

    @Test
    fun `should leave the name of the list alone when the rewrite is blank`() {
        render(content(active = listOf(item("1", "Apples"))), listName = "Groceries")

        composeRule.onNodeWithText("Groceries").performClick()
        editField().performTextReplacement(" ")
        editField().performImeAction()

        assertEquals(emptyList<String>(), renamed)
        assertFalse(screenState.renamingList)
    }

    @Test
    fun `should open the calendar on the day the jot already carries`() {
        render(
            content(active = listOf(item("1", "Apples"))),
            TodoListSummary(
                list = TodoList(LIST_ID, "Groceries", dueDate = TODAY),
                allDone = false,
                dueDateStatus = DueDateStatus.TODAY
            )
        )

        composeRule
            .onNodeWithContentDescription("Due date ${formatListDate(TODAY, true, locale())}")
            .performClick()

        assertEquals(DateSelection(DateKind.DUE, TODAY), screenState.dateSheet)
    }

    /**
     * The jot is a control, and it has to say so on the node that says the date —
     * a press hung on the line around it is a mark a screen reader can read but
     * not press, beside a press it cannot name.
     */
    @Test
    fun `should offer the head rule's jot as a press a screen reader can name and make`() {
        render(
            content(active = listOf(item("1", "Apples"))),
            TodoListSummary(
                list = TodoList(LIST_ID, "Groceries", dueDate = TODAY),
                allDone = false,
                dueDateStatus = DueDateStatus.TODAY
            )
        )

        val jot = composeRule
            .onNodeWithContentDescription("Due date ${formatListDate(TODAY, true, locale())}")

        jot.assertHasClickAction()
        assertEquals(
            "Set due date",
            jot.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label
        )
        jot.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(DateSelection(DateKind.DUE, TODAY), screenState.dateSheet)
    }

    @Test
    fun `should write the day picked on the calendar back onto the list`() {
        screenState.animationsEnabled = false
        render(
            content(active = listOf(item("1", "Apples"))),
            TodoListSummary(
                list = TodoList(LIST_ID, "Groceries", targetDate = TODAY),
                allDone = false
            )
        )

        composeRule
            .onNodeWithContentDescription("Target date ${formatListDate(TODAY, true, locale())}")
            .performClick()
        composeRule.onNodeWithText(TODAY.plusDays(1).dayOfMonth.toString()).performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(DateSelection(DateKind.TARGET, TODAY.plusDays(1))),
            datesWritten
        )
        assertNull(screenState.dateSheet)
    }

    @Test
    fun `should write the list name on the first line of the page`() {
        render(content(active = listOf(item("1", "Apples"))), listName = "Groceries")

        composeRule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun `should navigate back when the back affordance is tapped`() {
        render(TodoListState.Empty)

        composeRule.onNodeWithContentDescription(BACK).performClick()

        assertEquals(1, backPressed)
    }

    @Test
    fun `should show every active item followed by the ghost row`() {
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        composeRule.onNodeWithText("Apples").assertIsDisplayed()
        composeRule.onNodeWithText("Bread").assertIsDisplayed()
        composeRule.onNodeWithText(GHOST_HINT).assertIsDisplayed()
    }

    @Test
    fun `should show the divider with a count when both sections have items`() {
        render(
            content(
                active = listOf(item("1", "Apples")),
                completed = listOf(completed("2", "Milk"), completed("3", "Eggs"))
            )
        )

        composeRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun `should not show the divider when every item is active`() {
        render(content(active = listOf(item("1", "Apples"))))

        assertEquals(listOf("Apples", GHOST_HINT), texts())
    }

    @Test
    fun `should not show the divider when every item is completed`() {
        render(content(completed = listOf(completed("2", "Milk"))))

        assertEquals(listOf(GHOST_HINT, "Milk"), texts())
    }

    @Test
    fun `should place completed items after the ghost row`() {
        render(
            content(
                active = listOf(item("1", "Apples")),
                completed = listOf(completed("2", "Milk"))
            )
        )

        assertEquals(listOf("Apples", GHOST_HINT, "1", "Milk"), texts())
    }

    @Test
    fun `should write a completed title in solid pale ink and never a font strike`() {
        render(content(completed = listOf(completed("2", "Milk"))))

        val style = textStyleOf("Milk")

        assertNull(style?.textDecoration)
        assertEquals(PaperPalette.light.inkDone, style?.color)
        assertEquals(1f, style?.color?.alpha ?: 0f, 0.01f)
    }

    @Test
    fun `should write an active title in full ink`() {
        render(content(active = listOf(item("1", "Apples"))))

        val style = textStyleOf("Apples")

        assertNull(style?.textDecoration)
        assertEquals(PaperPalette.light.ink, style?.color)
    }

    @Test
    fun `should carry nothing on an active row but its ring`() {
        render(content(active = listOf(item("1", "Apples"))))

        assertEquals(listOf(MARK_COMPLETED, BACK), descriptions())
    }

    @Test
    fun `should offer the same ring on a completed row to undo it`() {
        render(content(completed = listOf(completed("2", "Milk"))))

        composeRule.onNodeWithContentDescription(MARK_INCOMPLETE).assertIsDisplayed()
    }

    @Test
    fun `should toggle an item only once its ink has been drawn`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(MARK_COMPLETED).performClick()

        assertEquals(emptyList<String>(), toggled)

        inkSettles()

        assertEquals(listOf("1"), toggled)
    }

    @Test
    fun `should reverse a toggle that is tapped again before the ink lands`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(MARK_COMPLETED).performClick()
        composeRule.mainClock.advanceTimeBy(INK_TICK_MILLIS / 2)
        composeRule.onNodeWithContentDescription(MARK_INCOMPLETE).performClick()
        inkSettles()

        assertEquals(emptyList<String>(), toggled)
        assertNull(screenState.pendingToggle)
    }

    @Test
    fun `should buzz when the tick finishes drawing and not before`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(MARK_COMPLETED).performClick()

        assertEquals(NO_FEEDBACK, lastFeedback())

        tickLands()

        assertEquals(HapticFeedbackConstants.TOGGLE_ON, lastFeedback())
    }

    @Test
    fun `should buzz when a completed item is restored`() {
        render(content(completed = listOf(completed("2", "Milk"))))

        composeRule.onNodeWithContentDescription(MARK_INCOMPLETE).performClick()
        tickLands()

        assertEquals(HapticFeedbackConstants.TOGGLE_OFF, lastFeedback())
    }

    @Test
    fun `should still buzz the tick when the page is not allowed to animate`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(MARK_COMPLETED).performClick()

        assertEquals(HapticFeedbackConstants.TOGGLE_ON, lastFeedback())
    }

    @Test
    fun `should still buzz the restore when the page is not allowed to animate`() {
        screenState.animationsEnabled = false
        render(content(completed = listOf(completed("2", "Milk"))))

        composeRule.onNodeWithContentDescription(MARK_INCOMPLETE).performClick()

        assertEquals(HapticFeedbackConstants.TOGGLE_OFF, lastFeedback())
    }

    @Test
    fun `should stay silent while no item is toggled`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithText("Apples").performClick()

        assertEquals(NO_FEEDBACK, lastFeedback())
    }

    @Test
    fun `should replace the title with a prefilled field when the words are tapped`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithText("Apples").performClick()

        editField().assertTextEquals("Apples")
    }

    @Test
    fun `should commit a new title when the edit field is submitted`() {
        render(content(active = listOf(item("1", "Apples"))))
        composeRule.onNodeWithText("Apples").performClick()

        editField().performTextReplacement("Pears")
        editField().performImeAction()

        assertEquals(listOf("1" to "Pears"), edited)
    }

    @Test
    fun `should leave the title unchanged when the edit field is submitted blank`() {
        render(content(active = listOf(item("1", "Apples"))))
        composeRule.onNodeWithText("Apples").performClick()

        editField().performTextReplacement("   ")
        editField().performImeAction()

        assertEquals(emptyList<Pair<String, String>>(), edited)
    }

    @Test
    fun `should return to the title view after an edit is submitted`() {
        render(content(active = listOf(item("1", "Apples"))))
        composeRule.onNodeWithText("Apples").performClick()

        editField().performImeAction()
        composeRule.waitForIdle()

        assertNull(screenState.editingItemId)
    }

    @Test
    fun `should take the pen when the add line is tapped`() {
        render(TodoListState.Empty)

        composeRule.onNodeWithText(GHOST_HINT).performClick()
        composeRule.waitForIdle()

        assertTrue(screenState.addRowExpanded)
        addLine().assertIsFocused()
    }

    @Test
    fun `should carry no submit affordance on the add line`() {
        render(TodoListState.Empty)

        assertEquals(listOf(BACK), descriptions())
    }

    @Test
    fun `should clear the field after a title is submitted`() {
        screenState.addRowExpanded = true
        render(TodoListState.Empty)

        addLine().performTextReplacement("Bread")
        addLine().performImeAction()
        composeRule.waitForIdle()

        assertEquals("", screenState.addRowText)
    }

    @Test
    fun `should leave a fresh caret waiting on the add line after a title is committed`() {
        screenState.addRowExpanded = true
        render(TodoListState.Empty)

        addLine().performTextReplacement("Bread")
        addLine().performImeAction()
        composeRule.waitForIdle()

        addLine().assertIsFocused()
        assertTrue(screenState.addRowExpanded)
    }

    @Test
    fun `should not submit a blank title when the field takes its IME action`() {
        screenState.addRowExpanded = true
        render(TodoListState.Empty)

        addLine().performTextReplacement("   ")
        addLine().performImeAction()

        assertEquals(emptyList<String>(), submitted)
    }

    @Test
    fun `should submit the typed title when the field takes its IME action`() {
        screenState.addRowExpanded = true
        render(TodoListState.Empty)

        addLine().performTextReplacement("Bread")
        addLine().performImeAction()

        assertEquals(listOf("Bread"), submitted)
    }

    @Test
    fun `should render the preview order instead of the repository order while a drag is staged`() {
        screenState.previewOrder = listOf("2", "1")
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        assertEquals(listOf("Bread", "Apples", GHOST_HINT), texts())
    }

    // ── Tearing a row off ─────────────────────────────────────────────────────

    @Test
    fun `should take an item off the page without writing the delete through yet`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(item("1", "Apples"))))

        tearOffApples()

        assertTrue(deleted.isEmpty())
        composeRule.onNodeWithText("Apples").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(UNDO).assertIsDisplayed()
    }

    @Test
    fun `should write the item delete through once the undo slip has settled away`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(item("1", "Apples"))))

        tearOffApples()
        slipSettles()

        assertEquals(listOf("1"), deleted)
    }

    @Test
    fun `should put the item back when the undo slip is tapped`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(item("1", "Apples"))))

        tearOffApples()
        composeRule.onNodeWithContentDescription(UNDO).performClick()
        slipSettles()

        assertTrue(deleted.isEmpty())
        composeRule.onNodeWithText("Apples").assertIsDisplayed()
    }

    @Test
    fun `should complete an item when the row is swiped the other way`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithText("Apples").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        inkSettles()

        assertEquals(listOf("1"), toggled)
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun `should offer no undo at all while nothing has been torn off`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(UNDO).assertDoesNotExist()
    }

    // ── Reaching the gestures without one ─────────────────────────────────────

    @Test
    fun `should name every verb an active row answers to`() {
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        assertEquals(
            listOf(MARK_COMPLETED, EDIT, DELETE, MOVE_DOWN),
            verbsOf("Apples").map { it.label }
        )
    }

    @Test
    fun `should offer a completed row the verb that puts it back`() {
        render(content(completed = listOf(completed("1", "Apples"))))

        assertEquals(listOf(MARK_INCOMPLETE, EDIT, DELETE), verbsOf("Apples").map { it.label })
    }

    @Test
    fun `should offer no way to move a row past either end of the page`() {
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        assertEquals(listOf(MOVE_DOWN), moveVerbsOf("Apples"))
        assertEquals(listOf(MOVE_UP), moveVerbsOf("Bread"))
    }

    @Test
    fun `should complete an item asked to complete without a gesture`() {
        render(content(active = listOf(item("1", "Apples"))))

        perform("Apples", MARK_COMPLETED)
        inkSettles()

        assertEquals(listOf("1"), toggled)
    }

    @Test
    fun `should open the editor on an item asked to be edited without a gesture`() {
        render(content(active = listOf(item("1", "Apples"))))

        perform("Apples", EDIT)

        assertEquals("1", screenState.editingItemId)
    }

    @Test
    fun `should tear an item off when it is asked to be deleted without a gesture`() {
        screenState.animationsEnabled = false
        render(content(active = listOf(item("1", "Apples"))))

        perform("Apples", DELETE)

        composeRule.onNodeWithText("Apples").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(UNDO).assertIsDisplayed()
    }

    @Test
    fun `should reorder an item asked to move without a gesture`() {
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        perform("Bread", MOVE_UP)

        assertEquals(listOf(listOf("2", "1")), reordered)
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

    private fun tearOffApples() {
        composeRule.onNodeWithText("Apples").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    private fun slipSettles() {
        composeRule.mainClock.advanceTimeBy(UNDO_SLIP_MILLIS + SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    private fun editField(): SemanticsNodeInteraction =
        composeRule.onNode(hasSetTextAction() and isFocused())

    private fun addLine(): SemanticsNodeInteraction =
        composeRule.onAllNodes(hasSetTextAction()).onLast()

    private fun render(state: TodoListState, listName: String = "") {
        render(
            state,
            listName.takeIf { it.isNotEmpty() }
                ?.let { TodoListSummary(TodoList(LIST_ID, it), allDone = false) }
        )
    }

    private fun render(state: TodoListState, summary: TodoListSummary?) {
        composeRule.setContent { PaperTheme { Screen(state, summary) } }
    }

    @Composable
    private fun Screen(state: TodoListState, summary: TodoListSummary?) {
        hostView = LocalView.current
        TodoListScreen(
            summary = summary,
            today = TODAY,
            state = state,
            screenState = screenState,
            onBack = { backPressed += 1 },
            onToggle = { toggled += it },
            onEdit = { id, title -> edited += id to title },
            onDelete = { deleted += it },
            onSubmitInline = { submitted += it },
            onReorder = { orderedIds -> reordered += orderedIds },
            onRenameList = { renamed += it },
            onWriteDate = { datesWritten += it }
        )
    }

    private fun locale(): Locale = Locale.getDefault(Locale.Category.FORMAT)

    private fun lastFeedback(): Int {
        var view: View? = hostView
        while (view != null) {
            val buzz = shadowOf(view).lastHapticFeedbackPerformed()
            if (buzz != NO_FEEDBACK) return buzz
            view = view.parent as? View
        }
        return NO_FEEDBACK
    }

    private fun tickLands() {
        composeRule.mainClock.advanceTimeBy(INK_TICK_MILLIS + SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    private fun inkSettles() {
        composeRule.mainClock.advanceTimeBy(INK_TICK_MILLIS + INK_STRIKE_MILLIS + SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    private fun texts(): List<String> =
        collectText(composeRule.onRoot().fetchSemanticsNode())

    private fun collectText(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            .map { it.text }
            .filter { it.isNotBlank() } + node.children.flatMap { collectText(it) }

    private fun descriptions(): List<String> =
        collectDescriptions(composeRule.onRoot().fetchSemanticsNode())

    private fun collectDescriptions(
        node: SemanticsNode
    ): List<String> =
        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            node.children.flatMap { collectDescriptions(it) }

    private fun textStyleOf(text: String): TextStyle? {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        return results.firstOrNull()?.layoutInput?.style
    }

    private fun content(
        active: List<TodoItem> = emptyList(),
        completed: List<TodoItem> = emptyList()
    ): TodoListState =
        if (active.isEmpty() && completed.isEmpty()) {
            TodoListState.Empty
        } else {
            TodoListState.Content(active, completed)
        }

    private fun item(id: String, title: String) = TodoItem(id, title, LIST_ID)

    private fun completed(id: String, title: String) =
        TodoItem(id, title, LIST_ID, isCompleted = true, completedAt = 1000L)

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 1, 1)
        const val LIST_ID = "list-1"
        const val GHOST_HINT = "…"
        const val BACK = "Navigate up"
        const val MARK_COMPLETED = "Mark item as completed"
        const val MARK_INCOMPLETE = "Mark item as incomplete"
        const val EDIT = "Edit item"
        const val DELETE = "Delete item"
        const val MOVE_UP = "Move up"
        const val MOVE_DOWN = "Move down"
        const val UNDO = "Undo delete"
        const val NO_FEEDBACK = -1
        const val SETTLE_MILLIS = 100L
    }
}
