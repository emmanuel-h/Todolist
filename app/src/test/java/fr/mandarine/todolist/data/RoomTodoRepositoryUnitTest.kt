package fr.mandarine.todolist.data

import fr.mandarine.todolist.FakeClock
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.TodoCounts
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
    private val clock = FakeClock(nowMillis = 1000L)

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
    fun `should complete item keeping its position via dao when toggle is called on inactive item`() {
        clock.nowMillis = 7000L
        every { dao.updateCompletedAndPosition("item-1", true, 7000L, 3) } returns Unit
        every { dao.getById("item-1") } returns TodoItemEntity("item-1", "Item 1", "list-1", completed = false, position = 3)
        repository.toggle("item-1")
        verify { dao.updateCompletedAndPosition("item-1", true, 7000L, 3) }
    }

    @Test
    fun `should reopen item at end of active items via dao when toggle is called on completed item`() {
        every { dao.updateCompletedAndPosition("item-1", false, null, 2) } returns Unit
        every { dao.getById("item-1") } returns TodoItemEntity("item-1", "Item 1", "list-1", completed = true, completedAt = 1000L)
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("a", "Active A", "list-1", completed = false, position = 0),
            TodoItemEntity("b", "Active B", "list-1", completed = false, position = 1),
            TodoItemEntity("item-1", "Item 1", "list-1", completed = true, completedAt = 1000L)
        )
        repository.toggle("item-1")
        verify { dao.updateCompletedAndPosition("item-1", false, null, 2) }
    }

    @Test
    fun `should reopen item at position zero when it is the only item in the list`() {
        every { dao.updateCompletedAndPosition("item-1", false, null, 0) } returns Unit
        every { dao.getById("item-1") } returns TodoItemEntity("item-1", "Item 1", "list-1", completed = true, completedAt = 1000L)
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("item-1", "Item 1", "list-1", completed = true, completedAt = 1000L)
        )
        repository.toggle("item-1")
        verify { dao.updateCompletedAndPosition("item-1", false, null, 0) }
    }

    @Test
    fun `should reopen item at one past the highest active position when gaps exist in active positions`() {
        every { dao.updateCompletedAndPosition("item-1", false, null, 3) } returns Unit
        every { dao.getById("item-1") } returns TodoItemEntity("item-1", "Item 1", "list-1", completed = true, completedAt = 1000L)
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("b", "Active B", "list-1", completed = false, position = 1),
            TodoItemEntity("c", "Active C", "list-1", completed = false, position = 2),
            TodoItemEntity("item-1", "Item 1", "list-1", completed = true, completedAt = 1000L)
        )
        repository.toggle("item-1")
        verify { dao.updateCompletedAndPosition("item-1", false, null, 3) }
    }


    @Test
    fun `should do nothing when toggle is called for non-existent id`() {
        every { dao.getById("non-existent") } returns null
        repository.toggle("non-existent")
        verify(exactly = 0) { dao.updateCompletedAndPosition(any(), any(), any(), any()) }
    }

    @Test
    fun `should use system clock when no clock is provided`() {
        val repoWithDefaultClock = RoomTodoRepository(dao)
        every { dao.getAllByListId("list-1") } returns emptyList()
        assertTrue(repoWithDefaultClock.getAllByListId("list-1").isEmpty())
    }

    @Test
    fun `should call dao delete when delete is called`() {
        every { dao.deleteById("item-1") } returns Unit
        repository.delete("item-1")
        verify { dao.deleteById("item-1") }
    }

    @Test
    fun `should call dao updateTitle when updateTitle is called`() {
        every { dao.updateTitle("item-1", "New title") } returns Unit
        repository.updateTitle("item-1", "New title")
        verify { dao.updateTitle("item-1", "New title") }
    }

    @Test
    fun `should map dao count rows to domain counts`() {
        every { dao.countsByList() } returns listOf(TodoCountsRow("list-1", 2, 3))
        assertEquals(listOf(TodoCounts("list-1", 2, 3)), repository.countsByList())
    }
}
