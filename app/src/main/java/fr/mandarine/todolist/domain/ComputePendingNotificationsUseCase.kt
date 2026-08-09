package fr.mandarine.todolist.domain

class ComputePendingNotificationsUseCase(private val clock: Clock) {
    operator fun invoke(lists: List<TodoList>): List<ListNotification> {
        val today = clock.today()
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
