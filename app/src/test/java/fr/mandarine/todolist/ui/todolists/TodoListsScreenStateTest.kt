package fr.mandarine.todolist.ui.todolists

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TutorialBounds
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoListsScreenStateTest {

    private val state = TodoListsScreenState()
    private val bounds = TutorialBounds(left = 10, top = 20, width = 30, height = 40)
    private val date = LocalDate.of(2026, 3, 14)

    @Test
    fun `should hand back the bounds recorded for an anchor`() {
        state.putBounds(TutorialAnchor.CreateListButton, bounds)

        assertEquals(bounds, state.boundsOf(TutorialAnchor.CreateListButton))
    }

    @Test
    fun `should hand back nothing once an anchor leaves the composition`() {
        state.putBounds(TutorialAnchor.CreateListButton, bounds)

        state.removeBounds(TutorialAnchor.CreateListButton)

        assertNull(state.boundsOf(TutorialAnchor.CreateListButton))
    }

    @Test
    fun `should leave a pending deletion alone when the create row opens`() {
        state.deletion.request("list-1")

        state.openAddRow()

        assertTrue(state.addRowExpanded)
        assertEquals("list-1", state.deletion.pending?.id)
    }

    @Test
    fun `should forget the typed name and the picked date when the create row closes`() {
        state.openAddRow()
        state.addRowText = "Groceries"
        state.addRowSelection = DateSelection(DateKind.DUE, date)

        state.closeAddRow()

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

        val submitted = submitAddRow(state) { name, target, due ->
            created = Triple(name, target, due)
        }

        assertTrue(submitted)
        assertEquals(Triple("Groceries", date, null), created)
        assertTrue(state.addRowExpanded)
        assertEquals("", state.addRowText)
        assertEquals(DateSelection.None, state.addRowSelection)
    }

    @Test
    fun `should abandon the typed name when the pen goes down on the create row`() {
        state.openAddRow()
        state.addRowText = "Groceries"

        state.closeAddRow()

        assertFalse(state.addRowExpanded)
        assertEquals("", state.addRowText)
    }

    @Test
    fun `should keep the create row open when the name is blank`() {
        var created = false
        state.openAddRow()
        state.addRowText = "   "

        val submitted = submitAddRow(state) { _, _, _ -> created = true }

        assertFalse(submitted)
        assertFalse(created)
        assertTrue(state.addRowExpanded)
    }

    @Test
    fun `should apply a picked date to the create row`() {
        val request = DatePickerRequest(DateTarget.ADD_ROW, DateKind.DUE, null)

        applyPickedDate(state, request, date)

        assertEquals(DateSelection(DateKind.DUE, date), state.addRowSelection)
    }

    @Test
    fun `should apply a picked date to the rename dialog`() {
        state.rename = RenameState.of(TodoList("list-1", "Groceries"))
        val request = DatePickerRequest(DateTarget.RENAME, DateKind.TARGET, null)

        applyPickedDate(state, request, date)

        assertEquals(DateSelection(DateKind.TARGET, date), state.rename?.selection)
    }

    @Test
    fun `should ignore a picked date when the rename dialog has already closed`() {
        val request = DatePickerRequest(DateTarget.RENAME, DateKind.TARGET, null)

        applyPickedDate(state, request, date)

        assertNull(state.rename)
    }
}
