package fr.mandarine.todolist.ui.todolist

import androidx.compose.runtime.saveable.SaverScope
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
}
