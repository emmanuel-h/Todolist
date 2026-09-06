package fr.mandarine.todolist.ui.todolist

import fr.mandarine.todolist.domain.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class TodoListScreenStateTest {

    private val state = TodoListScreenState()

    @Test
    fun `should hold the staged order while the page is still handed the old one`() {
        state.stageOrder(listOf("item-2", "item-1"))

        state.releaseOrder(listOf("item-1", "item-2"))

        assertEquals(listOf("item-2", "item-1"), state.previewOrder)
    }

    @Test
    fun `should let the staged order go once the page is handed exactly it`() {
        state.stageOrder(listOf("item-2", "item-1"))

        state.releaseOrder(listOf("item-2", "item-1"))

        assertNull(state.previewOrder)
    }

    @Test
    fun `should raise a fresh keyboard-hide signal on every request`() {
        val first = state.hideKeyboardSignal

        state.requestHideKeyboard()
        val second = state.hideKeyboardSignal
        state.requestHideKeyboard()

        assertEquals(first + 1, second)
        assertEquals(first + 2, state.hideKeyboardSignal)
    }

    /**
     * The tick stays drawn while the write is in flight. Letting go as soon as the
     * write was issued left a frame reading the old stored value, which the ring
     * erased and buzzed for before drawing it again.
     */
    @Test
    fun `should keep drawing the tick while the store still says otherwise`() {
        val item = TodoItem("item-1", "Milk", "list-1", isCompleted = false)
        state.startToggle("item-1", drawing = true)

        state.releaseToggles(listOf(item))

        assertTrue(state.inked(item))
    }

    @Test
    fun `should let the tick go once the store says the same thing`() {
        val item = TodoItem("item-1", "Milk", "list-1", isCompleted = false)
        state.startToggle("item-1", drawing = true)

        state.releaseToggles(listOf(item.copy(isCompleted = true)))

        assertTrue(state.pendingToggles.isEmpty())
    }

    @Test
    fun `should draw what the store says for a row with nothing pending`() {
        val item = TodoItem("item-1", "Milk", "list-1", isCompleted = true)

        assertTrue(state.inked(item))
        assertFalse(state.inked(item.copy(isCompleted = false)))
    }

    @Test
    fun `should hold one row's tick while letting another go`() {
        val milk = TodoItem("milk", "Milk", "list-1", isCompleted = false)
        val eggs = TodoItem("eggs", "Eggs", "list-1", isCompleted = false)
        state.startToggle("milk", drawing = true)
        state.startToggle("eggs", drawing = true)

        state.releaseToggles(listOf(milk.copy(isCompleted = true), eggs))

        assertEquals(setOf("eggs"), state.pendingToggles.keys)
    }
}
