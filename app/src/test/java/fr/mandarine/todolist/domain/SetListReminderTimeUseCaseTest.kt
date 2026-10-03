package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalTime
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class SetListReminderTimeUseCaseTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: SetListReminderTimeUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = SetListReminderTimeUseCase(repository)
    }

    @Test
    fun `should store 07h30 as reminder time when minute of day is 450`() {
        useCase("list-1", 450)

        verify { repository.setReminderTime("list-1", LocalTime.of(7, 30)) }
    }

    @Test
    fun `should store midnight when minute of day is 0`() {
        useCase("list-2", 0)

        verify { repository.setReminderTime("list-2", LocalTime.of(0, 0)) }
    }

    @Test
    fun `should store 23h59 when minute of day is 1439`() {
        useCase("list-3", 1439)

        verify { repository.setReminderTime("list-3", LocalTime.of(23, 59)) }
    }

    @Test
    fun `should clear reminder time when minute of day is null`() {
        useCase("list-4", null)

        verify { repository.setReminderTime("list-4", null) }
    }

    @Test
    fun `should throw when minute of day is negative`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", -1)
        }
    }

    @Test
    fun `should throw when minute of day is 1440`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", 1440)
        }
    }

    @Test
    fun `should throw when minute of day exceeds maximum`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", 2000)
        }
    }
}
