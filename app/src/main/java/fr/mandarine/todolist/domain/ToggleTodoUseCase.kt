package fr.mandarine.todolist.domain

class ToggleTodoUseCase(private val repository: TodoRepository) {
    operator fun invoke(todoId: String) {
        repository.toggle(todoId)
    }
}
