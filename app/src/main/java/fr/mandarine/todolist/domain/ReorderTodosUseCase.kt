package fr.mandarine.todolist.domain

class ReorderTodosUseCase(private val repository: TodoRepository) {
    operator fun invoke(listId: String, orderedActiveIds: List<String>) {
        require(listId.isNotBlank()) { "a reorder needs the list it happened on" }
        require(orderedActiveIds.distinct().size == orderedActiveIds.size) {
            "the same item named twice in one reorder"
        }
        repository.reorder(listId, orderedActiveIds)
    }
}
