package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.TodoItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomTodoRepositoryUnitTest {

    private lateinit var dao: TodoItemDao
    private lateinit var repository: RoomTodoRepository
    private var clockTime = 1000L
    private val clock = Clock { clockTime }

    @Before
    fun setUp() {
        dao = mockk()
        repository = RoomTodoRepository(dao, clock)
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
    fun `should map completedAt from entity to domain model`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "Item 1", "list-1", completed = true, completedAt = 5000L)
        )
        val result = repository.getAllByListId("list-1")
        assertEquals(5000L, result.first().completedAt)
    }

    @Test
    fun `should map completedAt null from entity to domain model`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "Item 1", "list-1", completed = false, completedAt = null)
        )
        val result = repository.getAllByListId("list-1")
        assertNull(result.first().completedAt)
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
        verify { dao.insert(TodoItemEntity("1", "Item 1", "list-1", completed = false, completedAt = null)) }
    }

    @Test
    fun `should insert entity with completed true via dao when add is called with completed item`() {
        every { dao.insert(any()) } returns Unit
        repository.add(TodoItem("1", "Item 1", "list-1", isCompleted = true, completedAt = 3000L))
        verify { dao.insert(TodoItemEntity("1", "Item 1", "list-1", completed = true, completedAt = 3000L)) }
    }

    @Test
    fun `should delete all items for list via dao when deleteAllByListId is called`() {
        every { dao.deleteAllByListId("list-1") } returns Unit
        repository.deleteAllByListId("list-1")
        verify { dao.deleteAllByListId("list-1") }
    }

    @Test
    fun `should call updateCompleted with true and clock time via dao when toggle is called on inactive item`() {
        clockTime = 7000L
        every { dao.updateCompleted("item-1", true, 7000L) } returns Unit
        every { dao.getById("item-1") } returns TodoItemEntity("item-1", "Item 1", "list-1", completed = false)
        repository.toggle("item-1")
        verify { dao.updateCompleted("item-1", true, 7000L) }
    }

    @Test
    fun `should call updateCompleted with false and null via dao when toggle is called on completed item`() {
        every { dao.updateCompleted("item-1", false, null) } returns Unit
        every { dao.getById("item-1") } returns TodoItemEntity("item-1", "Item 1", "list-1", completed = true, completedAt = 1000L)
        repository.toggle("item-1")
        verify { dao.updateCompleted("item-1", false, null) }
    }

    @Test
    fun `should do nothing when toggle is called for non-existent id`() {
        every { dao.getById("non-existent") } returns null
        repository.toggle("non-existent")
        verify(exactly = 0) { dao.updateCompleted(any(), any(), any()) }
    }

    @Test
    fun `should use system clock when no clock is provided`() {
        val repoWithDefaultClock = RoomTodoRepository(dao)
        every { dao.getAllByListId("list-1") } returns emptyList()
        assertTrue(repoWithDefaultClock.getAllByListId("list-1").isEmpty())
    }
}
