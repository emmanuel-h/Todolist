package fr.mandarine.todolist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.domain.TodoList
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomTodoListRepositoryTargetDateTest {

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

    @Test
    fun `should persist targetDate when list is added with targetDate`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries", targetDate = targetDate))

        assertEquals(targetDate, repository.getAll().first().targetDate)
    }

    @Test
    fun `should return null targetDate for list added without targetDate`() {
        repository.add(TodoList("1", "Groceries"))

        assertNull(repository.getAll().first().targetDate)
    }

    @Test
    fun `should persist targetDate when updateTargetDate is called`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))

        repository.updateTargetDate("1", targetDate)

        assertEquals(targetDate, repository.getAll().first().targetDate)
    }

    @Test
    fun `should clear targetDate when updateTargetDate is called with null`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries", targetDate = targetDate))

        repository.updateTargetDate("1", null)

        assertNull(repository.getAll().first().targetDate)
    }

    @Test
    fun `should only update targetDate of the targeted list`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Work"))

        repository.updateTargetDate("1", targetDate)

        assertEquals(targetDate, repository.getAll()[0].targetDate)
        assertNull(repository.getAll()[1].targetDate)
    }
}
