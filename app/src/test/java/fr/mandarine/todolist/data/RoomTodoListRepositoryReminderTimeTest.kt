package fr.mandarine.todolist.data

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalTime
import org.junit.Before
import org.junit.Test

class RoomTodoListRepositoryReminderTimeTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should store reminder time as minute of day when setReminderTime is called`() {
        repository.setReminderTime("list-1", LocalTime.of(7, 30))

        verify { dao.setReminderMinute("list-1", 450) }
    }

    @Test
    fun `should store noon as 720 minutes when setReminderTime is called with 12h00`() {
        repository.setReminderTime("list-2", LocalTime.of(12, 0))

        verify { dao.setReminderMinute("list-2", 720) }
    }

    @Test
    fun `should store null when setReminderTime is called with null`() {
        repository.setReminderTime("list-3", null)

        verify { dao.setReminderMinute("list-3", null) }
    }
}
