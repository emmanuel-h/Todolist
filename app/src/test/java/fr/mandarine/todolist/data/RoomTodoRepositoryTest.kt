package fr.mandarine.todolist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.TodoItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomTodoRepositoryTest {

    private lateinit var database: TodoDatabase
    private lateinit var repository: RoomTodoRepository
    private var clockTime = 1000L
    private val clock = Clock { clockTime }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        database.todoListDao().insert(TodoListEntity("list-1", "List 1"))
        database.todoListDao().insert(TodoListEntity("list-2", "List 2"))
        repository = RoomTodoRepository(database.todoItemDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
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
    fun `should persist isCompleted false by default when item is added`() {
        val item = TodoItem("1", "Item 1", "list-1")
        repository.add(item)
        assertFalse(repository.getAllByListId("list-1").first().isCompleted)
    }

    @Test
    fun `should persist completedAt as null by default when item is added`() {
        val item = TodoItem("1", "Item 1", "list-1")
        repository.add(item)
        assertNull(repository.getAllByListId("list-1").first().completedAt)
    }

    @Test
    fun `should mark item as completed when toggle is called on an incomplete item`() {
        repository.add(TodoItem("1", "Item 1", "list-1"))
        repository.toggle("1")
        assertTrue(repository.getAllByListId("list-1").first().isCompleted)
    }

    @Test
    fun `should set completedAt to clock time when toggle is called on an incomplete item`() {
        clockTime = 9000L
        repository.add(TodoItem("1", "Item 1", "list-1"))
        repository.toggle("1")
        assertEquals(9000L, repository.getAllByListId("list-1").first().completedAt)
    }

    @Test
    fun `should mark item as incomplete when toggle is called on a completed item`() {
        repository.add(TodoItem("1", "Item 1", "list-1", isCompleted = true, completedAt = 1000L))
        repository.toggle("1")
        assertFalse(repository.getAllByListId("list-1").first().isCompleted)
    }

    @Test
    fun `should clear completedAt when toggle is called on a completed item`() {
        repository.add(TodoItem("1", "Item 1", "list-1", isCompleted = true, completedAt = 1000L))
        repository.toggle("1")
        assertNull(repository.getAllByListId("list-1").first().completedAt)
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
}
