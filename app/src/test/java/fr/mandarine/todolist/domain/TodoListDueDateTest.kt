package fr.mandarine.todolist.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TodoListDueDateTest {

    @Test
    fun `should create list with given due date`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        val list = TodoList("1", "Groceries", dueDate = dueDate)

        assertEquals(dueDate, list.dueDate)
    }

    @Test
    fun `should create list with null due date by default`() {
        val list = TodoList("1", "Groceries")

        assertNull(list.dueDate)
    }

    @Test
    fun `should create list with null target date and non-null due date`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        val list = TodoList("1", "Groceries", targetDate = null, dueDate = dueDate)

        assertNull(list.targetDate)
        assertEquals(dueDate, list.dueDate)
    }

    @Test
    fun `should create list with non-null target date and null due date`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        val list = TodoList("1", "Groceries", targetDate = targetDate, dueDate = null)

        assertEquals(targetDate, list.targetDate)
        assertNull(list.dueDate)
    }

    @Test
    fun `should throw IllegalArgumentException when both target date and due date are set`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        val dueDate = LocalDate.of(2027, 7, 1)

        assertThrows(IllegalArgumentException::class.java) {
            TodoList("1", "Groceries", targetDate = targetDate, dueDate = dueDate)
        }
    }

    @Test
    fun `should be equal when due date is the same`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        assertEquals(
            TodoList("1", "Groceries", dueDate = dueDate),
            TodoList("1", "Groceries", dueDate = dueDate)
        )
    }

    @Test
    fun `should not be equal when due date differs`() {
        assertNotEquals(
            TodoList("1", "Groceries", dueDate = LocalDate.of(2027, 6, 22)),
            TodoList("1", "Groceries", dueDate = LocalDate.of(2027, 6, 23))
        )
    }

    @Test
    fun `should not be equal when one has due date and other has null`() {
        assertNotEquals(
            TodoList("1", "Groceries", dueDate = LocalDate.of(2027, 6, 22)),
            TodoList("1", "Groceries", dueDate = null)
        )
    }

    @Test
    fun `should copy list with updated due date`() {
        val original = TodoList("1", "Groceries")
        val dueDate = LocalDate.of(2027, 6, 22)

        val copy = original.copy(dueDate = dueDate)

        assertEquals(dueDate, copy.dueDate)
        assertEquals("1", copy.id)
        assertEquals("Groceries", copy.name)
    }

    @Test
    fun `should copy list and clear due date`() {
        val dueDate = LocalDate.of(2027, 6, 22)
        val original = TodoList("1", "Groceries", dueDate = dueDate)

        val copy = original.copy(dueDate = null)

        assertNull(copy.dueDate)
    }

    @Test
    fun `should throw when copying with both target date and due date set`() {
        val original = TodoList("1", "Groceries", dueDate = LocalDate.of(2027, 6, 22))

        assertThrows(IllegalArgumentException::class.java) {
            original.copy(targetDate = LocalDate.of(2027, 5, 1))
        }
    }
}
