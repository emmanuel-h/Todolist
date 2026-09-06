package fr.mandarine.todolist.domain

/**
 * Every list, ordered by `position`, with no derived status attached.
 *
 * Used where only the raw lists are wanted: the daily notification check, and the
 * items screen asking "does the list I am open on still exist". The page of lists
 * uses [GetTodoListsWithStatusUseCase] instead, which costs an extra query.
 */
class GetTodoListsUseCase(private val repository: TodoListRepository) {
    operator fun invoke(): List<TodoList> = repository.getAll()
}
