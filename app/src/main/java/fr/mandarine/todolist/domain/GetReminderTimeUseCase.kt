package fr.mandarine.todolist.domain

import java.time.LocalTime

/**
 * The hour the daily check runs at. A one-line pass-through, kept as a use case so
 * that the ViewModel depends on `domain/` rather than reaching into a repository
 * interface directly — the same shape as every other read in the app.
 */
class GetReminderTimeUseCase(private val repository: ReminderTimeRepository) {
    operator fun invoke(): LocalTime = repository.getReminderTime()
}
