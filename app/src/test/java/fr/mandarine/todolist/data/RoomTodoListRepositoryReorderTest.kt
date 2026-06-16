package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RoomTodoListRepositoryReorderTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should update positions of lists when reorder is called moving item forward`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "First", position = 0),
            TodoListEntity("2", "Second", position = 1),
            TodoListEntity("3", "Third", position = 2)
        )

        repository.reorder(0, 2)

        verify { dao.updatePosition("2", 0) }
        verify { dao.updatePosition("3", 1) }
        verify { dao.updatePosition("1", 2) }
    }

    @Test
    fun `should update positions of lists when reorder is called moving item backward`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "First", position = 0),
            TodoListEntity("2", "Second", position = 1),
            TodoListEntity("3", "Third", position = 2)
        )

        repository.reorder(2, 0)

        verify { dao.updatePosition("3", 0) }
        verify { dao.updatePosition("1", 1) }
        verify { dao.updatePosition("2", 2) }
    }

    @Test
    fun `should not call updatePosition when fromIndex equals toIndex`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "First", position = 0),
            TodoListEntity("2", "Second", position = 1)
        )

        repository.reorder(0, 0)

        verify(exactly = 0) { dao.updatePosition(any(), any()) }
    }

    @Test
    fun `should do nothing when reorder is called and dao returns empty list`() {
        every { dao.getAll() } returns emptyList()

        repository.reorder(0, 1)

        verify(exactly = 0) { dao.updatePosition(any(), any()) }
    }

    @Test
    fun `should sort by position field before reordering when entities are returned out of position order`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("3", "Third", position = 2),
            TodoListEntity("1", "First", position = 0),
            TodoListEntity("2", "Second", position = 1)
        )

        repository.reorder(0, 2)

        verify { dao.updatePosition("2", 0) }
        verify { dao.updatePosition("3", 1) }
        verify { dao.updatePosition("1", 2) }
    }

    @Test
    fun `should insert position zero for new list via dao when add is called`() {
        repository.add(TodoList("1", "Groceries"))

        verify { dao.insert(TodoListEntity("1", "Groceries", position = 0)) }
    }

    @Test
    fun `should insert position from domain model when add is called with non-zero position`() {
        repository.add(TodoList("1", "Groceries", position = 5))

        verify { dao.insert(TodoListEntity("1", "Groceries", position = 5)) }
    }

    @Test
    fun `should map position from entity to domain model`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "Groceries", position = 5)
        )

        val result = repository.getAll()

        assertEquals(5, result.first().position)
    }

    @Test
    fun `should return lists ordered by position from dao`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "Groceries", position = 0),
            TodoListEntity("2", "Work", position = 1)
        )

        val result = repository.getAll()

        assertEquals(listOf(TodoList("1", "Groceries", position = 0), TodoList("2", "Work", position = 1)), result)
    }
}
