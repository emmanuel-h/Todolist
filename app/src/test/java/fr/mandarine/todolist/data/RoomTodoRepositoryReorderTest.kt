package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    fun `should lay the named items back down in the order they were named`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "First", "list-1", position = 0),
            TodoItemEntity("2", "Second", "list-1", position = 1),
            TodoItemEntity("3", "Third", "list-1", position = 2)
        )

        repository.reorder("list-1", listOf("2", "3", "1"))

        verify { dao.updatePositions(listOf("2", "3", "1")) }
    }

    @Test
    fun `should lay an item named first back down at the top`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "First", "list-1", position = 0),
            TodoItemEntity("2", "Second", "list-1", position = 1),
            TodoItemEntity("3", "Third", "list-1", position = 2)
        )

        repository.reorder("list-1", listOf("3", "1", "2"))

        verify { dao.updatePositions(listOf("3", "1", "2")) }
    }

    @Test
    fun `should never name a completed item in the order it writes`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "Active A", "list-1", completed = false, position = 0),
            TodoItemEntity("2", "Active B", "list-1", completed = false, position = 1),
            TodoItemEntity("3", "Done", "list-1", completed = true, completedAt = 1000L, position = 0)
        )

        repository.reorder("list-1", listOf("2", "1"))

        verify { dao.updatePositions(listOf("2", "1")) }
    }

    /**
     * The page hides a torn-off row for the length of its undo slip while the
     * repository still holds it, so the order it names is one row shorter than
     * the section. The unnamed row keeps the slot it had.
     */
    @Test
    fun `should leave an item the page did not name in the slot it already had`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("a", "A", "list-1", position = 0),
            TodoItemEntity("b", "B", "list-1", position = 1),
            TodoItemEntity("hidden", "Hidden", "list-1", position = 2),
            TodoItemEntity("c", "C", "list-1", position = 3)
        )

        repository.reorder("list-1", listOf("c", "a", "b"))

        verify { dao.updatePositions(listOf("c", "a", "hidden", "b")) }
    }

    @Test
    fun `should leave an item the page did not name in a slot between the named ones`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("a", "A", "list-1", position = 0),
            TodoItemEntity("hidden", "Hidden", "list-1", position = 1),
            TodoItemEntity("b", "B", "list-1", position = 2),
            TodoItemEntity("c", "C", "list-1", position = 3)
        )

        repository.reorder("list-1", listOf("b", "a", "c"))

        verify { dao.updatePositions(listOf("b", "hidden", "a", "c")) }
    }

    @Test
    fun `should do nothing when the order names nothing`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "First", "list-1", position = 0)
        )

        repository.reorder("list-1", emptyList())

        verify(exactly = 0) { dao.updatePositions(any()) }
    }

    @Test
    fun `should do nothing when the order names an item the section no longer holds`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "First", "list-1", position = 0),
            TodoItemEntity("2", "Second", "list-1", position = 1)
        )

        repository.reorder("list-1", listOf("1", "gone"))

        verify(exactly = 0) { dao.updatePositions(any()) }
    }

    @Test
    fun `should do nothing when the order names an item that is completed`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("1", "First", "list-1", completed = false, position = 0),
            TodoItemEntity("done", "Done", "list-1", completed = true, completedAt = 1L, position = 1)
        )

        repository.reorder("list-1", listOf("1", "done"))

        verify(exactly = 0) { dao.updatePositions(any()) }
    }

    @Test
    fun `should do nothing when the list is empty`() {
        every { dao.getAllByListId("list-empty") } returns emptyList()

        repository.reorder("list-empty", listOf("1"))

        verify(exactly = 0) { dao.updatePositions(any()) }
    }

    @Test
    fun `should sort by position before reordering when entities arrive out of order`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("3", "Third", "list-1", position = 2),
            TodoItemEntity("1", "First", "list-1", position = 0),
            TodoItemEntity("2", "Second", "list-1", position = 1)
        )

        repository.reorder("list-1", listOf("2", "3", "1"))

        verify { dao.updatePositions(listOf("2", "3", "1")) }
    }

    /**
     * The slots an unnamed row leaves behind are only in the right places if the
     * section was read in position order first. With every row named the two
     * orders agree by accident, so the sort has to be pinned by a section that
     * has a row the order does not name.
     */
    @Test
    fun `should sort by position before choosing slots when a row is unnamed`() {
        every { dao.getAllByListId("list-1") } returns listOf(
            TodoItemEntity("c", "C", "list-1", position = 3),
            TodoItemEntity("a", "A", "list-1", position = 0),
            TodoItemEntity("hidden", "Hidden", "list-1", position = 1),
            TodoItemEntity("b", "B", "list-1", position = 2)
        )

        repository.reorder("list-1", listOf("b", "a", "c"))

        verify { dao.updatePositions(listOf("b", "hidden", "a", "c")) }
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
