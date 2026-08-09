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

class RoomTodoListRepositoryTargetDateUnitTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should call dao update with epoch day when update is called with a date`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        repository.update("1", "Groceries", targetDate, null)

        verify { dao.update("1", "Groceries", targetDate.toEpochDay(), null) }
    }

    @Test
    fun `should call dao update with null when update is called with null`() {
        repository.update("1", "Groceries", null, null)

        verify { dao.update("1", "Groceries", null, null) }
    }

    @Test
    fun `should call dao update with another id and date`() {
        val targetDate = LocalDate.of(2026, 1, 15)

        repository.update("list-42", "Work", targetDate, null)

        verify { dao.update("list-42", "Work", targetDate.toEpochDay(), null) }
    }

    @Test
    fun `should map targetDate epoch day from entity to LocalDate in domain model`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", 0, targetDate.toEpochDay()))

        val result = repository.getAll()

        assertEquals(targetDate, result[0].targetDate)
    }

    @Test
    fun `should map null targetDate from entity to null in domain model`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", 0, null))

        val result = repository.getAll()

        assertNull(result[0].targetDate)
    }

    @Test
    fun `should insert entity with targetDate epoch day when add is called with targetDate`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        repository.add(TodoList("1", "Groceries", 0, targetDate))

        verify { dao.insert(TodoListEntity("1", "Groceries", 0, targetDate.toEpochDay())) }
    }

    @Test
    fun `should insert entity with null targetDate when add is called without targetDate`() {
        repository.add(TodoList("1", "Groceries"))

        verify { dao.insert(TodoListEntity("1", "Groceries", 0, null)) }
    }
}
