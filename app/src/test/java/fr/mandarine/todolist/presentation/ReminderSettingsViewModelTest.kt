package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.GetReminderTimeUseCase
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.ReminderTimeRepository
import fr.mandarine.todolist.domain.SetReminderTimeUseCase
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReminderSettingsViewModelTest {

    private lateinit var repository: ReminderTimeRepository
    private lateinit var getReminderTimeUseCase: GetReminderTimeUseCase
    private lateinit var setReminderTimeUseCase: SetReminderTimeUseCase
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var viewModel: ReminderSettingsViewModel

    @Before
    fun setUp() {
        repository = FakeReminderTimeRepository(LocalTime.of(8, 0))
        getReminderTimeUseCase = GetReminderTimeUseCase(repository)
        setReminderTimeUseCase = SetReminderTimeUseCase(repository)
        notificationScheduler = mockk(relaxed = true)
        viewModel = ReminderSettingsViewModel(
            getReminderTimeUseCase,
            setReminderTimeUseCase,
            notificationScheduler,
            Dispatchers.Unconfined
        )
    }

    @Test
    fun `should expose the current reminder time on creation`() {
        assertEquals(LocalTime.of(8, 0), viewModel.reminderTime.value)
        assertEquals(8, viewModel.reminderTime.value.hour)
        assertEquals(0, viewModel.reminderTime.value.minute)
    }

    @Test
    fun `should persist new time when setReminderTime is called`() {
        viewModel.setReminderTime(870)

        assertEquals(LocalTime.of(14, 30), viewModel.reminderTime.value)
        assertEquals(14, viewModel.reminderTime.value.hour)
        assertEquals(30, viewModel.reminderTime.value.minute)
    }

    @Test
    fun `should reschedule after persisting when setReminderTime is called`() {
        viewModel.setReminderTime(480)

        verify { notificationScheduler.scheduleDailyCheck() }
    }

    @Test
    fun `should persist before rescheduling when setReminderTime is called`() {
        val schedulerMock: NotificationScheduler = mockk(relaxed = true)
        val repoMock: ReminderTimeRepository = mockk(relaxed = true)
        val getUseCase = GetReminderTimeUseCase(repoMock)
        val setUseCase = SetReminderTimeUseCase(repoMock)
        io.mockk.every { repoMock.getReminderTime() } returns LocalTime.of(8, 0)

        val vm = ReminderSettingsViewModel(getUseCase, setUseCase, schedulerMock, Dispatchers.Unconfined)
        vm.setReminderTime(600)

        verifyOrder {
            repoMock.setReminderTime(600)
            schedulerMock.scheduleDailyCheck()
        }
    }

    @Test
    fun `should update state to reflect new time after setReminderTime`() {
        val repo = FakeReminderTimeRepository(LocalTime.of(8, 0))
        val vm = ReminderSettingsViewModel(
            GetReminderTimeUseCase(repo),
            SetReminderTimeUseCase(repo),
            notificationScheduler,
            Dispatchers.Unconfined
        )

        vm.setReminderTime(1200)

        assertEquals(LocalTime.of(20, 0), vm.reminderTime.value)
        assertEquals(20, vm.reminderTime.value.hour)
        assertEquals(0, vm.reminderTime.value.minute)
    }

    private class FakeReminderTimeRepository(initialTime: LocalTime) : ReminderTimeRepository {
        private var storedMinuteOfDay = initialTime.hour * 60 + initialTime.minute

        override fun getReminderTime(): LocalTime =
            LocalTime.of(storedMinuteOfDay / 60, storedMinuteOfDay % 60)

        override fun setReminderTime(minuteOfDay: Int) {
            storedMinuteOfDay = minuteOfDay
        }
    }
}
