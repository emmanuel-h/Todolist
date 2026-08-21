package fr.mandarine.todolist.ui.todolist

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.ui.paper.PaperTheme
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
class TodoListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val screenState = TodoListScreenState()
    private val toggled = mutableListOf<String>()
    private val deleted = mutableListOf<String>()
    private val edited = mutableListOf<Pair<String, String>>()
    private val submitted = mutableListOf<String>()
    private val reordered = mutableListOf<Pair<Int, Int>>()
    private var backPressed = 0

    @Test
    fun `should show the list name in the top bar`() {
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
    fun `should strike through and fade the title of a completed item`() {
        render(content(completed = listOf(completed("2", "Milk"))))

        val style = textStyleOf("Milk")

        assertEquals(TextDecoration.LineThrough, style?.textDecoration)
        assertEquals(0.5f, style?.color?.alpha ?: 1f, 0.01f)
    }

    @Test
    fun `should not strike through the title of an active item`() {
        render(content(active = listOf(item("1", "Apples"))))

        val style = textStyleOf("Apples")

        assertNull(style?.textDecoration)
        assertEquals(1f, style?.color?.alpha ?: 0f, 0.01f)
    }

    @Test
    fun `should offer a drag handle on an active row`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(DRAG_HANDLE).assertIsDisplayed()
    }

    @Test
    fun `should not offer a drag handle on a completed row`() {
        render(content(completed = listOf(completed("2", "Milk"))))

        assertTrue(descriptions().none { it == DRAG_HANDLE })
    }

    @Test
    fun `should offer an undo affordance on a completed row`() {
        render(content(completed = listOf(completed("2", "Milk"))))

        composeRule.onNodeWithContentDescription(MARK_INCOMPLETE).assertIsDisplayed()
    }

    @Test
    fun `should toggle an item when its complete affordance is tapped`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(MARK_COMPLETED).performClick()

        assertEquals(listOf("1"), toggled)
    }

    @Test
    fun `should delete an item when its delete affordance is tapped`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(DELETE).performClick()

        assertEquals(listOf("1"), deleted)
    }

    @Test
    fun `should replace the title with a prefilled field when edit is tapped`() {
        render(content(active = listOf(item("1", "Apples"))))

        composeRule.onNodeWithContentDescription(EDIT).performClick()

        composeRule.onNode(hasSetTextAction()).assertTextEquals("Apples")
    }

    @Test
    fun `should commit a new title when the edit field is submitted`() {
        render(content(active = listOf(item("1", "Apples"))))
        composeRule.onNodeWithContentDescription(EDIT).performClick()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Pears")
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf("1" to "Pears"), edited)
    }

    @Test
    fun `should leave the title unchanged when the edit field is submitted blank`() {
        render(content(active = listOf(item("1", "Apples"))))
        composeRule.onNodeWithContentDescription(EDIT).performClick()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("   ")
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(emptyList<Pair<String, String>>(), edited)
    }

    @Test
    fun `should return to the title view after an edit is submitted`() {
        render(content(active = listOf(item("1", "Apples"))))
        composeRule.onNodeWithContentDescription(EDIT).performClick()

        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()

        assertNull(screenState.editingItemId)
    }

    @Test
    fun `should expand the add row when the ghost row is tapped`() {
        render(TodoListState.Empty)

        composeRule.onNodeWithText(GHOST_HINT).performClick()
        composeRule.waitForIdle()

        assertTrue(screenState.addRowExpanded)
        composeRule.onNodeWithContentDescription(SUBMIT).assertIsDisplayed()
    }

    @Test
    fun `should submit the typed title when the add affordance is tapped`() {
        screenState.addRowExpanded = true
        render(TodoListState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Bread")
        composeRule.onNodeWithContentDescription(SUBMIT).performClick()

        assertEquals(listOf("Bread"), submitted)
    }

    @Test
    fun `should clear the field after a title is submitted`() {
        screenState.addRowExpanded = true
        render(TodoListState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Bread")
        composeRule.onNodeWithContentDescription(SUBMIT).performClick()
        composeRule.waitForIdle()

        assertEquals("", screenState.addRowText)
    }

    @Test
    fun `should submit the typed title when the field takes its IME action`() {
        screenState.addRowExpanded = true
        render(TodoListState.Empty)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Bread")
        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf("Bread"), submitted)
    }

    @Test
    fun `should reorder an item when its handle is dragged past a neighbour`() {
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        composeRule.onAllNodesWithContentDescription(DRAG_HANDLE)[0].performTouchInput {
            down(center)
            moveBy(Offset(0f, 400f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(listOf(0 to 1), reordered)
    }

    @Test
    fun `should show the dragged row in its new place before the drag is released`() {
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        composeRule.onAllNodesWithContentDescription(DRAG_HANDLE)[0].performTouchInput {
            down(center)
            moveBy(Offset(0f, 400f))
        }
        composeRule.mainClock.advanceTimeBy(500L)

        assertEquals(listOf("2", "1"), screenState.previewOrder)
    }

    @Test
    fun `should drop the staged order when a drag ends where it started`() {
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        composeRule.onAllNodesWithContentDescription(DRAG_HANDLE)[0].performTouchInput {
            down(center)
            moveBy(Offset(0f, 4f))
            up()
        }
        composeRule.waitForIdle()

        assertNull(screenState.previewOrder)
        assertEquals(emptyList<Pair<Int, Int>>(), reordered)
    }

    @Test
    fun `should render the preview order instead of the repository order while a drag is staged`() {
        screenState.previewOrder = listOf("2", "1")
        render(content(active = listOf(item("1", "Apples"), item("2", "Bread"))))

        assertEquals(listOf("Bread", "Apples", GHOST_HINT), texts())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun render(state: TodoListState, listName: String = "") {
        composeRule.setContent { PaperTheme { Screen(state, listName) } }
    }

    @Composable
    private fun Screen(state: TodoListState, listName: String) {
        TodoListScreen(
            listName = listName,
            state = state,
            screenState = screenState,
            onBack = { backPressed += 1 },
            onToggle = { toggled += it },
            onEdit = { id, title -> edited += id to title },
            onDelete = { deleted += it },
            onSubmitInline = { submitted += it },
            onReorder = { from, to -> reordered += from to to }
        )
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
        const val LIST_ID = "list-1"
        const val GHOST_HINT = "…"
        const val BACK = "Navigate up"
        const val DRAG_HANDLE = "Drag to reorder"
        const val MARK_COMPLETED = "Mark item as completed"
        const val MARK_INCOMPLETE = "Mark item as incomplete"
        const val EDIT = "Edit item"
        const val DELETE = "Delete item"
        const val SUBMIT = "Submit new item"
    }
}
