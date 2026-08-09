package fr.mandarine.todolist.domain

class GetTodoListsWithStatusUseCase(
    private val todoListRepository: TodoListRepository,
    private val todoRepository: TodoRepository,
    private val clock: Clock
) {
    operator fun invoke(): List<TodoListSummary> {
        val today = clock.today()
        val countsByList = todoRepository.countsByList().associateBy { it.listId }
        return todoListRepository.getAll().map { list ->
            val counts = countsByList[list.id]
            val activeCount = counts?.activeCount ?: 0
            val completedCount = counts?.completedCount ?: 0
            val allDone = activeCount == 0 && completedCount > 0
            val isElapsed = list.targetDate != null && list.targetDate.isBefore(today)
            val showYear = list.targetDate != null && list.targetDate.year != today.year
            val dueDateStatus = list.dueDate?.let { d ->
                when {
                    d.isBefore(today) -> DueDateStatus.OVERDUE
                    d == today -> DueDateStatus.TODAY
                    else -> DueDateStatus.FUTURE
                }
            }
            val showDueDateYear = list.dueDate != null && list.dueDate.year != today.year
            TodoListSummary(list, allDone, activeCount, completedCount, isElapsed, showYear, dueDateStatus, showDueDateYear)
        }
    }
}
