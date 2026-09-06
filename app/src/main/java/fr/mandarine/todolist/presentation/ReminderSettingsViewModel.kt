package fr.mandarine.todolist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.todolist.domain.GetReminderTimeUseCase
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.SetReminderTimeUseCase
import java.time.LocalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Holds the one setting the app has: the hour the daily check runs at.
 *
 * ### The `_x` / `x` pair
 * This is the standard Kotlin ViewModel shape and it appears in all three
 * ViewModels here. `_reminderTime` is a `MutableStateFlow` and is private;
 * `reminderTime` exposes the same object typed as the read-only `StateFlow`. The
 * screen can collect it but cannot write to it, so there is exactly one place state
 * changes. A `StateFlow` always holds a current value, which is why the UI never
 * has to handle "no value yet".
 *
 * The initial value is read **synchronously in the constructor**. That is safe here
 * because the backing store is `SharedPreferences` rather than the database, and it
 * means the settings dialog opens already showing the right hour.
 *
 * ### Why the scheduler is called here
 * Storing a new hour is not enough — WorkManager already has a job booked for the
 * old one. Writing the setting and re-laying the schedule must happen together, and
 * this is the only caller of [SetReminderTimeUseCase], so the pairing lives here.
 * The final read is deliberate rather than reusing the argument: it publishes what
 * was actually stored.
 */
class ReminderSettingsViewModel(
    private val getReminderTimeUseCase: GetReminderTimeUseCase,
    private val setReminderTimeUseCase: SetReminderTimeUseCase,
    private val notificationScheduler: NotificationScheduler,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _reminderTime = MutableStateFlow(getReminderTimeUseCase())
    val reminderTime: StateFlow<LocalTime> = _reminderTime

    fun setReminderTime(minuteOfDay: Int) {
        viewModelScope.launch(dispatcher) {
            setReminderTimeUseCase(minuteOfDay)
            notificationScheduler.scheduleDailyCheck()
            _reminderTime.value = getReminderTimeUseCase()
        }
    }
}
