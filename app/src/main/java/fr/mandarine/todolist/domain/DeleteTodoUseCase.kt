package fr.mandarine.todolist.domain

class DeleteTodoUseCase(private val repository: TodoRepository) {
    operator fun invoke(todoId: String) {
        repository.delete(todoId)
    }
}
