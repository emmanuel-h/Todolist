package fr.mandarine.todolist.domain

class EditTodoUseCase(private val repository: TodoRepository) {
    operator fun invoke(todoId: String, title: String) {
        require(title.isNotBlank())
        repository.updateTitle(todoId, title)
    }
}
