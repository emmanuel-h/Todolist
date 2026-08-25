package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.todolist.TodoRow
import fr.mandarine.todolist.ui.todolists.TodoListRow
import fr.mandarine.todolist.ui.todolists.dueTone
import fr.mandarine.todolist.ui.todolists.targetTone
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
class InkBudgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val palette = PaperPalette.light

    @Test
    fun `should give every tone an ink of its own`() {
        val inks = InkTone.entries.map { palette.inked(it) }

        assertEquals(InkTone.entries.size, inks.toSet().size)
        inks.forEach { assertEquals(FULLY_INKED, it.alpha, TOLERANCE) }
    }

    @Test
    fun `should recognise the tone of every ink it spends`() {
        InkTone.entries.forEach { tone ->
            assertEquals(tone, InkBudget.toneOf(palette.inked(tone), palette))
        }
    }

    @Test
    fun `should recognise no tone in a colour the budget never spends`() {
        assertNull(InkBudget.toneOf(palette.paper, palette))
        assertNull(InkBudget.toneOf(Color.Magenta, palette))
    }

    @Test
    fun `should rest on every tone that is neither blue nor red`() {
        val resting = InkTone.entries.filter { InkBudget.restsOn(it) }

        assertEquals(
            listOf(InkTone.Words, InkTone.Crossed, InkTone.Margin, InkTone.Today),
            resting
        )
        assertTrue(resting.none { palette.inked(it) == palette.inkBlue })
        assertTrue(resting.none { palette.inked(it) == palette.inkRed })
    }

    @Test
    fun `should spend blue on nothing but the mark being made`() {
        assertEquals(InkTone.Acted, InkBudget.ring(wet = true))
        assertEquals(palette.inkBlue, palette.inked(InkTone.Acted))
        assertTrue(!InkBudget.restsOn(InkTone.Acted))
    }

    @Test
    fun `should spend red on nothing but a day already missed`() {
        assertEquals(InkTone.Lost, dueTone(DueDateStatus.OVERDUE))
        assertEquals(palette.inkRed, palette.inked(InkTone.Lost))
        assertTrue(!InkBudget.restsOn(InkTone.Lost))
    }

    @Test
    fun `should write words in ink until they are finished and then in spent ink`() {
        assertEquals(InkTone.Words, InkBudget.words(finished = false))
        assertEquals(InkTone.Crossed, InkBudget.words(finished = true))
    }

    @Test
    fun `should draw a resting ring in pencil`() {
        assertEquals(InkTone.Margin, InkBudget.ring(wet = false))
    }

    @Test
    fun `should keep every jot a row can carry at rest within the budget`() {
        val jots = listOf(
            targetTone(elapsed = false),
            targetTone(elapsed = true),
            dueTone(DueDateStatus.FUTURE),
            dueTone(DueDateStatus.TODAY)
        )

        jots.forEach { assertTrue("$it is not a resting ink", InkBudget.restsOn(it)) }
    }

    @Test
    fun `should write a resting list row in nothing but ink and pencil`() {
        composeRule.setContent {
            PaperTheme {
                Column {
                    RestingListRow(dueStatus = null)
                    RestingListRow(dueStatus = DueDateStatus.FUTURE)
                    RestingListRow(dueStatus = DueDateStatus.TODAY)
                }
            }
        }

        val spent = drawnInks().map { InkBudget.toneOf(it, palette) }

        assertTrue(spent.contains(InkTone.Words))
        assertTrue(spent.contains(InkTone.Margin))
        spent.forEach { tone ->
            assertTrue("$tone was drawn on a resting row", tone != null && InkBudget.restsOn(tone))
        }
    }

    @Test
    fun `should write a resting item row in nothing but ink and pencil`() {
        composeRule.setContent { PaperTheme { RestingItemRow() } }

        val spent = drawnInks().map { InkBudget.toneOf(it, palette) }

        assertTrue(spent.contains(InkTone.Words))
        spent.forEach { tone ->
            assertTrue("$tone was drawn on a resting row", tone != null && InkBudget.restsOn(tone))
        }
    }

    @Test
    fun `should spend the page's only red on the day a resting row has missed`() {
        composeRule.setContent {
            PaperTheme { RestingListRow(dueStatus = DueDateStatus.OVERDUE) }
        }

        val spent = drawnInks().map { InkBudget.toneOf(it, palette) }

        assertEquals(ONE_MARK, spent.count { it == InkTone.Lost })
        assertTrue(spent.none { it == InkTone.Acted })
    }

    @Composable
    private fun RestingListRow(dueStatus: DueDateStatus?) {
        TodoListRow(
            summary = TodoListSummary(
                list = TodoList(
                    id = "list-1",
                    name = "Groceries",
                    targetDate = if (dueStatus == null) DATE else null,
                    dueDate = if (dueStatus == null) null else DATE
                ),
                allDone = false,
                activeCount = 3,
                completedCount = 1,
                isTargetDateElapsed = false,
                showTargetYear = false,
                dueDateStatus = dueStatus,
                showDueDateYear = false
            ),
            animated = false,
            onOpen = {},
            onDeleteRequested = {}
        )
    }

    @Composable
    private fun RestingItemRow() {
        TodoRow(
            item = TodoItem("item-1", "Apples", "list-1"),
            checked = false,
            editing = false,
            onToggle = {},
            onEditRequested = {},
            onEditCommitted = {},
            onEditDismissed = {},
            onDeleteRequested = {},
            animated = false
        )
    }

    private fun drawnInks(): List<Color> = inksOf(composeRule.onRoot().fetchSemanticsNode())

    private fun inksOf(node: SemanticsNode): List<Color> {
        val laid = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(laid)
        val own = laid.mapNotNull { result ->
            result.layoutInput.style.color.takeIf { it != Color.Unspecified }
        }
        return own + node.children.flatMap { inksOf(it) }
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 3, 14)
        const val FULLY_INKED = 1f
        const val TOLERANCE = 0.001f
        const val ONE_MARK = 1
    }
}
