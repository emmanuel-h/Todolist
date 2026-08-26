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
    fun `should lay the named lists back down in the order they were named`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "First", position = 0),
            TodoListEntity("2", "Second", position = 1),
            TodoListEntity("3", "Third", position = 2)
        )

        repository.reorder(listOf("2", "3", "1"))

        verify { dao.updatePositions(listOf("2", "3", "1")) }
    }

    @Test
    fun `should lay a list named first back down at the top`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "First", position = 0),
            TodoListEntity("2", "Second", position = 1),
            TodoListEntity("3", "Third", position = 2)
        )

        repository.reorder(listOf("3", "1", "2"))

        verify { dao.updatePositions(listOf("3", "1", "2")) }
    }

    /**
     * The page hides a torn-off row for the length of its undo slip while the
     * repository still holds it, so the order it names is one row shorter than
     * the page. The unnamed row keeps the slot it had rather than being
     * renumbered around.
     */
    @Test
    fun `should leave a list the page did not name in the slot it already had`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("a", "A", position = 0),
            TodoListEntity("b", "B", position = 1),
            TodoListEntity("hidden", "Hidden", position = 2),
            TodoListEntity("c", "C", position = 3)
        )

        repository.reorder(listOf("c", "a", "b"))

        verify { dao.updatePositions(listOf("c", "a", "hidden", "b")) }
    }

    @Test
    fun `should leave a list the page did not name in a slot between the named ones`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("a", "A", position = 0),
            TodoListEntity("hidden", "Hidden", position = 1),
            TodoListEntity("b", "B", position = 2),
            TodoListEntity("c", "C", position = 3)
        )

        repository.reorder(listOf("b", "a", "c"))

        verify { dao.updatePositions(listOf("b", "hidden", "a", "c")) }
    }

    @Test
    fun `should do nothing when the order names nothing`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "First", position = 0))

        repository.reorder(emptyList())

        verify(exactly = 0) { dao.updatePositions(any()) }
    }

    @Test
    fun `should do nothing when the order names a list the page no longer holds`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("1", "First", position = 0),
            TodoListEntity("2", "Second", position = 1)
        )

        repository.reorder(listOf("1", "gone"))

        verify(exactly = 0) { dao.updatePositions(any()) }
    }

    @Test
    fun `should do nothing when the page is empty`() {
        every { dao.getAll() } returns emptyList()

        repository.reorder(listOf("1"))

        verify(exactly = 0) { dao.updatePositions(any()) }
    }

    @Test
    fun `should sort by position before reordering when entities arrive out of order`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("3", "Third", position = 2),
            TodoListEntity("1", "First", position = 0),
            TodoListEntity("2", "Second", position = 1)
        )

        repository.reorder(listOf("2", "3", "1"))

        verify { dao.updatePositions(listOf("2", "3", "1")) }
    }

    /**
     * The slots an unnamed row leaves behind are only in the right places if the
     * page was read in position order first. With every row named the two orders
     * agree by accident, so the sort has to be pinned by a page that has a row
     * the order does not name.
     */
    @Test
    fun `should sort by position before choosing slots when a row is unnamed`() {
        every { dao.getAll() } returns listOf(
            TodoListEntity("c", "C", position = 3),
            TodoListEntity("a", "A", position = 0),
            TodoListEntity("hidden", "Hidden", position = 1),
            TodoListEntity("b", "B", position = 2)
        )

        repository.reorder(listOf("b", "a", "c"))

        verify { dao.updatePositions(listOf("b", "hidden", "a", "c")) }
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
