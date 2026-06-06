package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryTodoRepositoryTest {

    private lateinit var repository: InMemoryTodoRepository

    @Before
    fun setUp() {
        repository = InMemoryTodoRepository()
    }

    @Test
    fun `should return empty list when no items added`() {
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `should return added item when one item is added`() {
        val item = TodoItem("1", "Item 1")
        repository.add(item)
        assertEquals(listOf(item), repository.getAll())
    }

    @Test
    fun `should return all items when multiple items added`() {
        val item1 = TodoItem("1", "Item 1")
        val item2 = TodoItem("2", "Item 2")
        repository.add(item1)
        repository.add(item2)
        assertEquals(listOf(item1, item2), repository.getAll())
    }

    @Test
    fun `should return snapshot independent of future modifications`() {
        val item1 = TodoItem("1", "Item 1")
        repository.add(item1)
        val snapshot = repository.getAll()
        repository.add(TodoItem("2", "Item 2"))
        assertEquals(1, snapshot.size)
    }
}
