package fr.mandarine.todolist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoItemEntityTest {

    @Test
    fun `should create entity with given id title and listId`() {
        val entity = TodoItemEntity("1", "Buy milk", "list-1")
        assertEquals("1", entity.id)
        assertEquals("Buy milk", entity.title)
        assertEquals("list-1", entity.listId)
    }

    @Test
    fun `should default completed to false when not specified`() {
        val entity = TodoItemEntity("1", "Buy milk", "list-1")
        assertFalse(entity.completed)
    }

    @Test
    fun `should store true when completed is set to true`() {
        val entity = TodoItemEntity("1", "Buy milk", "list-1", completed = true)
        assertTrue(entity.completed)
    }

    @Test
    fun `should be equal when all fields are the same`() {
        assertEquals(TodoItemEntity("1", "Buy milk", "list-1"), TodoItemEntity("1", "Buy milk", "list-1"))
    }

    @Test
    fun `should not be equal when id differs`() {
        assertNotEquals(TodoItemEntity("1", "Buy milk", "list-1"), TodoItemEntity("2", "Buy milk", "list-1"))
    }

    @Test
    fun `should not be equal when completed differs`() {
        assertNotEquals(
            TodoItemEntity("1", "Buy milk", "list-1", completed = false),
            TodoItemEntity("1", "Buy milk", "list-1", completed = true)
        )
    }

    @Test
    fun `should copy entity with updated title`() {
        val original = TodoItemEntity("1", "Buy milk", "list-1")
        val copy = original.copy(title = "Buy eggs")
        assertEquals("1", copy.id)
        assertEquals("Buy eggs", copy.title)
        assertEquals("list-1", copy.listId)
    }

    @Test
    fun `should copy entity toggling completed`() {
        val original = TodoItemEntity("1", "Buy milk", "list-1", completed = false)
        val toggled = original.copy(completed = true)
        assertTrue(toggled.completed)
        assertEquals("1", toggled.id)
    }
}
