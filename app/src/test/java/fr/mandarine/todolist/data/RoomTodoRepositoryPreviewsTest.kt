package fr.mandarine.todolist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.ListPreview
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomTodoRepositoryPreviewsTest {

    private lateinit var database: TodoDatabase
    private lateinit var repository: RoomTodoRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        database.todoListDao().insert(TodoListEntity("list-1", "List 1"))
        database.todoListDao().insert(TodoListEntity("list-2", "List 2"))
        repository = RoomTodoRepository(database.todoItemDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should return empty list when there are no items`() {
        assertTrue(repository.previewsByList(3).isEmpty())
    }

    @Test
    fun `should return preview with titles in position order`() {
        repository.add(TodoItem("a", "First", "list-1", position = 0))
        repository.add(TodoItem("b", "Second", "list-1", position = 1))
        repository.add(TodoItem("c", "Third", "list-1", position = 2))

        val result = repository.previewsByList(3)

        assertEquals(1, result.size)
        assertEquals("list-1", result[0].listId)
        assertEquals(listOf("First", "Second", "Third"), result[0].titles)
    }

    @Test
    fun `should limit titles to the given limit`() {
        repository.add(TodoItem("a", "One", "list-1", position = 0))
        repository.add(TodoItem("b", "Two", "list-1", position = 1))
        repository.add(TodoItem("c", "Three", "list-1", position = 2))
        repository.add(TodoItem("d", "Four", "list-1", position = 3))

        val result = repository.previewsByList(2)

        assertEquals(listOf("One", "Two"), result[0].titles)
    }

    @Test
    fun `should exclude completed items from preview`() {
        repository.add(TodoItem("a", "Open", "list-1", position = 0))
        repository.add(TodoItem("b", "Done", "list-1", isCompleted = true, completedAt = 500L, position = 1))

        val result = repository.previewsByList(3)

        assertEquals(listOf("Open"), result[0].titles)
    }

    @Test
    fun `should produce separate previews for each list`() {
        repository.add(TodoItem("a", "List1Item", "list-1", position = 0))
        repository.add(TodoItem("b", "List2Item", "list-2", position = 0))

        val result = repository.previewsByList(3).associateBy { it.listId }

        assertEquals(listOf("List1Item"), result["list-1"]?.titles)
        assertEquals(listOf("List2Item"), result["list-2"]?.titles)
    }

    @Test
    fun `should omit list with no open items`() {
        repository.add(TodoItem("a", "Done", "list-1", isCompleted = true, completedAt = 500L, position = 0))
        repository.add(TodoItem("b", "Open", "list-2", position = 0))

        val result = repository.previewsByList(3)

        assertEquals(1, result.size)
        assertEquals("list-2", result[0].listId)
    }

    @Test
    fun `should throw when limit is zero`() {
        try {
            repository.previewsByList(0)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
        }
    }
}
