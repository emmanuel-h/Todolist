package fr.mandarine.todolist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TodoListEntityTest {

    @Test
    fun `should create entity with given id and name`() {
        val entity = TodoListEntity("1", "Groceries")
        assertEquals("1", entity.id)
        assertEquals("Groceries", entity.name)
    }

    @Test
    fun `should be equal when id and name are the same`() {
        assertEquals(TodoListEntity("1", "Groceries"), TodoListEntity("1", "Groceries"))
    }

    @Test
    fun `should not be equal when id differs`() {
        assertNotEquals(TodoListEntity("1", "Groceries"), TodoListEntity("2", "Groceries"))
    }

    @Test
    fun `should copy entity with updated name`() {
        val original = TodoListEntity("1", "Groceries")
        val copy = original.copy(name = "Home")
        assertEquals("1", copy.id)
        assertEquals("Home", copy.name)
    }
}
