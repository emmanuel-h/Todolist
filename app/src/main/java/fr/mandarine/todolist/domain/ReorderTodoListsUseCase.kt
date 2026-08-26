package fr.mandarine.todolist.domain

class ReorderTodoListsUseCase(private val repository: TodoListRepository) {
    operator fun invoke(orderedActiveIds: List<String>) {
        require(orderedActiveIds.distinct().size == orderedActiveIds.size) {
            "the same list named twice in one reorder"
        }
        repository.reorder(orderedActiveIds)
    }
}
