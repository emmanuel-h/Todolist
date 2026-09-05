package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GetReminderTimeUseCaseTest {

    private val repository: ReminderTimeRepository = mockk()
    private val useCase = GetReminderTimeUseCase(repository)

    @Test
    fun `should return time from repository`() {
        val expectedTime = LocalTime.of(14, 30)
        every { repository.getReminderTime() } returns expectedTime

        val result = useCase()

        assertEquals(expectedTime, result)
        assertEquals(14, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun `should delegate to repository`() {
        every { repository.getReminderTime() } returns LocalTime.of(8, 0)

        useCase()

        verify { repository.getReminderTime() }
    }
}
