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
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.domain.TutorialStep
import fr.mandarine.todolist.presentation.TutorialLine
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.ui.tutorial.narrationStringRes
import org.junit.Assert.assertNotEquals
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

    @Test
    fun `should not contain any static text in the empty state of todo list`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        assertEquals(listOf(GHOST_HINT), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    /**
     * The rule forbids words drawn on the paper, not words spoken about it. The
     * line every row is written on is named for a screen reader — it draws
     * nothing but a rule and a ghost ellipsis, so without a name it is the one
     * affordance on an empty page that cannot be found at all.
     */
    fun `should expose only the back affordance and the add line in the empty state of todo list`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        assertEquals(
            listOf(ADD_ITEM_DESCRIPTION, BACK_DESCRIPTION),
            composeRule.onRoot().fetchSemanticsNode().contentDescriptions()
        )
    }

    @Test
    fun `should show the ghost row as the only row when the list has no items`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        composeRule.onNodeWithText(GHOST_HINT).assertIsDisplayed()
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
        const val ADD_ITEM_DESCRIPTION = "Add an item"
        const val BACK_DESCRIPTION = "Navigate up"
        const val REPLAY_DESCRIPTION = "Replay tutorial"
        const val CREATE_LIST_DESCRIPTION = "Create new list"
        const val APP_NAME = "To do list"
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

/**
 * The tour is the one part of the app that speaks in sentences, and these are the
 * sentences. It is pinned here for the same reason every other word is: so that
 * adding one, or quietly rewording one, is something someone decided to do.
 *
 * Six lines, one per scene. If a scene is added or dropped, this list changes with
 * it — do not replace it with a check that every line merely resolves to something.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialWordsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `should say exactly these six things and nothing else during the tour`() {
        val said = TutorialLine.entries.map { context.getString(narrationStringRes(it)) }

        assertEquals(
            listOf(
                "Take a sheet and write a list on it",
                "Circle a day and you get a reminder that morning",
                "Tap a list to open it",
                "Write what is on it, line by line",
                "Tap to tick something off. Hold a row to move it.",
                "Pull a row right to edit it, left to tear it off"
            ),
            said
        )
    }

    @Test
    fun `should give every scene of the tour a line of its own`() {
        val lines = TutorialLine.entries.map { narrationStringRes(it) }

        assertEquals(lines.size, lines.toSet().size)
        assertEquals(TutorialStep.entries.size + 1, lines.size)
    }

    @Test
    fun `should translate every line of the tour into French`() {
        val french = context.createConfigurationContext(
            android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(java.util.Locale.FRENCH)
            }
        )

        for (line in TutorialLine.entries) {
            val english = context.getString(narrationStringRes(line))
            val translated = french.getString(narrationStringRes(line))
            assertNotEquals("$line was left in English", english, translated)
        }
    }
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
