package fr.mandarine.todolist.domain

import java.time.LocalDate

class ComputePendingNotificationsUseCase(private val clock: Clock) {
    operator fun invoke(lists: List<TodoList>): List<ListNotification> {
        val today = LocalDate.ofEpochDay(clock.now() / 86_400_000L)
        val tomorrow = today.plusDays(1)
        return lists.mapNotNull { list ->
            when {
                list.dueDate == today -> ListNotification.DueDateToday(list)
                list.targetDate == tomorrow -> ListNotification.TargetDateTomorrow(list)
                else -> null
            }
        }
    }
}
