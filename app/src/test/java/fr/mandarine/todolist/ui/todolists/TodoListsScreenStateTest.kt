package fr.mandarine.todolist.ui.todolists

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.presentation.TodoListsState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoListsScreenStateTest {

    private val state = TodoListsScreenState()
    private val date = LocalDate.of(2026, 3, 14)

    @Test
    fun `should hold the staged order while the page is still handed the old one`() {
        state.stageOrder(listOf("list-2", "list-1"))

        state.releaseOrder(listOf("list-1", "list-2"))

        assertEquals(listOf("list-2", "list-1"), state.previewOrder)
    }

    @Test
    fun `should let the staged order go once the page is handed exactly it`() {
        state.stageOrder(listOf("list-2", "list-1"))

        state.releaseOrder(listOf("list-2", "list-1"))

        assertNull(state.previewOrder)
    }

    @Test
    fun `should leave a pending deletion alone when the create row opens`() {
        state.deletion.request("list-1")

        state.openAddRow()

        assertTrue(state.addRowExpanded)
        assertEquals("list-1", state.deletion.pending?.id)
    }

    @Test
    fun `should forget the typed name and the picked date when the create row is abandoned`() {
        state.openAddRow()
        state.addRowText = "Groceries"
        state.addRowSelection = DateSelection(DateKind.DUE, date)

        state.abandonAddRow()

        assertFalse(state.addRowExpanded)
        assertEquals("", state.addRowText)
        assertEquals(DateSelection.None, state.addRowSelection)
    }

    @Test
    fun `should drop in the row that arrived on the same publish as the create`() {
        state.dropInFor(listOf("a"))
        state.noteListAdded()

        assertEquals("b", state.dropInFor(listOf("b", "a")))
    }

    @Test
    fun `should not drop in a row that was already on the page`() {
        state.dropInFor(listOf("a", "b"))
        state.noteListAdded()

        assertNull(state.dropInFor(listOf("b", "a")))
    }

    @Test
    fun `should not drop in rows read from the repository without a create`() {
        assertNull(state.dropInFor(listOf("a", "b")))
    }

    @Test
    fun `should drop in a row only once`() {
        state.dropInFor(listOf("a"))
        state.noteListAdded()
        state.dropInFor(listOf("b", "a"))

        assertNull(state.dropInFor(listOf("b", "a")))
    }

    @Test
    fun `should open the rename dialog on the list's own name and date`() {
        val list = TodoList("list-1", "Groceries", dueDate = date)

        val rename = RenameState.of(list)

        assertEquals("list-1", rename.listId)
        assertEquals("Groceries", rename.name)
        assertEquals(DateKind.DUE, rename.selection.kind)
        assertEquals(date, rename.selection.date)
    }

    @Test
    fun `should open the rename dialog in target mode when the list has no due date`() {
        val list = TodoList("list-1", "Groceries", targetDate = date)

        val rename = RenameState.of(list)

        assertEquals(DateKind.TARGET, rename.selection.kind)
        assertEquals(date, rename.selection.date)
    }

    @Test
    fun `should create the list with the typed name and picked date when submitted`() {
        var created: Triple<String, LocalDate?, LocalDate?>? = null
        state.openAddRow()
        state.addRowText = "Groceries"
        state.addRowSelection = DateSelection(DateKind.TARGET, date)

        val submitted = submitAddRow(
            state,
            onCreateList = { name, target, due -> created = Triple(name, target, due) }
        )

        assertTrue(submitted)
        assertEquals(Triple("Groceries", date, null), created)
        assertTrue(state.addRowExpanded)
        assertEquals("", state.addRowText)
        assertEquals(DateSelection.None, state.addRowSelection)
    }

    /**
     * The day was circled on a line, and a line is not a list. The ask is owed
     * when the list exists — backing out of the line writes no reminder at all,
     * and spending the one ask on it left the reader silently un-remindable.
     */
    @Test
    fun `should owe the ask once a line carrying a day becomes a list`() {
        var owed = false
        state.addRowText = "Groceries"
        state.addRowSelection = DateSelection(DateKind.TARGET, date)

        submitAddRow(state, onCreateList = { _, _, _ -> }, onReminderWritten = { owed = true })

        assertTrue(owed)
    }

    @Test
    fun `should owe the ask for a due date the same way`() {
        var owed = false
        state.addRowText = "Groceries"
        state.addRowSelection = DateSelection(DateKind.DUE, date)

        submitAddRow(state, onCreateList = { _, _, _ -> }, onReminderWritten = { owed = true })

        assertTrue(owed)
    }

    @Test
    fun `should owe nothing when the line carries no day`() {
        var owed = false
        state.addRowText = "Groceries"

        submitAddRow(state, onCreateList = { _, _, _ -> }, onReminderWritten = { owed = true })

        assertFalse(owed)
    }

    @Test
    fun `should owe nothing when a blank line is refused`() {
        var owed = false
        state.addRowText = "   "
        state.addRowSelection = DateSelection(DateKind.DUE, date)

        submitAddRow(state, onCreateList = { _, _, _ -> }, onReminderWritten = { owed = true })

        assertFalse(owed)
    }

    @Test
    /**
     * Putting the pen down folds the line away without tearing up what was on it,
     * which is what the page of items has always done with a half-written row.
     */
    fun `should keep the typed name when the pen goes down on the create row`() {
        state.openAddRow()
        state.addRowText = "Groceries"
        state.addRowSelection = DateSelection(DateKind.DUE, date)

        state.closeAddRow()

        assertFalse(state.addRowExpanded)
        assertEquals("Groceries", state.addRowText)
        assertEquals(DateSelection(DateKind.DUE, date), state.addRowSelection)
    }

    @Test
    fun `should keep the create row open when the name is blank`() {
        var created = false
        state.openAddRow()
        state.addRowText = "   "

        val submitted = submitAddRow(state, onCreateList = { _, _, _ -> created = true })

        assertFalse(submitted)
        assertFalse(created)
        assertTrue(state.addRowExpanded)
    }

    @Test
    fun `should apply a picked date to the create row`() {
        val request = DatePickerRequest(DateTarget.AddRow, DateKind.DUE, null)

        applyPickedDate(state, request, date, ::writeRow)

        assertEquals(DateSelection(DateKind.DUE, date), state.addRowSelection)
    }

    @Test
    fun `should apply a picked date to the rename dialog`() {
        state.rename = RenameState.of(TodoList("list-1", "Groceries"))
        val request = DatePickerRequest(DateTarget.Rename, DateKind.TARGET, null)

        applyPickedDate(state, request, date, ::writeRow)

        assertEquals(DateSelection(DateKind.TARGET, date), state.rename?.selection)
    }

    @Test
    fun `should ignore a picked date when the rename dialog has already closed`() {
        val request = DatePickerRequest(DateTarget.Rename, DateKind.TARGET, null)

        applyPickedDate(state, request, date, ::writeRow)

        assertNull(state.rename)
    }

    @Test
    /**
     * A day circled on a line owes nothing yet: the line may never become a list.
     * The ask is owed at the commit, which submitAddRow answers for.
     */
    fun `should report no ask owed for a day circled on the line being written`() {
        val request = DatePickerRequest(DateTarget.AddRow, DateKind.DUE, null)

        assertFalse(applyPickedDate(state, request, date, ::writeRow))
    }

    @Test
    fun `should still write the day circled on the line being written`() {
        val request = DatePickerRequest(DateTarget.AddRow, DateKind.TARGET, null)

        applyPickedDate(state, request, date, ::writeRow)

        assertEquals(DateSelection(DateKind.TARGET, date), state.addRowSelection)
    }

    @Test
    fun `should report the ask owed when the alarm is rung over the day on the line`() {
        state.addRowSelection = DateSelection(DateKind.TARGET, date)

        val owed = writeAddRowSelection(state, state.addRowSelection.withKind(DateKind.DUE))

        assertTrue(owed)
        assertEquals(date, state.addRowSelection.dueDate)
    }

    @Test
    fun `should report the ask owed when the alarm is rung over the day on the sheet`() {
        state.rename = RenameState.of(TodoList("list-1", "Groceries", targetDate = date))

        val owed = writeRenameSelection(state, DateSelection(DateKind.DUE, date))

        assertTrue(owed)
        assertEquals(date, state.rename?.selection?.dueDate)
    }

    @Test
    fun `should report no ask owed when the sheet has already been put down`() {
        assertFalse(writeRenameSelection(state, DateSelection(DateKind.DUE, date)))
    }

    @Test
    fun `should leave the line bare until words are written on it`() {
        state.openAddRow()

        assertFalse(dateMarksOwed(state))
    }

    @Test
    fun `should owe the line its date marks once words are written on it`() {
        state.openAddRow()
        state.addRowText = "Groceries"

        assertTrue(dateMarksOwed(state))
    }

    @Test
    fun `should keep the date marks on a line whose words were rubbed out`() {
        state.openAddRow()
        state.addRowSelection = DateSelection(DateKind.DUE, date)

        assertTrue(dateMarksOwed(state))
    }

    @Test
    fun `should lay the pad on the page when the page fills the window`() {
        assertTrue(padLiesOnPage(windowWidth = 411.dp, pageWidth = 640.dp, reach = 80.dp))
    }

    @Test
    fun `should lay the pad on the page when the margin beside it is too narrow`() {
        assertTrue(padLiesOnPage(windowWidth = 700.dp, pageWidth = 640.dp, reach = 80.dp))
    }

    @Test
    fun `should leave the pad off the page when the desk beside it is wide enough`() {
        assertFalse(padLiesOnPage(windowWidth = 914.dp, pageWidth = 640.dp, reach = 80.dp))
    }

    // ── A date jotted against a row already on the page ───────────────────────

    @Test
    fun `should hand a day circled for a row straight to that row's list`() {
        val request = DatePickerRequest(DateTarget.Row("list-1"), DateKind.DUE, null)

        applyPickedDate(state, request, date, ::writeRow)

        assertEquals(listOf("list-1" to DateSelection(DateKind.DUE, date)), rowWrites)
    }

    @Test
    fun `should leave the line and the edit sheet alone when a row is given a date`() {
        state.rename = RenameState.of(TodoList("list-1", "Groceries"))
        val request = DatePickerRequest(DateTarget.Row("list-1"), DateKind.TARGET, null)

        applyPickedDate(state, request, date, ::writeRow)

        assertEquals(DateSelection.None, state.addRowSelection)
        assertEquals(DateSelection.None, state.rename?.selection)
    }

    @Test
    fun `should report the ask owed when the day circled for a row lands under the alarm`() {
        val request = DatePickerRequest(DateTarget.Row("list-1"), DateKind.DUE, null)

        assertTrue(applyPickedDate(state, request, date, ::writeRow))
    }

    @Test
    fun `should write a row's new day through under the name the list already has`() {
        val owed = writeListDate(page, "list-1", DateSelection(DateKind.DUE, date), ::rename)

        assertTrue(owed)
        assertEquals(listOf(Rename("list-1", "Groceries", null, date)), renames)
    }

    @Test
    fun `should trade a list's target date for the due date circled against its row`() {
        writeListDate(page, "list-2", DateSelection(DateKind.DUE, date), ::rename)

        assertEquals(listOf(Rename("list-2", "Jardin", null, date)), renames)
    }

    @Test
    fun `should report no ask owed when a row keeps the day its alarm was already set to`() {
        val owed = writeListDate(page, "list-3", DateSelection(DateKind.DUE, date), ::rename)

        assertFalse(owed)
        assertEquals(listOf(Rename("list-3", "Courses", null, date)), renames)
    }

    @Test
    fun `should write nothing when the row's list has left the page`() {
        val owed = writeListDate(page, "gone", DateSelection(DateKind.DUE, date), ::rename)

        assertFalse(owed)
        assertEquals(emptyList<Rename>(), renames)
    }

    @Test
    fun `should reach a finished list below the divider as readily as an unfinished one`() {
        writeListDate(page, "list-4", DateSelection(DateKind.TARGET, date), ::rename)

        assertEquals(listOf(Rename("list-4", "Voyage", date, null)), renames)
    }

    private val rowWrites = mutableListOf<Pair<String, DateSelection>>()
    private val renames = mutableListOf<Rename>()

    private fun writeRow(listId: String, written: DateSelection): Boolean {
        rowWrites += listId to written
        return written.dueDate != null
    }

    private fun rename(
        listId: String,
        name: String,
        targetDate: LocalDate?,
        dueDate: LocalDate?
    ) {
        renames += Rename(listId, name, targetDate, dueDate)
    }

    private data class Rename(
        val listId: String,
        val name: String,
        val targetDate: LocalDate?,
        val dueDate: LocalDate?
    )

    private val page = TodoListsState.Content(
        activeSummaries = listOf(
            TodoListSummary(TodoList("list-1", "Groceries"), allDone = false),
            TodoListSummary(TodoList("list-2", "Jardin", targetDate = date), allDone = false),
            TodoListSummary(TodoList("list-3", "Courses", dueDate = date), allDone = false)
        ),
        doneSummaries = listOf(TodoListSummary(TodoList("list-4", "Voyage"), allDone = true))
    )
}
