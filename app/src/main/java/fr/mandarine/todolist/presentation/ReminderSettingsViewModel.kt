package fr.mandarine.todolist.presentation

import androidx.lifecycle.ViewModel
import fr.mandarine.todolist.domain.GetReminderTimeUseCase
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the app-wide hour: the fallback for every list without a time of its own.
 *
 * Nothing on the page sets it any more — the bell that did went when lists got
 * their own time (#106) — so this only reads it. A reader who had chosen an hour
 * keeps it; everyone else follows 08:00. The page uses it to seed a list's clock
 * and to write, faded, the time a list follows.
 *
 * The value is read **synchronously in the constructor**. That is safe here
 * because the backing store is `SharedPreferences` rather than the database. A
 * `StateFlow` always holds a current value, which is why the UI never has to
 * handle "no value yet".
 */
class ReminderSettingsViewModel(
    getReminderTimeUseCase: GetReminderTimeUseCase
) : ViewModel() {

    val reminderTime: StateFlow<LocalTime> = MutableStateFlow(getReminderTimeUseCase())
}
