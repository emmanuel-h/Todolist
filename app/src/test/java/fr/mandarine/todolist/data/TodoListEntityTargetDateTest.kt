package fr.mandarine.todolist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoListEntityTargetDateTest {

    @Test
    fun `should create entity with null targetDate by default`() {
        val entity = TodoListEntity("1", "Groceries")
        assertNull(entity.targetDate)
    }

    @Test
    fun `should create entity with given targetDate`() {
        val entity = TodoListEntity("1", "Groceries", 0, 20000L)
        assertEquals(20000L, entity.targetDate)
    }

    @Test
    fun `should be equal when targetDate is the same`() {
        assertEquals(
            TodoListEntity("1", "Groceries", 0, 20000L),
            TodoListEntity("1", "Groceries", 0, 20000L)
        )
    }

    @Test
    fun `should not be equal when targetDate differs`() {
        assertNotEquals(
            TodoListEntity("1", "Groceries", 0, 20000L),
            TodoListEntity("1", "Groceries", 0, 20001L)
        )
    }

    @Test
    fun `should not be equal when one has targetDate and other has null`() {
        assertNotEquals(
            TodoListEntity("1", "Groceries", 0, 20000L),
            TodoListEntity("1", "Groceries", 0, null)
        )
    }

    @Test
    fun `should copy entity with updated targetDate`() {
        val original = TodoListEntity("1", "Groceries", 0, null)
        val copy = original.copy(targetDate = 20000L)
        assertEquals("1", copy.id)
        assertEquals("Groceries", copy.name)
        assertEquals(20000L, copy.targetDate)
    }

    @Test
    fun `should copy entity and clear targetDate`() {
        val original = TodoListEntity("1", "Groceries", 0, 20000L)
        val copy = original.copy(targetDate = null)
        assertNull(copy.targetDate)
    }
}
