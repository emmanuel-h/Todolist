package fr.mandarine.todolist.domain

import java.time.LocalTime

/**
 * Writes a per-list reminder time. Null clears the time so the list follows the
 * app-wide hour.
 *
 * [minuteOfDay] must be in `0..1439` when non-null; passing a value outside that
 * range throws [IllegalArgumentException].
 */
class SetListReminderTimeUseCase(private val repository: TodoListRepository) {
    operator fun invoke(todoListId: String, minuteOfDay: Int?) {
        if (minuteOfDay != null) require(minuteOfDay in 0..1439) {
            "minuteOfDay must be in 0..1439 but was $minuteOfDay"
        }
        val reminderTime = minuteOfDay?.let { LocalTime.of(it / 60, it % 60) }
        repository.setReminderTime(todoListId, reminderTime)
    }
}
