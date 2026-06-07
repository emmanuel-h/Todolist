package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `should map completed true from entity to domain model`() {
        every { dao.getAllByListId("list-1") } returns listOf(TodoItemEntity("1", "Item 1", "list-1", completed = true))
        val result = repository.getAllByListId("list-1")
        assertTrue(result.first().isCompleted)
    }

    @Test
    fun `should map completed false from entity to domain model`() {
        every { dao.getAllByListId("list-1") } returns listOf(TodoItemEntity("1", "Item 1", "list-1", completed = false))
        val result = repository.getAllByListId("list-1")
        assertFalse(result.first().isCompleted)
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
        verify { dao.insert(TodoItemEntity("1", "Item 1", "list-1", completed = false)) }
    }

    @Test
    fun `should insert entity with completed true via dao when add is called with completed item`() {
        every { dao.insert(any()) } returns Unit
        repository.add(TodoItem("1", "Item 1", "list-1", isCompleted = true))
        verify { dao.insert(TodoItemEntity("1", "Item 1", "list-1", completed = true)) }
    }

    @Test
    fun `should delete all items for list via dao when deleteAllByListId is called`() {
        every { dao.deleteAllByListId("list-1") } returns Unit
        repository.deleteAllByListId("list-1")
        verify { dao.deleteAllByListId("list-1") }
    }

    @Test
    fun `should call updateCompleted via dao when toggle is called`() {
        every { dao.updateCompleted("item-1", true) } returns Unit
        every { dao.getById("item-1") } returns TodoItemEntity("item-1", "Item 1", "list-1", completed = false)
        repository.toggle("item-1")
        verify { dao.updateCompleted("item-1", true) }
    }

    @Test
    fun `should toggle to false when item is currently completed`() {
        every { dao.updateCompleted("item-1", false) } returns Unit
        every { dao.getById("item-1") } returns TodoItemEntity("item-1", "Item 1", "list-1", completed = true)
        repository.toggle("item-1")
        verify { dao.updateCompleted("item-1", false) }
    }

    @Test
    fun `should do nothing when toggle is called for non-existent id`() {
        every { dao.getById("non-existent") } returns null
        repository.toggle("non-existent")
        verify(exactly = 0) { dao.updateCompleted(any(), any()) }
    }
}
