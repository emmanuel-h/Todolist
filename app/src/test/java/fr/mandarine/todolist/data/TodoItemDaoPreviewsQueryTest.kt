package fr.mandarine.todolist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class TodoItemDaoPreviewsQueryTest {

    private lateinit var database: TodoDatabase
    private lateinit var dao: TodoItemDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        database.todoListDao().insert(TodoListEntity("list-1", "List 1"))
        database.todoListDao().insert(TodoListEntity("list-2", "List 2"))
        dao = database.todoItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should return empty when there are no items`() {
        assertTrue(dao.previewsQuery(3).isEmpty())
    }

    @Test
    fun `should return open items up to the limit`() {
        dao.insert(TodoItemEntity("a", "Alpha", "list-1", completed = false, position = 0))
        dao.insert(TodoItemEntity("b", "Beta", "list-1", completed = false, position = 1))
        dao.insert(TodoItemEntity("c", "Gamma", "list-1", completed = false, position = 2))
        dao.insert(TodoItemEntity("d", "Delta", "list-1", completed = false, position = 3))

        val result = dao.previewsQuery(3)

        assertEquals(3, result.size)
        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun `should exclude completed items`() {
        dao.insert(TodoItemEntity("a", "Alpha", "list-1", completed = false, position = 0))
        dao.insert(TodoItemEntity("b", "Beta", "list-1", completed = true, completedAt = 500L, position = 1))
        dao.insert(TodoItemEntity("c", "Gamma", "list-1", completed = false, position = 2))

        val result = dao.previewsQuery(3)

        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    @Test
    fun `should return items from multiple lists each limited independently`() {
        dao.insert(TodoItemEntity("a1", "List1 First", "list-1", completed = false, position = 0))
        dao.insert(TodoItemEntity("a2", "List1 Second", "list-1", completed = false, position = 1))
        dao.insert(TodoItemEntity("a3", "List1 Third", "list-1", completed = false, position = 2))
        dao.insert(TodoItemEntity("a4", "List1 Fourth", "list-1", completed = false, position = 3))
        dao.insert(TodoItemEntity("b1", "List2 First", "list-2", completed = false, position = 0))
        dao.insert(TodoItemEntity("b2", "List2 Second", "list-2", completed = false, position = 1))

        val result = dao.previewsQuery(2)

        assertEquals(4, result.size)
        assertEquals(listOf("a1", "a2"), result.filter { it.listId == "list-1" }.map { it.id })
        assertEquals(listOf("b1", "b2"), result.filter { it.listId == "list-2" }.map { it.id })
    }

    @Test
    fun `should order by position ascending within a list`() {
        dao.insert(TodoItemEntity("a", "Third", "list-1", completed = false, position = 2))
        dao.insert(TodoItemEntity("b", "First", "list-1", completed = false, position = 0))
        dao.insert(TodoItemEntity("c", "Second", "list-1", completed = false, position = 1))

        val result = dao.previewsQuery(3)

        assertEquals(listOf("b", "c", "a"), result.map { it.id })
    }

    @Test
    fun `should break position ties by id ascending`() {
        dao.insert(TodoItemEntity("b", "Tied B", "list-1", completed = false, position = 0))
        dao.insert(TodoItemEntity("a", "Tied A", "list-1", completed = false, position = 0))
        dao.insert(TodoItemEntity("c", "After", "list-1", completed = false, position = 1))

        val result = dao.previewsQuery(3)

        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun `should exclude list with only completed items`() {
        dao.insert(TodoItemEntity("a", "Done", "list-1", completed = true, completedAt = 500L, position = 0))
        dao.insert(TodoItemEntity("b", "Open", "list-2", completed = false, position = 0))

        val result = dao.previewsQuery(3)

        assertEquals(listOf("b"), result.map { it.id })
    }

    @Test
    fun `should return fewer than limit when list has fewer open items`() {
        dao.insert(TodoItemEntity("a", "Only", "list-1", completed = false, position = 0))

        val result = dao.previewsQuery(5)

        assertEquals(1, result.size)
        assertEquals("a", result[0].id)
    }

    @Test
    fun `should handle limit of one`() {
        dao.insert(TodoItemEntity("a", "First", "list-1", completed = false, position = 0))
        dao.insert(TodoItemEntity("b", "Second", "list-1", completed = false, position = 1))

        val result = dao.previewsQuery(1)

        assertEquals(listOf("a"), result.map { it.id })
    }
}
