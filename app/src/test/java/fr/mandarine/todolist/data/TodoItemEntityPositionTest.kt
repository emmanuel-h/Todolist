package fr.mandarine.todolist.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoItemEntityPositionTest {

    @Test
    fun `should default position to zero when not specified`() {
        val entity = TodoItemEntity("1", "Buy milk", "list-1")
        assertEquals(0, entity.position)
    }

    @Test
    fun `should store provided position value`() {
        val entity = TodoItemEntity("1", "Buy milk", "list-1", position = 5)
        assertEquals(5, entity.position)
    }

    @Test
    fun `should copy entity with updated position`() {
        val original = TodoItemEntity("1", "Buy milk", "list-1", position = 0)
        val copy = original.copy(position = 3)
        assertEquals(3, copy.position)
        assertEquals("1", copy.id)
        assertEquals("Buy milk", copy.title)
    }
}
