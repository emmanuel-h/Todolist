package fr.mandarine.todolist.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletionStateTest {

    private val state = DeletionState()

    @Test
    fun `should keep the row on the page while it is still tearing off`() {
        state.request("a")

        assertTrue(state.tearing("a"))
        assertFalse(state.hides("a"))
    }

    @Test
    fun `should drop the row from the page once the tear has finished`() {
        state.request("a")

        state.markTorn()

        assertFalse(state.tearing("a"))
        assertTrue(state.hides("a"))
    }

    @Test
    fun `should leave every other row alone`() {
        state.request("a")
        state.markTorn()

        assertFalse(state.hides("b"))
        assertFalse(state.tearing("b"))
    }

    @Test
    fun `should show the slip only while a deletion is waiting`() {
        assertNull(state.pending)

        state.request("a")

        assertEquals("a", state.pending?.id)
    }

    @Test
    fun `should hand back the id to write through when the slip settles away`() {
        state.request("a")

        assertEquals("a", state.commit())
        assertNull(state.pending)
    }

    @Test
    fun `should keep the row hidden after the commit until the repository catches up`() {
        state.request("a")
        state.commit()

        assertTrue(state.hides("a"))

        state.forget(emptySet())

        assertFalse(state.hides("a"))
    }

    @Test
    fun `should keep the row hidden while the repository still reports it`() {
        state.request("a")
        state.commit()

        state.forget(setOf("a"))

        assertTrue(state.hides("a"))
    }

    @Test
    fun `should put the row back and write nothing through when the slip is tapped`() {
        state.request("a")

        assertEquals("a", state.undo())
        assertFalse(state.hides("a"))
        assertNull(state.pending)
    }

    @Test
    fun `should hand back nothing to commit or undo when nothing is pending`() {
        assertNull(state.commit())
        assertNull(state.undo())
    }

    @Test
    fun `should finish the first deletion when a second row is torn off`() {
        state.request("a")

        assertEquals("a", state.request("b"))
        assertTrue(state.hides("a"))
        assertTrue(state.tearing("b"))
    }
}
