package fr.mandarine.todolist.ui.todolist

import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TutorialBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoListScreenStateTest {

    private val state = TodoListScreenState()
    private val bounds = TutorialBounds(left = 10, top = 20, width = 30, height = 40)

    @Test
    fun `should hand back the bounds recorded for an anchor`() {
        state.putBounds(TutorialAnchor.ItemGhostRow, bounds)

        assertEquals(bounds, state.boundsOf(TutorialAnchor.ItemGhostRow))
    }

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
    fun `should hand back nothing for an anchor that was never recorded`() {
        assertNull(state.boundsOf(TutorialAnchor.SubmitItemButton))
    }

    @Test
    fun `should hand back nothing once an anchor leaves the composition`() {
        state.putBounds(TutorialAnchor.ItemGhostRow, bounds)

        state.removeBounds(TutorialAnchor.ItemGhostRow)

        assertNull(state.boundsOf(TutorialAnchor.ItemGhostRow))
    }

    @Test
    fun `should tell rows apart when the same anchor kind is recorded at two indices`() {
        val second = TutorialBounds(left = 11, top = 21, width = 31, height = 41)
        state.putBounds(TutorialAnchor.ActiveItemToggle(0), bounds)
        state.putBounds(TutorialAnchor.ActiveItemToggle(1), second)

        assertEquals(bounds, state.boundsOf(TutorialAnchor.ActiveItemToggle(0)))
        assertEquals(second, state.boundsOf(TutorialAnchor.ActiveItemToggle(1)))
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
