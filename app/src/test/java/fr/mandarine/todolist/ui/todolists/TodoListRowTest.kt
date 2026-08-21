package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
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
class TodoListRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var opened = 0
    private var renamed = 0
    private var deleteRequested = 0
    private var deleteCancelled = 0
    private var deleteConfirmed = 0

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
    fun `should show both counts as bare numbers`() {
        render(summary(activeCount = 3, completedCount = 12))

        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertIsDisplayed()
    }

    @Test
    fun `should show zero in both counts when the list is untouched`() {
        render(summary(activeCount = 0, completedCount = 0))

        assertEquals(listOf("Groceries", "0", "0"), texts())
    }

    @Test
    fun `should strike the name through when every item is done`() {
        render(summary(allDone = true))

        assertEquals(TextDecoration.LineThrough, textStyleOf("Groceries")?.textDecoration)
    }

    @Test
    fun `should fade the name when every item is done`() {
        render(summary(allDone = true))

        assertEquals(0.5f, textStyleOf("Groceries")!!.color.alpha, 0.01f)
    }

    @Test
    fun `should leave the name unstruck while the list still has work`() {
        render(summary(allDone = false))

        assertNull(textStyleOf("Groceries")?.textDecoration)
    }

    @Test
    fun `should open the rename dialog when the edit affordance is tapped`() {
        render(summary())

        composeRule.onNodeWithContentDescription(EDIT).performClick()

        assertEquals(1, renamed)
    }

    @Test
    fun `should offer a drag handle on the row`() {
        render(summary())

        composeRule.onNodeWithContentDescription(DRAG_HANDLE).assertIsDisplayed()
    }

    @Test
    fun `should not delete on the first tap of the delete affordance`() {
        render(summary())

        composeRule.onNodeWithContentDescription(DELETE_LIST).performClick()

        assertEquals(1, deleteRequested)
        assertEquals(0, deleteConfirmed)
    }

    @Test
    fun `should name the list in the confirm strip once delete is armed`() {
        render(summary(name = "Groceries"), confirmingDelete = true)

        assertTrue(texts().count { it == "Groceries" } >= 1)
        composeRule.onNodeWithContentDescription(CONFIRM).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(CANCEL).assertIsDisplayed()
    }

    @Test
    fun `should delete the list when the confirm ring is tapped`() {
        render(summary(), confirmingDelete = true)

        composeRule.onNodeWithContentDescription(CONFIRM).performClick()

        assertEquals(1, deleteConfirmed)
    }

    @Test
    fun `should disarm the delete when the cancel ring is tapped`() {
        render(summary(), confirmingDelete = true)

        composeRule.onNodeWithContentDescription(CANCEL).performClick()

        assertEquals(1, deleteCancelled)
        assertEquals(0, deleteConfirmed)
    }

    @Test
    fun `should hide the confirm strip while the delete is unarmed`() {
        render(summary(), confirmingDelete = false)

        composeRule.onNodeWithContentDescription(CONFIRM).assertDoesNotExist()
    }

    @Test
    fun `should show no date line when the list has neither date`() {
        render(summary())

        assertEquals(listOf("Groceries", "0", "0"), texts())
    }

    @Test
    fun `should show the target date when the list has one`() {
        render(summary(targetDate = DATE))

        composeRule.onNodeWithText(formatted(DATE)).assertIsDisplayed()
    }

    @Test
    fun `should show the target date with its year when the year is not obvious`() {
        render(summary(targetDate = DATE, showTargetYear = true))

        assertTrue("Expected the year in ${texts()}", texts().any { it.contains("2026") })
    }

    @Test
    fun `should show the due date when the list has one`() {
        render(summary(dueDate = DATE, dueDateStatus = DueDateStatus.FUTURE))

        composeRule.onNodeWithText(formatted(DATE)).assertIsDisplayed()
    }

    @Test
    fun `should show no due date line when the status has not been computed`() {
        render(summary(dueDate = DATE, dueDateStatus = null))

        assertEquals(listOf("Groceries", "0", "0"), texts())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun render(summary: TodoListSummary, confirmingDelete: Boolean = false) {
        composeRule.setContent { PaperTheme { Row(summary, confirmingDelete) } }
    }

    @Composable
    private fun Row(summary: TodoListSummary, confirmingDelete: Boolean) {
        TodoListRow(
            summary = summary,
            confirmingDelete = confirmingDelete,
            animated = false,
            onOpen = { opened += 1 },
            onRename = { renamed += 1 },
            onDeleteRequested = { deleteRequested += 1 },
            onDeleteCancelled = { deleteCancelled += 1 },
            onDeleteConfirmed = { deleteConfirmed += 1 }
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

    private fun formatted(date: LocalDate): String =
        formatListDate(date, showYear = false, locale = Locale.getDefault(Locale.Category.FORMAT))

    private fun texts(): List<String> = collectText(composeRule.onRoot().fetchSemanticsNode())

    private fun collectText(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            .map { it.text }
            .filter { it.isNotBlank() } + node.children.flatMap { collectText(it) }

    private fun textStyleOf(text: String): TextStyle? {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        return results.firstOrNull()?.layoutInput?.style
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 3, 14)
        const val EDIT = "Edit list name"
        const val DELETE_LIST = "Delete list"
        const val DRAG_HANDLE = "Drag to reorder"
        const val CONFIRM = "Delete"
        const val CANCEL = "Cancel"
    }
}
