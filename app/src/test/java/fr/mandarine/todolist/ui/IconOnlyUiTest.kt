package fr.mandarine.todolist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.ConfirmDeleteRequest
import fr.mandarine.todolist.ui.paper.PaperTheme
import fr.mandarine.todolist.ui.paper.SectionSkip
import fr.mandarine.todolist.ui.todolist.TodoListScreen
import fr.mandarine.todolist.ui.todolist.TodoListScreenState
import fr.mandarine.todolist.ui.todolists.TodoListsScreen
import fr.mandarine.todolist.ui.todolists.TodoListsScreenState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The guard on what the app says out loud, not a ban on its saying anything. The
 * page is wordless wherever a glyph does the job and carries words where one does
 * not — so what this pins is which words each screen draws, exactly. A word nobody
 * decided on fails here; a word added on purpose comes with an assertion to update,
 * which is the point. See the design principle in docs/SPEC.md.
 *
 * Update these lists when words are added deliberately. Do not loosen them into
 * assertions that would pass for any string at all — that throws away the only
 * thing keeping the page from filling up with labels nobody chose.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconOnlyUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should show only numeric count in the section skip without the word Completed`() {
        composeRule.setContent { PaperTheme { SectionSkip(completedCount = 2, spoken = "2 items done") } }

        assertEquals(listOf("2"), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should show count of one in the section skip when exactly one completed item exists`() {
        composeRule.setContent { PaperTheme { SectionSkip(completedCount = 1, spoken = "1 item done") } }

        assertEquals(listOf("1"), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should not contain the word Completed in the section skip text`() {
        composeRule.setContent { PaperTheme { SectionSkip(completedCount = 12, spoken = "12 items done") } }

        val labels = composeRule.onRoot().fetchSemanticsNode().staticText().map { it.lowercase() }

        assert(labels.none { it.contains("completed") }) {
            "Expected the section skip to not contain 'completed' but was: $labels"
        }
    }

    /**
     * The add line on the items page is now marked: at rest it draws the plus mark
     * and the "Add an item" label in margin ink. The "…" ghost hint only appears
     * once the keyboard is up. The drawn label is the accessibility label for the
     * field, so the field's own contentDescription is suppressed to avoid repeating
     * the same words to a screen reader.
     */
    @Test
    fun `should draw the add label on the empty items page at rest`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        assertEquals(listOf(ADD_ITEM_LABEL), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should expose only the back affordance and the add line in the empty state of todo list`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        assertEquals(
            listOf(BACK_DESCRIPTION, ADD_ITEM_LABEL),
            composeRule.onRoot().fetchSemanticsNode().contentDescriptions()
        )
    }

    @Test
    fun `should show the add label as the only drawn text when the list has no items`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        composeRule.onNodeWithText(ADD_ITEM_LABEL).assertIsDisplayed()
    }

    /**
     * The masthead is the one word the empty page draws, and it is deliberate
     * ([#43]): a pad with nothing written at the top of it reads as unfinished
     * rather than calm. Anything else appearing here has to justify itself in this
     * list first.
     */
    @Test
    fun `should draw nothing on the empty lists page but its own name`() {
        composeRule.setContent { PaperTheme { EmptyListsScreen() } }

        assertEquals(listOf(APP_NAME), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    /**
     * A decorative image draws no semantics node at all, so the View-era check for
     * an undecorated `ImageView` has no Compose equivalent. Pinning the empty
     * screen to exactly the affordances it is allowed to have is what replaces it:
     * anything added to the empty state has to justify itself here first.
     */
    @Test
    fun `should expose only the create affordance in the empty state of lists screen`() {
        composeRule.setContent { PaperTheme { EmptyListsScreen() } }

        assertEquals(
            setOf(CREATE_LIST_DESCRIPTION),
            composeRule.onRoot().fetchSemanticsNode().contentDescriptions().toSet()
        )
    }

    /**
     * The gesture map is spoken to a screen reader as a list of named verbs. None
     * of those names may leak onto the page as a label, which is what would happen
     * if a verb were ever wired to a `Text` instead of to `customActions`.
     */
    @Test
    fun `should keep the spoken verbs of a row out of the drawn page`() {
        composeRule.setContent { PaperTheme { OneItemScreen() } }

        assertEquals(
            listOf(LIST_NAME, ITEM_TITLE, ADD_ITEM_LABEL),
            composeRule.onRoot().fetchSemanticsNode().staticText()
        )
    }

    /**
     * The resting lists page draws only its name. The delete prompt must not appear
     * without being raised: the buttons and the question line must be absent.
     */
    @Test
    fun `should draw no delete prompt on the resting lists page`() {
        composeRule.setContent { PaperTheme { EmptyListsScreen() } }

        val descriptions = composeRule.onRoot().fetchSemanticsNode().contentDescriptions()

        assert(DELETE_LABEL !in descriptions) { "Delete button must not appear at rest" }
        assert(CANCEL_LABEL !in descriptions) { "Cancel button must not appear at rest" }
    }

    /**
     * The resting items page draws only the ghost hint. The delete prompt must not
     * appear without being raised.
     */
    @Test
    fun `should draw no delete prompt on the resting items page`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        val descriptions = composeRule.onRoot().fetchSemanticsNode().contentDescriptions()

        assert(DELETE_LABEL !in descriptions) { "Delete button must not appear at rest" }
        assert(CANCEL_LABEL !in descriptions) { "Cancel button must not appear at rest" }
    }

    /**
     * When the delete prompt is raised on the lists page, it draws the exact
     * question line and the two button labels — no more, no less.
     */
    @Test
    fun `should draw the delete question and both button labels when the lists prompt is raised`() {
        composeRule.setContent { PaperTheme { ListsScreenWithDeletePrompt() } }

        composeRule.onNodeWithText(DELETE_QUESTION).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(DELETE_LABEL).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(CANCEL_LABEL).assertIsDisplayed()
    }

    /**
     * When the delete prompt is raised on the items page, it draws the question line
     * with no cascade line (items carry no cascade), and both button labels.
     */
    @Test
    fun `should draw the delete question and both button labels when the items prompt is raised`() {
        composeRule.setContent { PaperTheme { ItemsScreenWithDeletePrompt() } }

        composeRule.onNodeWithText(DELETE_QUESTION).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(DELETE_LABEL).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(CANCEL_LABEL).assertIsDisplayed()
    }

    /**
     * The cascade line — naming how many items go with the list — appears only when
     * the list has at least one item, and must not appear for an empty list.
     */
    @Test
    fun `should draw the cascade count in the prompt only when the list has items`() {
        lateinit var stateWithItems: TodoListsScreenState
        lateinit var stateNoItems: TodoListsScreenState
        composeRule.setContent {
            PaperTheme {
                stateWithItems = remember {
                    TodoListsScreenState().also {
                        it.confirmDelete = ConfirmDeleteRequest("1", "Groceries", 5)
                    }
                }
                ListsScreenWithDeletePromptState(stateWithItems)
            }
        }
        composeRule.onNodeWithText("and the 5 items on it").assertIsDisplayed()
    }

    private companion object {
        const val LIST_NAME = "Groceries"
        const val ITEM_TITLE = "Apples"
        /**
         * The add line on the items page now draws its label in ink at rest, so the
         * page's drawn text includes these words — they are the mark that makes the
         * affordance visible on a populated page. This replaces the "…" ghost hint
         * which only appears once the field is focused.
         */
        const val ADD_ITEM_LABEL = "Add an item"
        const val BACK_DESCRIPTION = "Navigate up"
        const val CREATE_LIST_DESCRIPTION = "Create new list"
        const val APP_NAME = "To do list"
        const val DELETE_LABEL = "Delete"
        const val CANCEL_LABEL = "Cancel"
        const val DELETE_QUESTION = "Delete \"Groceries\"?"
    }
}

private val TODAY: LocalDate = LocalDate.of(2026, 1, 1)

@Composable
private fun EmptyItemsScreen() {
    TodoListScreen(
        summary = null,
        today = TODAY,
        state = TodoListState.Empty,
        screenState = remember { TodoListScreenState() },
        onBack = {},
        onToggle = {},
        onEdit = { _, _ -> },
        onDelete = {},
        onSubmitInline = {},
        onReorder = {}
    )
}

@Composable
private fun OneItemScreen() {
    TodoListScreen(
        summary = TodoListSummary(TodoList("list-1", "Groceries"), allDone = false),
        today = TODAY,
        state = TodoListState.Content(listOf(TodoItem("1", "Apples", "list-1")), emptyList()),
        screenState = remember { TodoListScreenState() },
        onBack = {},
        onToggle = {},
        onEdit = { _, _ -> },
        onDelete = {},
        onSubmitInline = {},
        onReorder = {}
    )
}

@Composable
private fun EmptyListsScreen() {
    TodoListsScreen(
        state = TodoListsState.Empty,
        screenState = remember { TodoListsScreenState() },
        today = LocalDate.of(2026, 1, 1),
        onOpenList = {},
        onCreateList = { _, _, _ -> },
        onRenameList = { _, _, _, _ -> },
        onDeleteList = {},
        onReorder = {}
    )
}

@Composable
private fun ListsScreenWithDeletePrompt() {
    val state = remember {
        TodoListsScreenState().also {
            it.confirmDelete = ConfirmDeleteRequest("list-1", "Groceries", null)
        }
    }
    ListsScreenWithDeletePromptState(state)
}

@Composable
private fun ListsScreenWithDeletePromptState(screenState: TodoListsScreenState) {
    TodoListsScreen(
        state = TodoListsState.Content(
            listOf(TodoListSummary(TodoList("list-1", "Groceries"), allDone = false)),
            emptyList()
        ),
        screenState = screenState,
        today = TODAY,
        onOpenList = {},
        onCreateList = { _, _, _ -> },
        onRenameList = { _, _, _, _ -> },
        onDeleteList = {},
        onReorder = {}
    )
}

@Composable
private fun ItemsScreenWithDeletePrompt() {
    val state = remember {
        TodoListScreenState().also {
            it.confirmDelete = ConfirmDeleteRequest("item-1", "Groceries", null)
        }
    }
    TodoListScreen(
        summary = TodoListSummary(TodoList("list-1", "Groceries"), allDone = false),
        today = TODAY,
        state = TodoListState.Content(
            listOf(TodoItem("item-1", "Groceries", "list-1")),
            emptyList()
        ),
        screenState = state,
        onBack = {},
        onToggle = {},
        onEdit = { _, _ -> },
        onDelete = {},
        onSubmitInline = {},
        onReorder = {}
    )
}

internal fun SemanticsNode.staticText(): List<String> =
    config.getOrNull(SemanticsProperties.Text).orEmpty()
        .map { it.text }
        .filter { it.isNotBlank() } + children.flatMap { it.staticText() }

internal fun SemanticsNode.contentDescriptions(): List<String> =
    config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        .filter { it.isNotBlank() } + children.flatMap { it.contentDescriptions() }
