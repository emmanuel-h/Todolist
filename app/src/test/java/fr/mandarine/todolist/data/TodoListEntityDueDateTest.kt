package fr.mandarine.todolist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoListEntityDueDateTest {

    @Test
    fun `should create entity with null dueDate by default`() {
        val entity = TodoListEntity("1", "Groceries")
        assertNull(entity.dueDate)
    }

    @Test
    fun `should create entity with given dueDate`() {
        val entity = TodoListEntity("1", "Groceries", 0, null, 20000L)
        assertEquals(20000L, entity.dueDate)
    }

    @Test
    fun `should be equal when dueDate is the same`() {
        assertEquals(
            TodoListEntity("1", "Groceries", 0, null, 20000L),
            TodoListEntity("1", "Groceries", 0, null, 20000L)
        )
    }

    @Test
    fun `should not be equal when dueDate differs`() {
        assertNotEquals(
            TodoListEntity("1", "Groceries", 0, null, 20000L),
            TodoListEntity("1", "Groceries", 0, null, 20001L)
        )
    }

    @Test
    fun `should not be equal when one has dueDate and other has null`() {
        assertNotEquals(
            TodoListEntity("1", "Groceries", 0, null, 20000L),
            TodoListEntity("1", "Groceries", 0, null, null)
        )
    }

    @Test
    fun `should copy entity with updated dueDate`() {
        val original = TodoListEntity("1", "Groceries", 0, null, null)
        val copy = original.copy(dueDate = 20000L)
        assertEquals("1", copy.id)
        assertEquals("Groceries", copy.name)
        assertEquals(20000L, copy.dueDate)
    }

    @Test
    fun `should copy entity and clear dueDate`() {
        val original = TodoListEntity("1", "Groceries", 0, null, 20000L)
        val copy = original.copy(dueDate = null)
        assertNull(copy.dueDate)
    }
}
