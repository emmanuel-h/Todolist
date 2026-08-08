package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class InMemoryTodoListRepositoryTargetDateTest {

    private lateinit var repository: InMemoryTodoListRepository

    @Before
    fun setUp() {
        repository = InMemoryTodoListRepository()
    }

    @Test
    fun `should update targetDate when updateTargetDate is called with existing id`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))

        repository.updateTargetDate("1", targetDate)

        assertEquals(targetDate, repository.getAll().first().targetDate)
    }

    @Test
    fun `should clear targetDate when updateTargetDate is called with null`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries", targetDate = targetDate))

        repository.updateTargetDate("1", null)

        assertNull(repository.getAll().first().targetDate)
    }

    @Test
    fun `should preserve id after targetDate update`() {
        repository.add(TodoList("1", "Groceries"))

        repository.updateTargetDate("1", LocalDate.of(2027, 6, 22))

        assertEquals("1", repository.getAll().first().id)
    }

    @Test
    fun `should preserve name after targetDate update`() {
        repository.add(TodoList("1", "Groceries"))

        repository.updateTargetDate("1", LocalDate.of(2027, 6, 22))

        assertEquals("Groceries", repository.getAll().first().name)
    }

    @Test
    fun `should only update the targeted list targetDate when multiple lists exist`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Work tasks"))

        repository.updateTargetDate("1", targetDate)

        assertEquals(targetDate, repository.getAll()[0].targetDate)
        assertNull(repository.getAll()[1].targetDate)
    }

    @Test
    fun `should do nothing when updateTargetDate is called with a non-existent id`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))

        repository.updateTargetDate("nonexistent", targetDate)

        assertNull(repository.getAll().first().targetDate)
    }

    @Test
    fun `should store list with target date when add is called with targetDate`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        repository.add(TodoList("1", "Groceries", targetDate = targetDate))

        assertEquals(targetDate, repository.getAll().first().targetDate)
    }

    @Test
    fun `should update second list targetDate when second list is targeted`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Work tasks"))

        repository.updateTargetDate("2", targetDate)

        assertNull(repository.getAll()[0].targetDate)
        assertEquals(targetDate, repository.getAll()[1].targetDate)
    }
}
