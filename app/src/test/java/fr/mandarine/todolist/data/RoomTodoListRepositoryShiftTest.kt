package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class RoomTodoListRepositoryShiftTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should call dao insertAtTop when addAtTop is called`() {
        repository.addAtTop(TodoList("1", "Groceries", position = 0))

        verify { dao.insertAtTop(TodoListEntity("1", "Groceries", 0, null, null)) }
    }

    @Test
    fun `should call dao insertAtTop with dates mapped to epoch days`() {
        val targetDate = java.time.LocalDate.of(2027, 6, 22)

        repository.addAtTop(TodoList("2", "Trip", position = 0, targetDate = targetDate))

        verify { dao.insertAtTop(TodoListEntity("2", "Trip", 0, targetDate.toEpochDay(), null)) }
    }
}
