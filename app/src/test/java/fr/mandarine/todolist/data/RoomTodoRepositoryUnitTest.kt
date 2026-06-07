package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomTodoRepositoryUnitTest {

    private lateinit var dao: TodoItemDao
    private lateinit var repository: RoomTodoRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = RoomTodoRepository(dao)
    }

    @Test
    fun `should return empty list when dao returns no entities for given list`() {
        every { dao.getAllByListId("list-1") } returns emptyList()
        assertTrue(repository.getAllByListId("list-1").isEmpty())
    }

    @Test
    fun `should map entity to domain model when dao returns one entity`() {
        every { dao.getAllByListId("list-1") } returns listOf(TodoItemEntity("1", "Item 1", "list-1"))
        assertEquals(listOf(TodoItem("1", "Item 1", "list-1")), repository.getAllByListId("list-1"))
    }

    @Test
    fun `should map all entities to domain models when dao returns multiple entities`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "Item 1", "list-1"),
            TodoItemEntity("2", "Item 2", "list-1")
        )
        val result = repository.getAllByListId("list-1")
        assertEquals(2, result.size)
        assertEquals(TodoItem("1", "Item 1", "list-1"), result[0])
        assertEquals(TodoItem("2", "Item 2", "list-1"), result[1])
    }

    @Test
    fun `should insert entity via dao when add is called`() {
        every { dao.insert(any()) } returns Unit
        repository.add(TodoItem("1", "Item 1", "list-1"))
        verify { dao.insert(TodoItemEntity("1", "Item 1", "list-1")) }
    }

    @Test
    fun `should delete all items for list via dao when deleteAllByListId is called`() {
        every { dao.deleteAllByListId("list-1") } returns Unit
        repository.deleteAllByListId("list-1")
        verify { dao.deleteAllByListId("list-1") }
    }
}
