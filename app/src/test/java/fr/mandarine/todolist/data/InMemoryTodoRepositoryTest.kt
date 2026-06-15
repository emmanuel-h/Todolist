package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryTodoRepositoryTest {

    private lateinit var repository: InMemoryTodoRepository
    private var clockTime = 1000L
    private val clock = Clock { clockTime }

    @Before
    fun setUp() {
        clockTime = 1000L
        repository = InMemoryTodoRepository(clock)
    }

    @Test
    fun `should return empty list when no items added for given list`() {
        assertTrue(repository.getAllByListId("list-1").isEmpty())
    }

    @Test
    fun `should return item when one item is added to that list`() {
        val item = TodoItem("1", "Item 1", "list-1")
        repository.add(item)
        assertEquals(listOf(item), repository.getAllByListId("list-1"))
    }

    @Test
    fun `should return only items belonging to the requested list`() {
        val item1 = TodoItem("1", "Item 1", "list-1")
        val item2 = TodoItem("2", "Item 2", "list-2")
        repository.add(item1)
        repository.add(item2)
        assertEquals(listOf(item1), repository.getAllByListId("list-1"))
        assertEquals(listOf(item2), repository.getAllByListId("list-2"))
    }

    @Test
    fun `should return all items for a list when multiple items belong to it`() {
        val item1 = TodoItem("1", "Item 1", "list-1")
        val item2 = TodoItem("2", "Item 2", "list-1")
        repository.add(item1)
        repository.add(item2)
        assertEquals(listOf(item1, item2), repository.getAllByListId("list-1"))
    }

    @Test
    fun `should return snapshot independent of future modifications`() {
        val item1 = TodoItem("1", "Item 1", "list-1")
        repository.add(item1)
        val snapshot = repository.getAllByListId("list-1")
        repository.add(TodoItem("2", "Item 2", "list-1"))
        assertEquals(1, snapshot.size)
    }

    @Test
    fun `should remove all items for given list when deleteAllByListId is called`() {
        repository.add(TodoItem("1", "Item 1", "list-1"))
        repository.add(TodoItem("2", "Item 2", "list-1"))
        repository.deleteAllByListId("list-1")
        assertTrue(repository.getAllByListId("list-1").isEmpty())
    }

    @Test
    fun `should not remove items of other lists when deleteAllByListId is called`() {
        repository.add(TodoItem("1", "Item 1", "list-1"))
        repository.add(TodoItem("2", "Item 2", "list-2"))
        repository.deleteAllByListId("list-1")
        assertEquals(1, repository.getAllByListId("list-2").size)
    }

    @Test
    fun `should do nothing when deleteAllByListId is called for a list with no items`() {
        repository.deleteAllByListId("list-nonexistent")
        assertTrue(repository.getAllByListId("list-nonexistent").isEmpty())
    }

    @Test
    fun `should mark item as completed when toggle is called on an incomplete item`() {
        val item = TodoItem("1", "Item 1", "list-1")
        repository.add(item)
        repository.toggle("1")
        val result = repository.getAllByListId("list-1").first()
        assertTrue(result.isCompleted)
    }

    @Test
    fun `should set completedAt to clock time when toggle is called on an incomplete item`() {
        clockTime = 5000L
        val item = TodoItem("1", "Item 1", "list-1")
        repository.add(item)
        repository.toggle("1")
        val result = repository.getAllByListId("list-1").first()
        assertEquals(5000L, result.completedAt)
    }

    @Test
    fun `should mark item as incomplete when toggle is called on a completed item`() {
        val item = TodoItem("1", "Item 1", "list-1", isCompleted = true, completedAt = 1000L)
        repository.add(item)
        repository.toggle("1")
        val result = repository.getAllByListId("list-1").first()
        assertFalse(result.isCompleted)
    }

    @Test
    fun `should clear completedAt when toggle is called on a completed item`() {
        val item = TodoItem("1", "Item 1", "list-1", isCompleted = true, completedAt = 1000L)
        repository.add(item)
        repository.toggle("1")
        val result = repository.getAllByListId("list-1").first()
        assertNull(result.completedAt)
    }

    @Test
    fun `should only toggle the item with the matching id`() {
        repository.add(TodoItem("1", "Item 1", "list-1"))
        repository.add(TodoItem("2", "Item 2", "list-1"))
        repository.toggle("1")
        val items = repository.getAllByListId("list-1")
        assertTrue(items.first { it.id == "1" }.isCompleted)
        assertFalse(items.first { it.id == "2" }.isCompleted)
    }

    @Test
    fun `should do nothing when toggle is called for a non-existent id`() {
        repository.add(TodoItem("1", "Item 1", "list-1"))
        repository.toggle("non-existent")
        assertFalse(repository.getAllByListId("list-1").first().isCompleted)
    }

    @Test
    fun `should toggle the second item in a two-item list when toggling by its id`() {
        repository.add(TodoItem("1", "Item 1", "list-1"))
        repository.add(TodoItem("2", "Item 2", "list-1"))
        repository.toggle("2")
        val items = repository.getAllByListId("list-1")
        assertFalse(items.first { it.id == "1" }.isCompleted)
        assertTrue(items.first { it.id == "2" }.isCompleted)
    }

    @Test
    fun `should assign different completedAt values when two items are toggled at different times`() {
        repository.add(TodoItem("1", "Item 1", "list-1"))
        repository.add(TodoItem("2", "Item 2", "list-1"))
        clockTime = 1000L
        repository.toggle("1")
        clockTime = 2000L
        repository.toggle("2")
        val items = repository.getAllByListId("list-1")
        assertEquals(1000L, items.first { it.id == "1" }.completedAt)
        assertEquals(2000L, items.first { it.id == "2" }.completedAt)
    }

    @Test
    fun `should use system clock when no clock is provided`() {
        val repoWithDefaultClock = InMemoryTodoRepository()
        assertTrue(repoWithDefaultClock.getAllByListId("list-1").isEmpty())
    }
}
