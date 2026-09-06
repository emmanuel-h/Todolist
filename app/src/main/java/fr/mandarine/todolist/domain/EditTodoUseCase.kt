package fr.mandarine.todolist.domain

/**
 * Rewrites an item's text. Only the title is editable; done-ness, position and list
 * membership each have their own use case, so this one cannot accidentally move an
 * item while renaming it.
 *
 * @throws IllegalArgumentException if [title] is blank. A blank title would leave a
 * row on the page with nothing written on it and no way to select it again.
 */
class EditTodoUseCase(private val repository: TodoRepository) {
    operator fun invoke(todoId: String, title: String) {
        require(title.isNotBlank())
        repository.updateTitle(todoId, title)
    }
}
