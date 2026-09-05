package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SetReminderTimeUseCaseTest {

    private val repository: ReminderTimeRepository = mockk(relaxed = true)
    private val useCase = SetReminderTimeUseCase(repository)

    @Test
    fun `should call repository with provided minute of day`() {
        useCase(480)

        verify { repository.setReminderTime(480) }
    }

    @Test
    fun `should call repository with midnight when minute of day is 0`() {
        useCase(0)

        verify { repository.setReminderTime(0) }
    }

    @Test
    fun `should call repository with 23 59 when minute of day is 1439`() {
        useCase(1439)

        verify { repository.setReminderTime(1439) }
    }
}
