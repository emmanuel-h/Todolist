package fr.mandarine.todolist.domain

/**
 * Records the order a drag left the lists in. The same contract as
 * [ReorderTodosUseCase]: [orderedActiveIds] names only the rows that were on
 * screen, and lists not named — a finished one, or one held behind an undo slip —
 * keep the places they had.
 *
 * @throws IllegalArgumentException if any id appears twice.
 */
class ReorderTodoListsUseCase(private val repository: TodoListRepository) {
    operator fun invoke(orderedActiveIds: List<String>) {
        require(orderedActiveIds.distinct().size == orderedActiveIds.size) {
            "the same list named twice in one reorder"
        }
        repository.reorder(orderedActiveIds)
    }
}
