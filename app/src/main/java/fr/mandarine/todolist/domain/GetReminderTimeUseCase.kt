package fr.mandarine.todolist.domain

import java.time.LocalTime

class GetReminderTimeUseCase(private val repository: ReminderTimeRepository) {
    operator fun invoke(): LocalTime = repository.getReminderTime()
}
