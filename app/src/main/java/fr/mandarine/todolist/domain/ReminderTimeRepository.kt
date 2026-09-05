package fr.mandarine.todolist.domain

import java.time.LocalTime

interface ReminderTimeRepository {
    fun getReminderTime(): LocalTime
    fun setReminderTime(minuteOfDay: Int)
}
