package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class InMemoryTodoListRepositoryDueDateTest {

    private lateinit var repository: InMemoryTodoListRepository

    @Before
    fun setUp() {
        repository = InMemoryTodoListRepository()
    }

    @Test
    fun `should update dueDate when updateDueDate is called with existing id`() {
        val dueDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))

        repository.updateDueDate("1", dueDate)

        assertEquals(dueDate, repository.getAll().first().dueDate)
    }

    @Test
    fun `should clear dueDate when updateDueDate is called with null`() {
        val dueDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries", dueDate = dueDate))

        repository.updateDueDate("1", null)

        assertNull(repository.getAll().first().dueDate)
    }

    @Test
    fun `should preserve id after dueDate update`() {
        repository.add(TodoList("1", "Groceries"))

        repository.updateDueDate("1", LocalDate.of(2027, 6, 22))

        assertEquals("1", repository.getAll().first().id)
    }

    @Test
    fun `should preserve name after dueDate update`() {
        repository.add(TodoList("1", "Groceries"))

        repository.updateDueDate("1", LocalDate.of(2027, 6, 22))

        assertEquals("Groceries", repository.getAll().first().name)
    }

    @Test
    fun `should only update the targeted list dueDate when multiple lists exist`() {
        val dueDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Work tasks"))

        repository.updateDueDate("1", dueDate)

        assertEquals(dueDate, repository.getAll()[0].dueDate)
        assertNull(repository.getAll()[1].dueDate)
    }

    @Test
    fun `should do nothing when updateDueDate is called with a non-existent id`() {
        val dueDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))

        repository.updateDueDate("nonexistent", dueDate)

        assertNull(repository.getAll().first().dueDate)
    }

    @Test
    fun `should store list with due date when add is called with dueDate`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        repository.add(TodoList("1", "Groceries", dueDate = dueDate))

        assertEquals(dueDate, repository.getAll().first().dueDate)
    }

    @Test
    fun `should update second list dueDate when second list is targeted`() {
        val dueDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Work tasks"))

        repository.updateDueDate("2", dueDate)

        assertNull(repository.getAll()[0].dueDate)
        assertEquals(dueDate, repository.getAll()[1].dueDate)
    }
}
