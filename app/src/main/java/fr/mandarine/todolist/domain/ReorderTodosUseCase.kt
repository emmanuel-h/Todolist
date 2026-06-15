package fr.mandarine.todolist.domain

class ReorderTodosUseCase(private val repository: TodoRepository) {
    operator fun invoke(listId: String, fromIndex: Int, toIndex: Int) {
        repository.reorder(listId, fromIndex, toIndex)
    }
}
