package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomTodoListRepositoryUnitTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should return empty list when dao returns no entities`() {
        every { dao.getAll() } returns emptyList()
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `should map entity to domain model when dao returns one entity`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries"))
        assertEquals(listOf(TodoList("1", "Groceries")), repository.getAll())
    }

    @Test
    fun `should map all entities to domain models when dao returns multiple entities`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "Groceries"),
            TodoListEntity("2", "Home")
        )
        val result = repository.getAll()
        assertEquals(2, result.size)
        assertEquals(TodoList("1", "Groceries"), result[0])
        assertEquals(TodoList("2", "Home"), result[1])
    }

    @Test
    fun `should insert entity via dao when add is called`() {
        every { dao.insert(any()) } returns Unit
        repository.add(TodoList("1", "Groceries"))
        verify { dao.insert(TodoListEntity("1", "Groceries")) }
    }

    @Test
    fun `should delete entity via dao when delete is called`() {
        every { dao.deleteById("1") } returns Unit
        repository.delete("1")
        verify { dao.deleteById("1") }
    }
}
