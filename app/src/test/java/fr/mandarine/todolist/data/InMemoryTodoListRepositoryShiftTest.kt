package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryTodoListRepositoryShiftTest {

    private lateinit var repository: InMemoryTodoListRepository

    @Before
    fun setUp() {
        repository = InMemoryTodoListRepository()
    }

    @Test
    fun `should do nothing when shiftAllPositionsUp is called on empty repository`() {
        repository.shiftAllPositionsUp()
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `should increment position of single list by one when shiftAllPositionsUp is called`() {
        repository.add(TodoList("1", "Groceries", position = 0))
        repository.shiftAllPositionsUp()
        assertEquals(1, repository.getAll().first().position)
    }

    @Test
    fun `should increment positions of all lists by one when shiftAllPositionsUp is called`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))
        repository.add(TodoList("3", "Third", position = 2))
        repository.shiftAllPositionsUp()
        val lists = repository.getAll()
        assertEquals(1, lists[0].position)
        assertEquals(2, lists[1].position)
        assertEquals(3, lists[2].position)
    }

    @Test
    fun `should preserve list order after shiftAllPositionsUp`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))
        repository.shiftAllPositionsUp()
        val lists = repository.getAll()
        assertEquals("First", lists[0].name)
        assertEquals("Second", lists[1].name)
    }

    @Test
    fun `should allow new list at position zero to appear first after shiftAllPositionsUp and add`() {
        repository.add(TodoList("1", "Old", position = 0))
        repository.shiftAllPositionsUp()
        repository.add(TodoList("2", "New", position = 0))
        val lists = repository.getAll()
        assertEquals("New", lists[0].name)
        assertEquals("Old", lists[1].name)
    }
}
