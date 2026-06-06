package fr.mandarine.todolist.domain

class GetTodosUseCase(private val repository: TodoRepository) {
    operator fun invoke(): List<TodoItem> = repository.getAll()
}
