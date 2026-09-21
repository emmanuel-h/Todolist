package fr.mandarine.todolist.domain

import java.time.LocalTime

/**
 * Which check a list belongs to.
 *
 * [AppWide] means the list follows the app-wide reminder hour (its own
 * [TodoList.reminderTime] is null). [At] means the list has its own time and is
 * notified by the check aimed at that specific moment.
 */
sealed interface ReminderSlot {
    data object AppWide : ReminderSlot
    data class At(val time: LocalTime) : ReminderSlot
}
