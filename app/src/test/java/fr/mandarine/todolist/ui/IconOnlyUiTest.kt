package fr.mandarine.todolist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListsState
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconOnlyUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should show only numeric count in the section skip without the word Completed`() {
        composeRule.setContent { PaperTheme { SectionSkip(completedCount = 2) } }

        assertEquals(listOf("2"), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should show count of one in the section skip when exactly one completed item exists`() {
        composeRule.setContent { PaperTheme { SectionSkip(completedCount = 1) } }

        assertEquals(listOf("1"), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should not contain the word Completed in the section skip text`() {
        composeRule.setContent { PaperTheme { SectionSkip(completedCount = 12) } }

        val labels = composeRule.onRoot().fetchSemanticsNode().staticText().map { it.lowercase() }

        assert(labels.none { it.contains("completed") }) {
            "Expected the section skip to not contain 'completed' but was: $labels"
        }
    }

    @Test
    fun `should not contain any static text in the empty state of todo list`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        assertEquals(listOf(GHOST_HINT), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should expose only the back affordance in the empty state of todo list`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        assertEquals(
            listOf(BACK_DESCRIPTION),
            composeRule.onRoot().fetchSemanticsNode().contentDescriptions()
        )
    }

    @Test
    fun `should show the ghost row as the only row when the list has no items`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        composeRule.onNodeWithText(GHOST_HINT).assertIsDisplayed()
    }

    @Test
    fun `should not contain any static text in the empty state of lists screen`() {
        composeRule.setContent { PaperTheme { EmptyListsScreen() } }

        assertEquals(emptyList<String>(), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    /**
     * A decorative image draws no semantics node at all, so the View-era check for
     * an undecorated `ImageView` has no Compose equivalent. Pinning the empty
     * screen to exactly the two affordances it is allowed to have is what replaces
     * it: anything added to the empty state has to justify itself here first.
     */
    @Test
    fun `should expose only the create and replay affordances in the empty state of lists screen`() {
        composeRule.setContent { PaperTheme { EmptyListsScreen() } }

        assertEquals(
            setOf(REPLAY_DESCRIPTION, CREATE_LIST_DESCRIPTION),
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
            listOf(LIST_NAME, ITEM_TITLE, GHOST_HINT),
            composeRule.onRoot().fetchSemanticsNode().staticText()
        )
    }

    private companion object {
        const val GHOST_HINT = "…"
        const val LIST_NAME = "Groceries"
        const val ITEM_TITLE = "Apples"
        const val BACK_DESCRIPTION = "Navigate up"
        const val REPLAY_DESCRIPTION = "Replay tutorial"
        const val CREATE_LIST_DESCRIPTION = "Create new list"
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
        onReorder = {},
        onReplayTutorial = {}
    )
}

internal fun SemanticsNode.staticText(): List<String> =
    config.getOrNull(SemanticsProperties.Text).orEmpty()
        .map { it.text }
        .filter { it.isNotBlank() } + children.flatMap { it.staticText() }

internal fun SemanticsNode.contentDescriptions(): List<String> =
    config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        .filter { it.isNotBlank() } + children.flatMap { it.contentDescriptions() }
