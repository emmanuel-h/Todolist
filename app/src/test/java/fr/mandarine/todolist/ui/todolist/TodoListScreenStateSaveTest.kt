package fr.mandarine.todolist.ui.todolist

import androidx.compose.runtime.saveable.SaverScope
import fr.mandarine.todolist.ui.ConfirmDeleteRequest
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.DateSelection
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoListScreenStateSaveTest {

    private val scope = SaverScope { true }

    private fun roundTrip(state: TodoListScreenState): TodoListScreenState {
        val saved = with(TodoListScreenState.Saver) { scope.save(state) }
        return requireNotNull(TodoListScreenState.Saver.restore(requireNotNull(saved)))
    }

    @Test
    fun `should carry a half-written item across`() {
        val restored = roundTrip(
            TodoListScreenState().apply {
                addRowExpanded = true
                addRowText = "Appl"
            }
        )

        assertTrue(restored.addRowExpanded)
        assertEquals("Appl", restored.addRowText)
    }

    @Test
    fun `should carry an open title editor across`() {
        val restored = roundTrip(TodoListScreenState().apply { editingItemId = "item-7" })

        assertEquals("item-7", restored.editingItemId)
    }

    @Test
    fun `should carry an open rename across`() {
        val restored = roundTrip(TodoListScreenState().apply { renamingList = true })

        assertTrue(restored.renamingList)
    }

    @Test
    fun `should carry an open date sheet across`() {
        val restored = roundTrip(
            TodoListScreenState().apply {
                dateSheet = DateSelection(DateKind.DUE, LocalDate.of(2026, 7, 8))
            }
        )

        assertEquals(DateKind.DUE, restored.dateSheet?.kind)
        assertEquals(LocalDate.of(2026, 7, 8), restored.dateSheet?.date)
    }

    @Test
    fun `should carry a date sheet with no day circled across as no day`() {
        val restored = roundTrip(
            TodoListScreenState().apply { dateSheet = DateSelection(DateKind.TARGET, null) }
        )

        assertEquals(DateKind.TARGET, restored.dateSheet?.kind)
        assertNull(restored.dateSheet?.date)
    }

    @Test
    fun `should leave a shut page shut`() {
        val restored = roundTrip(TodoListScreenState())

        assertNull(restored.dateSheet)
        assertNull(restored.editingItemId)
        assertEquals("", restored.addRowText)
    }

    /**
     * A delete the reader has already confirmed. The tear was dropped on a
     * rotation, so the delete simply did not happen and the row came back.
     */
    @Test
    fun `should carry a row already tearing across`() {
        val restored = roundTrip(TodoListScreenState().apply { tearingId = "item-3" })

        assertEquals("item-3", restored.tearingId)
    }

    @Test
    fun `should carry an open delete slip across`() {
        val restored = roundTrip(
            TodoListScreenState().apply {
                confirmDelete = ConfirmDeleteRequest("item-4", "Milk", null)
            }
        )

        assertEquals("item-4", restored.confirmDelete?.id)
        assertEquals("Milk", restored.confirmDelete?.name)
        assertNull(restored.confirmDelete?.cascadeCount)
    }

    @Test
    fun `should carry the count a delete slip warns about across`() {
        val restored = roundTrip(
            TodoListScreenState().apply {
                confirmDelete = ConfirmDeleteRequest("list-1", "Groceries", 3)
            }
        )

        assertEquals(3, restored.confirmDelete?.cascadeCount)
    }

    @Test
    fun `should carry a tick still being drawn across`() {
        val restored = roundTrip(
            TodoListScreenState().apply {
                startToggle("item-1", drawing = true)
                startToggle("item-2", drawing = false)
            }
        )

        assertEquals(mapOf("item-1" to true, "item-2" to false), restored.pendingToggles)
    }

    /**
     * A tick already written must not be written again on the way back — the
     * restored page re-arms the effect that writes, and a second write flips the
     * row back.
     */
    @Test
    fun `should remember which ticks have already been written`() {
        val restored = roundTrip(
            TodoListScreenState().apply {
                startToggle("item-1", drawing = true)
                markToggleIssued("item-1")
            }
        )

        assertEquals(setOf("item-1"), restored.issuedToggles)
    }

    @Test
    fun `should leave a page with nothing in flight with nothing in flight`() {
        val restored = roundTrip(TodoListScreenState())

        assertNull(restored.tearingId)
        assertNull(restored.confirmDelete)
        assertTrue(restored.pendingToggles.isEmpty())
        assertTrue(restored.issuedToggles.isEmpty())
    }
}
