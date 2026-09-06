package fr.mandarine.todolist.domain

/**
 * Every item of one list, active and completed mixed, ordered by `position`.
 *
 * The split into two sections and the sorting of the completed half happen in
 * [fr.mandarine.todolist.presentation.TodoListViewModel.buildState], not here —
 * that is a presentation decision, and a different screen could want a different
 * arrangement of the same items.
 */
class GetTodosUseCase(private val repository: TodoRepository) {
    operator fun invoke(listId: String): List<TodoItem> = repository.getAllByListId(listId)
}
