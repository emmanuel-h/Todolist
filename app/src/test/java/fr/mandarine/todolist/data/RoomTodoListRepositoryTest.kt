package fr.mandarine.todolist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.domain.TodoList
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
class RoomTodoListRepositoryTest {

    private lateinit var database: TodoDatabase
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomTodoListRepository(database.todoListDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Tearing a list off has to take its items with it, in one go. Two statements
     * left an emptied list behind when the process died between them.
     */
    @Test
    fun `should remove the items of a list when that list is deleted`() {
        repository.add(TodoList("1", "Groceries"))
        database.todoItemDao().insert(TodoItemEntity("i1", "milk", "1", false, null, 0))
        database.todoItemDao().insert(TodoItemEntity("i2", "eggs", "1", false, null, 1))

        repository.delete("1")

        assertTrue(database.todoItemDao().getAllByListId("1").isEmpty())
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `should leave the items of other lists when a list is deleted`() {
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Chores"))
        database.todoItemDao().insert(TodoItemEntity("i1", "milk", "1", false, null, 0))
        database.todoItemDao().insert(TodoItemEntity("i2", "sweep", "2", false, null, 0))

        repository.delete("1")

        assertEquals(1, database.todoItemDao().getAllByListId("2").size)
    }

    @Test
    fun `should return empty list when no lists have been added`() {
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `should return added list when one list is added`() {
        val todoList = TodoList("1", "Groceries")
        repository.add(todoList)
        assertEquals(listOf(todoList), repository.getAll())
    }

    @Test
    fun `should return all lists when multiple lists are added`() {
        val list1 = TodoList("1", "Groceries")
        val list2 = TodoList("2", "Home")
        repository.add(list1)
        repository.add(list2)
        assertEquals(listOf(list1, list2), repository.getAll())
    }

    @Test
    fun `should remove list when delete is called with its id`() {
        val todoList = TodoList("1", "Groceries")
        repository.add(todoList)
        repository.delete("1")
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `should remove only the specified list when delete is called`() {
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Home"))
        repository.delete("1")
        assertEquals(listOf(TodoList("2", "Home")), repository.getAll())
    }

    @Test
    fun `should do nothing when delete is called with a non-existent id`() {
        repository.add(TodoList("1", "Groceries"))
        repository.delete("nonexistent")
        assertEquals(1, repository.getAll().size)
    }
}
