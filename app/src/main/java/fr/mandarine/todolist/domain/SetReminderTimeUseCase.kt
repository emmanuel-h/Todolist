package fr.mandarine.todolist.domain

class SetReminderTimeUseCase(private val repository: ReminderTimeRepository) {
    operator fun invoke(minuteOfDay: Int) = repository.setReminderTime(minuteOfDay)
}
