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
class TodoListDaoIncrementTest {

    private lateinit var database: TodoDatabase
    private lateinit var dao: TodoListDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.todoListDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should do nothing when incrementAllPositions is called on empty table`() {
        dao.incrementAllPositions()
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `should increment position of single entity by one when incrementAllPositions is called`() {
        dao.insert(TodoListEntity("1", "Groceries", position = 0))
        dao.incrementAllPositions()
        assertEquals(1, dao.getAll().first().position)
    }

    @Test
    fun `should increment positions of all entities by one when incrementAllPositions is called`() {
        dao.insert(TodoListEntity("1", "First", position = 0))
        dao.insert(TodoListEntity("2", "Second", position = 1))
        dao.insert(TodoListEntity("3", "Third", position = 2))
        dao.incrementAllPositions()
        val entities = dao.getAll()
        assertEquals(1, entities[0].position)
        assertEquals(2, entities[1].position)
        assertEquals(3, entities[2].position)
    }
}
