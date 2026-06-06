package fr.mandarine.todolist.domain

class GetTodosUseCase(private val repository: TodoRepository) {
    operator fun invoke(listId: String): List<TodoItem> = repository.getAllByListId(listId)
}
