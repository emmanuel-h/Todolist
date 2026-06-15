package fr.mandarine.todolist.domain

class EditTodoListUseCase(private val repository: TodoListRepository) {
    operator fun invoke(todoListId: String, name: String) {
        require(name.isNotBlank())
        repository.updateName(todoListId, name)
    }
}
