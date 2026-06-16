package fr.mandarine.todolist.domain

class ReorderTodoListsUseCase(private val repository: TodoListRepository) {
    operator fun invoke(fromIndex: Int, toIndex: Int) {
        repository.reorder(fromIndex, toIndex)
    }
}
