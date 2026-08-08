package fr.mandarine.todolist.domain

import java.time.LocalDate

class GetTodoListsWithStatusUseCase(
    private val todoListRepository: TodoListRepository,
    private val todoRepository: TodoRepository,
    private val clock: Clock
) {
    operator fun invoke(): List<TodoListSummary> {
        val today = LocalDate.ofEpochDay(clock.now() / 86_400_000L)
        return todoListRepository.getAll().map { list ->
            val items = todoRepository.getAllByListId(list.id)
            var completedCount = 0
            for (item in items) {
                if (item.isCompleted) completedCount++
            }
            val activeCount = items.size - completedCount
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
            TodoListSummary(list, isAllDone(items), activeCount, completedCount, isElapsed, showYear, dueDateStatus, showDueDateYear)
        }
    }

    private fun isAllDone(items: List<TodoItem>): Boolean {
        if (items.isEmpty()) return false
        for (item in items) {
            if (!item.isCompleted) return false
        }
        return true
    }
}
