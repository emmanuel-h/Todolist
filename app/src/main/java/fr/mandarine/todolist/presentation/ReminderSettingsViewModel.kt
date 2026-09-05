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
