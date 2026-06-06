package fr.mandarine.todolist.domain

class DeleteTodoListUseCase(
    private val todoListRepository: TodoListRepository,
    private val todoRepository: TodoRepository
) {
    operator fun invoke(todoListId: String) {
        todoRepository.deleteAllByListId(todoListId)
        todoListRepository.delete(todoListId)
    }
}
