package fr.mandarine.todolist.domain

class GetTodoListsWithStatusUseCase(
    private val todoListRepository: TodoListRepository,
    private val todoRepository: TodoRepository
) {
    operator fun invoke(): List<TodoListSummary> =
        todoListRepository.getAll().map { list ->
            val items = todoRepository.getAllByListId(list.id)
            var completedCount = 0
            for (item in items) {
                if (item.isCompleted) completedCount++
            }
            val activeCount = items.size - completedCount
            TodoListSummary(list, isAllDone(items), activeCount, completedCount)
        }

    private fun isAllDone(items: List<TodoItem>): Boolean {
        if (items.isEmpty()) return false
        for (item in items) {
            if (!item.isCompleted) return false
        }
        return true
    }
}
