package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test

class RoomTodoRepositoryReorderTest {

    private lateinit var dao: TodoItemDao
    private lateinit var repository: RoomTodoRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoRepository(dao)
    }

    @Test
    fun `should update positions of active items when reorder is called moving item forward`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "First", "list-1", position = 0),
            TodoItemEntity("2", "Second", "list-1", position = 1),
            TodoItemEntity("3", "Third", "list-1", position = 2)
        )

        repository.reorder("list-1", 0, 2)

        verify { dao.updatePosition("2", 0) }
        verify { dao.updatePosition("3", 1) }
        verify { dao.updatePosition("1", 2) }
    }

    @Test
    fun `should update positions of active items when reorder is called moving item backward`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "First", "list-1", position = 0),
            TodoItemEntity("2", "Second", "list-1", position = 1),
            TodoItemEntity("3", "Third", "list-1", position = 2)
        )

        repository.reorder("list-1", 2, 0)

        verify { dao.updatePosition("3", 0) }
        verify { dao.updatePosition("1", 1) }
        verify { dao.updatePosition("2", 2) }
    }

    @Test
    fun `should not call updatePosition when fromIndex equals toIndex`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "First", "list-1", position = 0),
            TodoItemEntity("2", "Second", "list-1", position = 1)
        )

        repository.reorder("list-1", 0, 0)

        verify(exactly = 0) { dao.updatePosition(any(), any()) }
    }

    @Test
    fun `should only update active items positions when completed items are present`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "Active A", "list-1", completed = false, position = 0),
            TodoItemEntity("2", "Active B", "list-1", completed = false, position = 1),
            TodoItemEntity("3", "Done", "list-1", completed = true, completedAt = 1000L, position = 0)
        )

        repository.reorder("list-1", 0, 1)

        verify { dao.updatePosition("2", 0) }
        verify { dao.updatePosition("1", 1) }
        verify(exactly = 0) { dao.updatePosition("3", any()) }
    }

    @Test
    fun `should do nothing when reorder is called and dao returns empty list`() {
        every { dao.getAllByListId("list-empty") } returns emptyList()

        repository.reorder("list-empty", 0, 1)

        verify(exactly = 0) { dao.updatePosition(any(), any()) }
    }

    @Test
    fun `should sort by position field before reordering when entities are returned out of position order`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("3", "Third", "list-1", position = 2),
            TodoItemEntity("1", "First", "list-1", position = 0),
            TodoItemEntity("2", "Second", "list-1", position = 1)
        )

        repository.reorder("list-1", 0, 2)

        verify { dao.updatePosition("2", 0) }
        verify { dao.updatePosition("3", 1) }
        verify { dao.updatePosition("1", 2) }
    }

    @Test
    fun `should insert position zero for new item via dao when add is called`() {
        repository.add(TodoItem("1", "Item 1", "list-1"))

        verify { dao.insert(TodoItemEntity("1", "Item 1", "list-1", completed = false, completedAt = null, position = 0)) }
    }

    @Test
    fun `should map position from entity to domain model`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "Item 1", "list-1", position = 5)
        )

        val result = repository.getAllByListId("list-1")

        org.junit.Assert.assertEquals(5, result.first().position)
    }
}
