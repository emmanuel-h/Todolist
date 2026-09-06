package fr.mandarine.todolist.domain

/**
 * Stores a new reminder hour, as minutes since midnight.
 *
 * Writing the setting is only half the job — the already-scheduled WorkManager job
 * still points at the old hour and has to be laid again. That second half is done
 * by the caller, [fr.mandarine.todolist.presentation.ReminderSettingsViewModel],
 * which calls [NotificationScheduler.rescheduleDailyCheck] immediately after this.
 *
 * @throws IllegalArgumentException (from the repository) if outside `0..1439`.
 */
class SetReminderTimeUseCase(private val repository: ReminderTimeRepository) {
    operator fun invoke(minuteOfDay: Int) = repository.setReminderTime(minuteOfDay)
}
