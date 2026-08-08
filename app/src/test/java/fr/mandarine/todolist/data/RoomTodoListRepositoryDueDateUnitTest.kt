package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RoomTodoListRepositoryDueDateUnitTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should call dao updateDueDate with epoch day when updateDueDate is called with a date`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        repository.updateDueDate("1", dueDate)

        verify { dao.updateDueDate("1", dueDate.toEpochDay()) }
    }

    @Test
    fun `should call dao updateDueDate with null when updateDueDate is called with null`() {
        repository.updateDueDate("1", null)

        verify { dao.updateDueDate("1", null) }
    }

    @Test
    fun `should call dao updateDueDate with another id and date`() {
        val dueDate = LocalDate.of(2026, 1, 15)

        repository.updateDueDate("list-42", dueDate)

        verify { dao.updateDueDate("list-42", dueDate.toEpochDay()) }
    }

    @Test
    fun `should map dueDate epoch day from entity to LocalDate in domain model`() {
        val dueDate = LocalDate.of(2027, 6, 22)
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", 0, null, dueDate.toEpochDay()))

        val result = repository.getAll()

        assertEquals(dueDate, result[0].dueDate)
    }

    @Test
    fun `should map null dueDate from entity to null in domain model`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", 0, null, null))

        val result = repository.getAll()

        assertNull(result[0].dueDate)
    }

    @Test
    fun `should insert entity with dueDate epoch day when add is called with dueDate`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        repository.add(TodoList("1", "Groceries", dueDate = dueDate))

        verify { dao.insert(TodoListEntity("1", "Groceries", 0, null, dueDate.toEpochDay())) }
    }

    @Test
    fun `should insert entity with null dueDate when add is called without dueDate`() {
        repository.add(TodoList("1", "Groceries"))

        verify { dao.insert(TodoListEntity("1", "Groceries", 0, null, null)) }
    }
}
