package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.SemanticsActions
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.listmeta.formatJotDate
import fr.mandarine.todolist.ui.paper.LocalPagePitch
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var opened = 0
    private var deleteRequested = 0
    private var renameRequested = 0
    private var torn = 0

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

    @Test
    fun `should write the open count as a bare numeral in the margin`() {
        render(summary(activeCount = 3, completedCount = 12))

        assertEquals(listOf("Groceries", "3"), texts())
    }

    @Test
    fun `should leave the margin empty when nothing is left to do`() {
        render(summary(activeCount = 0, completedCount = 4))

        assertEquals(listOf("Groceries"), texts())
    }

    @Test
    fun `should say how many items are left for a reader who cannot see the numeral`() {
        render(summary(activeCount = 3))

        composeRule.onNodeWithContentDescription("3 items left").assertIsDisplayed()
    }

    @Test
    fun `should carry nothing at rest but the name and its marginalia`() {
        render(summary(activeCount = 2))

        assertEquals(listOf("2 items left"), descriptions())
    }

    /**
     * The marginalia are written beside the name, not instead of it: anything in
     * the margin that claims the row's whole width squeezes the name down to one
     * letter a line, which is what a tally that fills its box does.
     */
    @Test
    fun `should leave the name the room the marginalia does not need`() {
        render(summary(name = "Groceries", activeCount = 3))

        val name = widthOf("Groceries")
        val tally = widthOf("3")

        assertEquals(ONE_LINE, layoutOf("Groceries")?.lineCount)
        assertTrue("name $name against a tally of $tally", name > tally)
    }

    @Test
    fun `should write the open count in pencil`() {
        render(summary(activeCount = 3))

        assertEquals(PaperPalette.light.inked(InkTone.Margin), textStyleOf("3")?.color)
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
    fun `should ask to tear the list off when the row is swiped away`() {
        render(summary())

        composeRule.onNodeWithText("Groceries").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(1, deleteRequested)
    }

    @Test
    fun `should ask to edit the list when the row is swiped the other way`() {
        render(summary())

        composeRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(1, renameRequested)
        assertEquals(0, deleteRequested)
        assertEquals(0, opened)
    }

    @Test
    fun `should bring the row home once the edit surface is asked for`() {
        render(summary())
        val resting = nameX()

        composeRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(resting, nameX(), 0.5f)
    }

    @Test
    fun `should leave a list alone when it offers no edit surface and is swiped that way`() {
        render(summary(), editable = false)

        composeRule.onNodeWithText("Groceries").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(0, renameRequested)
        assertEquals(0, deleteRequested)
    }

    @Test
    fun `should report the tear as finished once the row has left the page`() {
        render(summary(), tearing = true)

        composeRule.waitForIdle()

        assertEquals(1, torn)
    }

    @Test
    fun `should jot no date when the list has neither`() {
        render(summary())

        assertEquals(listOf("Groceries"), texts())
    }

    @Test
    fun `should jot the target date in the margin without its weekday`() {
        render(summary(targetDate = DATE))

        composeRule.onNodeWithText(jotted(DATE)).assertIsDisplayed()
    }

    @Test
    fun `should append the year to the jot when the year is not obvious`() {
        render(summary(targetDate = DATE, showTargetYear = true))

        assertTrue("Expected the year in ${texts()}", texts().any { it.contains("26") })
    }

    @Test
    fun `should read the jotted target date out in full`() {
        render(summary(targetDate = DATE))

        composeRule
            .onNodeWithContentDescription("Target date ${spelled(DATE)}")
            .assertIsDisplayed()
    }

    @Test
    fun `should jot the due date when the list has one`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        composeRule.onNodeWithText(jotted(DATE)).assertIsDisplayed()
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
    fun `should alarm on an overdue jot and stay in pencil on a future one`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.OVERDUE))

        assertEquals(PaperPalette.light.inked(InkTone.Lost), textStyleOf(jotted(DATE))?.color)
    }

    /**
     * Above scale 1.05 the platform compresses the larger `sp` sizes hardest, so
     * the 14sp marginalia used to write a taller line than the 20sp name it
     * annotates and pushed the row onto a second rule half a line below it.
     */
    @Test
    fun `should hold a name its jot and its tally to one rule when the font scale bends`() {
        RuntimeEnvironment.setFontScale(BENT_SCALE)
        var pitch = 0
        composeRule.setContent {
            PaperTheme {
                pitch = with(LocalDensity.current) { LocalPagePitch.current.roundToPx() }
                Box(Modifier.testTag(ROW)) {
                    Row(
                        summary(
                            activeCount = 4,
                            dueDate = DATE,
                            dueDateStatus = DueDateStatus.FUTURE
                        ),
                        tearing = false,
                        editable = true
                    )
                }
            }
        }

        assertEquals(pitch, composeRule.onNodeWithTag(ROW).fetchSemanticsNode().size.height)
        assertEquals(baselineOf("Groceries"), baselineOf(jotted(DATE)), ONE_PIXEL)
        assertEquals(baselineOf("Groceries"), baselineOf("4"), ONE_PIXEL)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun render(
        summary: TodoListSummary,
        tearing: Boolean = false,
        editable: Boolean = true
    ) {
        composeRule.setContent { PaperTheme { Row(summary, tearing, editable) } }
    }

    private fun nameX(): Float =
        composeRule.onNodeWithText("Groceries").fetchSemanticsNode().positionInRoot.x

    private fun baselineOf(text: String): Float {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return node.positionInRoot.y + results.first().firstBaseline
    }

    @Composable
    private fun Row(summary: TodoListSummary, tearing: Boolean, editable: Boolean) {
        TodoListRow(
            summary = summary,
            animated = false,
            onOpen = { opened += 1 },
            onDeleteRequested = { deleteRequested += 1 },
            tearing = tearing,
            onTorn = { torn += 1 },
            onRenameRequested = if (editable) ({ renameRequested += 1 }) else null
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
        showDueDateYear: Boolean = false
    ) = TodoListSummary(
        list = TodoList("list-1", name, targetDate = targetDate, dueDate = dueDate),
        allDone = allDone,
        activeCount = activeCount,
        completedCount = completedCount,
        isTargetDateElapsed = isTargetDateElapsed,
        showTargetYear = showTargetYear,
        dueDateStatus = dueDateStatus,
        showDueDateYear = showDueDateYear
    )

    private fun jotted(date: LocalDate): String =
        formatJotDate(date, showYear = false, locale = locale())

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
        const val ROW = "row"
        const val ONE_LINE = 1
        const val ONE_PIXEL = 1f
        const val BENT_SCALE = 1.3f
    }
}
