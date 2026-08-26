package fr.mandarine.todolist.ui.todolists

import android.os.Bundle
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A rotation rebuilds the window, and this state hangs off the window rather than
 * off a composition. What the reader was in the middle of writing has to make the
 * crossing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsScreenStateSaveTest {

    @Test
    fun `should carry a half-written list name across`() {
        val saved = Bundle()
        TodoListsScreenState().apply {
            openAddRow()
            addRowText = "Groc"
        }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertTrue(restored.addRowExpanded)
        assertEquals("Groc", restored.addRowText)
    }

    @Test
    fun `should carry the day circled beside a half-written name across`() {
        val saved = Bundle()
        TodoListsScreenState().apply {
            addRowSelection = DateSelection(DateKind.DUE, LocalDate.of(2026, 3, 4))
        }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertEquals(DateKind.DUE, restored.addRowSelection.kind)
        assertEquals(LocalDate.of(2026, 3, 4), restored.addRowSelection.date)
    }

    @Test
    fun `should carry a line with no day circled across as no day`() {
        val saved = Bundle()
        TodoListsScreenState().apply { addRowText = "Work" }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertNull(restored.addRowSelection.date)
        assertEquals(DateKind.TARGET, restored.addRowSelection.kind)
    }

    @Test
    fun `should carry an open edit sheet across`() {
        val saved = Bundle()
        TodoListsScreenState().apply {
            rename = RenameState(
                listId = "list-1",
                name = "Weekend",
                selection = DateSelection(DateKind.TARGET, LocalDate.of(2026, 5, 6))
            )
        }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertEquals("list-1", restored.rename?.listId)
        assertEquals("Weekend", restored.rename?.name)
        assertEquals(LocalDate.of(2026, 5, 6), restored.rename?.selection?.date)
    }

    @Test
    fun `should leave the edit sheet shut when none was open`() {
        val saved = Bundle()
        TodoListsScreenState().saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertNull(restored.rename)
        assertFalse(restored.addRowExpanded)
    }
}
