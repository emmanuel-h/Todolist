package fr.mandarine.todolist.ui.todolist

import org.junit.Assert.assertEquals
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
}
