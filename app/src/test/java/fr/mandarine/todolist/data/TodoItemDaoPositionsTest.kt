package fr.mandarine.todolist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoItemDaoPositionsTest {

    private lateinit var database: TodoDatabase
    private lateinit var dao: TodoItemDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        database.todoListDao().insert(TodoListEntity("list-1", "List 1"))
        dao = database.todoItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should write sequential positions when updatePositions is called with ordered ids`() {
        dao.insert(TodoItemEntity("1", "First", "list-1", position = 0))
        dao.insert(TodoItemEntity("2", "Second", "list-1", position = 1))
        dao.insert(TodoItemEntity("3", "Third", "list-1", position = 2))

        dao.updatePositions(listOf("2", "3", "1"))

        assertEquals(listOf("2", "3", "1"), dao.getAllByListId("list-1").map { it.id })
    }

    @Test
    fun `should group counts by list id in countsByList`() {
        database.todoListDao().insert(TodoListEntity("list-2", "List 2"))
        dao.insert(TodoItemEntity("1", "A", "list-1", completed = false))
        dao.insert(TodoItemEntity("2", "B", "list-1", completed = true, completedAt = 500L))
        dao.insert(TodoItemEntity("3", "C", "list-2", completed = false))

        val counts = dao.countsByList().associateBy { it.listId }

        assertEquals(TodoCountsRow("list-1", 1, 1), counts["list-1"])
        assertEquals(TodoCountsRow("list-2", 1, 0), counts["list-2"])
    }
}
