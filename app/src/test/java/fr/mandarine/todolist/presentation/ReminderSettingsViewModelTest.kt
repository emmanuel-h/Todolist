package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.GetReminderTimeUseCase
import fr.mandarine.todolist.domain.ReminderTimeRepository
import io.mockk.every
import io.mockk.mockk
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSettingsViewModelTest {

    @Test
    fun `should expose the stored reminder time on creation`() {
        val repository: ReminderTimeRepository = mockk()
        every { repository.getReminderTime() } returns LocalTime.of(14, 30)

        val viewModel = ReminderSettingsViewModel(GetReminderTimeUseCase(repository))

        assertEquals(14, viewModel.reminderTime.value.hour)
        assertEquals(30, viewModel.reminderTime.value.minute)
    }
}
