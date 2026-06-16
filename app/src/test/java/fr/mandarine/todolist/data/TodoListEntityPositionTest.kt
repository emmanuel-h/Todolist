package fr.mandarine.todolist.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoListEntityPositionTest {

    @Test
    fun `should default position to zero when not specified`() {
        val entity = TodoListEntity("1", "Groceries")
        assertEquals(0, entity.position)
    }

    @Test
    fun `should store provided position value`() {
        val entity = TodoListEntity("1", "Groceries", position = 5)
        assertEquals(5, entity.position)
    }

    @Test
    fun `should copy entity with updated position`() {
        val original = TodoListEntity("1", "Groceries", position = 0)
        val copy = original.copy(position = 3)
        assertEquals(3, copy.position)
        assertEquals("1", copy.id)
        assertEquals("Groceries", copy.name)
    }
}
