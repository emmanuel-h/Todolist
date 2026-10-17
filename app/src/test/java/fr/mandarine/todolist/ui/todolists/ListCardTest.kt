package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.SemanticsActions
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.PaperPalette
import fr.mandarine.todolist.ui.paper.inked
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
class ListCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var opened = 0
    private var actionsRequested = 0
    private var torn = 0
    private val rewritten = mutableListOf<DateSelection>()

    @Test
    fun `should show the list name`() {
        render(summary(name = "Groceries"))

        composeRule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun `should open the list when the row is tapped`() {
        render(summary())

        composeRule.onNodeWithText("Groceries").performClick()

        assertEquals(1, opened)
    }

    // ── The jot in the margin ─────────────────────────────────────────────────

    @Test
    fun `should open the calendar on the day the jot carries rather than the list`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        composeRule.onNodeWithContentDescription("Due date ${spelled(DATE)}").performClick()

        assertEquals(listOf(DateSelection(DateKind.DUE, DATE)), rewritten)
        assertEquals(0, opened)
    }

    @Test
    fun `should hand the calendar the kind of date the jot was written as`() {
        render(summary(targetDate = DATE))

        composeRule.onNodeWithContentDescription("Target date ${spelled(DATE)}").performClick()

        assertEquals(listOf(DateSelection(DateKind.TARGET, DATE)), rewritten)
    }

    @Test
    fun `should still open the list when the row is tapped beside its jot`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        composeRule.onNodeWithText("Groceries", useUnmergedTree = true).performClick()

        assertEquals(1, opened)
        assertEquals(emptyList<DateSelection>(), rewritten)
    }

    @Test
    fun `should still hold out the actions when a row carrying a jot has its more mark pressed`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        composeRule.onNodeWithContentDescription(LIST_ACTIONS).performClick()
        composeRule.waitForIdle()

        assertEquals(1, actionsRequested)
        assertEquals(emptyList<DateSelection>(), rewritten)
    }

    @Test
    fun `should offer the jot as a press a screen reader can name and make`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        val jot = composeRule.onNodeWithContentDescription("Due date ${spelled(DATE)}")

        jot.assertHasClickAction()
        assertEquals("Set due date", clickLabelOf(jot))
        jot.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(DateSelection(DateKind.DUE, DATE)), rewritten)
    }

    @Test
    fun `should name the target jot's press after the date it writes`() {
        render(summary(targetDate = DATE))

        val jot = composeRule.onNodeWithContentDescription("Target date ${spelled(DATE)}")

        assertEquals("Set target date", clickLabelOf(jot))
    }

    @Test
    fun `should leave the jot inert on a page that has nowhere to write the date`() {
        render(summary(targetDate = DATE), rewritable = false)

        composeRule
            .onNodeWithContentDescription("Target date ${spelled(DATE)}")
            .assertHasNoClickAction()
    }

    /**
     * The tally is what the row says about itself, not something the row can be
     * asked to do: it is read out with the row and adds nothing to press. Only the
     * row itself, and the jot that carries a calendar, answer to a press.
     */
    @Test
    fun `should leave the tally a mark to be read and not one to be pressed`() {
        render(summary(activeCount = 3))

        assertEquals(listOf(OPENS_THE_LIST, LIST_ACTIONS), presses())
    }

    @Test
    fun `should add the jot to what a row can be pressed for and nothing else`() {
        render(summary(activeCount = 3, dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        assertEquals(listOf(OPENS_THE_LIST, LIST_ACTIONS, "Set due date"), presses())
    }

    @Test
    /**
     * A dated card adds the page torn off a calendar in its corner, read out as the
     * date it carries, and nothing else.
     */
    fun `should carry nothing at rest but the name, its status and its one control on a dated card`() {
        render(summary(activeCount = 2, targetDate = DATE))

        assertEquals(
            listOf("0 of 2 done", LIST_ACTIONS, "Target date ${spelled(DATE)}"),
            descriptions()
        )
    }

    /**
     * The marginalia are written beside the name, not instead of it: anything in
     * the margin that claims the row's whole width squeezes the name down to one
     * letter a line, which is what a tally that fills its box does.
     */
    @Test
    fun `should keep a short name to one line beside the day's page`() {
        render(summary(name = "Groceries", activeCount = 3, targetDate = DATE))

        assertEquals(ONE_LINE, layoutOf("Groceries")?.lineCount)
    }

    @Test
    fun `should write a finished name in solid pale ink and never a font strike`() {
        render(summary(allDone = true))

        val style = textStyleOf("Groceries")

        assertNull(style?.textDecoration)
        assertEquals(PaperPalette.light.inkDone, style?.color)
        assertEquals(1f, style!!.color.alpha, 0.01f)
    }

    @Test
    fun `should leave the name in full ink while the list still has work`() {
        render(summary(allDone = false))

        assertNull(textStyleOf("Groceries")?.textDecoration)
        assertEquals(PaperPalette.light.inked(InkTone.Words), textStyleOf("Groceries")?.color)
    }

    @Test
    fun `should ask for the actions and not open the list when its more mark is pressed`() {
        render(summary())

        composeRule.onNodeWithContentDescription(LIST_ACTIONS).performClick()
        composeRule.waitForIdle()

        assertEquals(1, actionsRequested)
        assertEquals(0, opened)
    }

    @Test
    fun `should leave the row in place when its more mark is pressed`() {
        render(summary())
        val resting = nameX()

        composeRule.onNodeWithContentDescription(LIST_ACTIONS).performClick()
        composeRule.waitForIdle()

        assertEquals(resting, nameX(), 0.5f)
    }

    @Test
    fun `should not show the more mark when the row offers no actions`() {
        render(summary(), editable = false)

        composeRule.onNodeWithContentDescription(LIST_ACTIONS).assertDoesNotExist()
        assertEquals(0, actionsRequested)
    }

    @Test
    fun `should report the tear as finished once the row has left the page`() {
        render(summary(), tearing = true)

        composeRule.waitForIdle()

        assertEquals(1, torn)
    }

    // ── The day ───────────────────────────────────────────────────────────────

    /**
     * A dateless row's route to a day is its sheet of actions, which says so in
     * words; the bare calendar glyph on every row is gone with the other controls.
     */
    @Test
    fun `should leave a dateless row's day to its sheet of actions`() {
        render(summary())

        composeRule.onNodeWithContentDescription(GIVE_A_DAY).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(LIST_ACTIONS).assertIsDisplayed()
    }

    @Test
    fun `should not draw the calendar button when the list has a target date`() {
        render(summary(targetDate = DATE))

        composeRule.onNodeWithContentDescription(GIVE_A_DAY).assertDoesNotExist()
    }

    @Test
    fun `should not draw the calendar button when the list has a due date`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        composeRule.onNodeWithContentDescription(GIVE_A_DAY).assertDoesNotExist()
    }

    @Test
    fun `should never draw both a calendar button and a date jot on the same row`() {
        render(summary(targetDate = DATE))

        val jotExists = composeRule.onNodeWithContentDescription("Target date ${spelled(DATE)}")
            .fetchSemanticsNode().let { true }
        val buttonExists = try {
            composeRule.onNodeWithContentDescription(GIVE_A_DAY).assertDoesNotExist()
            false
        } catch (_: AssertionError) {
            true
        }
        assert(!buttonExists) { "Calendar button must not appear alongside a date jot" }
    }

    @Test
    fun `should not draw the calendar button when the row has no rewrite surface`() {
        render(summary(), rewritable = false)

        composeRule.onNodeWithContentDescription(GIVE_A_DAY).assertDoesNotExist()
    }

    @Test
    fun `should print the day under the name`() {
        render(summary(targetDate = DATE))

        assertTrue(baselineOf("Groceries") < baselineOf(printed(DATE)))
    }

    @Test
    fun `should draw no calendar button when the list has a date`() {
        render(summary(targetDate = DATE))

        composeRule.onNodeWithContentDescription(GIVE_A_DAY).assertDoesNotExist()
    }

    @Test
    fun `should jot no date when the list has neither`() {
        render(summary())

        assertEquals(listOf("Groceries"), texts())
    }

    @Test
    fun `should print the target day on the card's status line`() {
        render(summary(targetDate = DATE))

        composeRule.onNodeWithText(printed(DATE), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `should append the year to the jot when the year is not obvious`() {
        render(summary(targetDate = DATE, showTargetYear = true))

        assertTrue("Expected the year in ${texts()}", texts().any { it.contains("2026") })
    }

    @Test
    fun `should read the jotted target date out in full`() {
        render(summary(targetDate = DATE))

        composeRule
            .onNodeWithContentDescription("Target date ${spelled(DATE)}")
            .assertIsDisplayed()
    }

    @Test
    fun `should print the due day and read it out in full when the list has one`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        composeRule.onNodeWithText(printed(DATE), useUnmergedTree = true).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Due date ${spelled(DATE)}")
            .assertIsDisplayed()
    }

    @Test
    fun `should jot no due date when the status has not been computed`() {
        render(summary(dueDate = DATE, dueDateStatus = null))

        assertEquals(listOf("Groceries"), texts())
    }

    @Test
    fun `should count the days a missed due day is late in red`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.OVERDUE, daysLate = 2))

        assertEquals(PaperPalette.light.inked(InkTone.Lost), textStyleOf("2 DAYS LATE")?.color)
    }

    @Test
    fun `should print a future due day in pencil`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        assertEquals(PaperPalette.light.inked(InkTone.Margin), textStyleOf(printed(DATE))?.color)
    }

    @Test
    fun `should cross out a target day that has gone by rather than redden it`() {
        render(summary(targetDate = DATE, isTargetDateElapsed = true))

        val style = textStyleOf(printed(DATE))
        assertEquals(PaperPalette.light.inked(InkTone.Crossed), style?.color)
        assertEquals(TextDecoration.LineThrough, style?.textDecoration)
    }

    @Test
    fun `should preview the first things still to do and count the rest`() {
        render(summary(activeCount = 5, preview = listOf("Milk", "Eggs", "Bread")))

        listOf("Milk", "Eggs", "Bread", "+ 2 more").forEach {
            composeRule.onNodeWithText(it, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `should preview nothing on a finished list`() {
        render(summary(allDone = true, completedCount = 2, preview = listOf("Milk")))

        composeRule.onNodeWithText("Milk", useUnmergedTree = true).assertDoesNotExist()
    }

    /**
     * Above scale 1.05 the platform compresses the larger `sp` sizes hardest, so
     * the 14sp marginalia used to write a taller line than the 20sp name it
     * annotates and pushed the row onto a second rule half a line below it.
     */

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun render(
        summary: TodoListSummary,
        tearing: Boolean = false,
        editable: Boolean = true,
        rewritable: Boolean = true
    ) {
        composeRule.setContent { PaperTheme { Row(summary, tearing, editable, rewritable) } }
    }

    private fun clickLabelOf(node: SemanticsNodeInteraction): String? =
        node.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label

    private fun verbs(): List<String> = collectVerbs(composeRule.onRoot().fetchSemanticsNode())

    private fun collectVerbs(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label } +
            node.children.flatMap { collectVerbs(it) }

    private fun presses(): List<String?> = collectPresses(composeRule.onRoot().fetchSemanticsNode())

    private fun collectPresses(node: SemanticsNode): List<String?> =
        node.config.getOrNull(SemanticsActions.OnClick).let { press ->
            if (press == null) emptyList() else listOf(press.label)
        } + node.children.flatMap { collectPresses(it) }

    private fun topOf(description: String): Float =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().positionInRoot.y

    private fun nameX(): Float =
        composeRule.onNodeWithText("Groceries").fetchSemanticsNode().positionInRoot.x

    private fun baselineOf(text: String): Float {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return node.positionInRoot.y + results.first().firstBaseline
    }

    @Composable
    private fun Row(
        summary: TodoListSummary,
        tearing: Boolean,
        editable: Boolean,
        rewritable: Boolean = true
    ) {
        ListCard(
            summary = summary,
            animated = false,
            onOpen = { opened += 1 },
            tearing = tearing,
            onTorn = { torn += 1 },
            onActionsRequested = if (editable) ({ actionsRequested += 1 }) else null,
            onRewriteDate = if (rewritable) ({ rewritten += it }) else null
        )
    }

    private fun summary(
        name: String = "Groceries",
        allDone: Boolean = false,
        activeCount: Int = 0,
        completedCount: Int = 0,
        targetDate: LocalDate? = null,
        dueDate: LocalDate? = null,
        isTargetDateElapsed: Boolean = false,
        showTargetYear: Boolean = false,
        dueDateStatus: DueDateStatus? = null,
        showDueDateYear: Boolean = false,
        preview: List<String> = emptyList(),
        daysLate: Int = 0
    ) = TodoListSummary(
        list = TodoList("list-1", name, targetDate = targetDate, dueDate = dueDate),
        allDone = allDone,
        activeCount = activeCount,
        completedCount = completedCount,
        isTargetDateElapsed = isTargetDateElapsed,
        showTargetYear = showTargetYear,
        dueDateStatus = dueDateStatus,
        showDueDateYear = showDueDateYear,
        preview = preview,
        daysLate = daysLate
    )

    private fun printed(date: LocalDate): String =
        cardDate(date, showYear = false, locale = locale()).uppercase(locale())

    private fun spelled(date: LocalDate): String =
        formatListDate(date, showYear = true, locale = locale())

    private fun locale(): Locale = Locale.getDefault(Locale.Category.FORMAT)

    private fun texts(): List<String> = collectText(composeRule.onRoot().fetchSemanticsNode())

    private fun collectText(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            .map { it.text }
            .filter { it.isNotBlank() } + node.children.flatMap { collectText(it) }

    private fun descriptions(): List<String> =
        collectDescriptions(composeRule.onRoot().fetchSemanticsNode())

    private fun collectDescriptions(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            .filter { it.isNotBlank() } + node.children.flatMap { collectDescriptions(it) }

    private fun widthOf(text: String): Int =
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().size.width

    private fun textStyleOf(text: String): TextStyle? = layoutOf(text)?.layoutInput?.style

    private fun layoutOf(text: String): TextLayoutResult? {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        return results.firstOrNull()
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 3, 14)
        val OPENS_THE_LIST: String? = null
        const val LIST_ACTIONS = "More for this list"
        const val GIVE_A_DAY = "Give this list a day"
        const val ONE_LINE = 1
    }
}
