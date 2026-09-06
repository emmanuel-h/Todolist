package fr.mandarine.todolist.domain

/**
 * Removes one item. Unconditional — the "are you sure" prompt lives in the UI
 * ([fr.mandarine.todolist.ui.ConfirmDeleteRequest]); by the time this is called the
 * reader has already answered.
 */
class DeleteTodoUseCase(private val repository: TodoRepository) {
    operator fun invoke(todoId: String) {
        repository.delete(todoId)
    }
}
