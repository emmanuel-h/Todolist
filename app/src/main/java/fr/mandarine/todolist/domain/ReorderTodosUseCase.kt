package fr.mandarine.todolist.domain

/**
 * Records the order a drag left the items in.
 *
 * [orderedActiveIds] is what the *page was showing*, in the order it ended up
 * showing it — not the whole list. Anything the page was not showing (a completed
 * item, an item held behind an undo slip) is not named and is deliberately left
 * where it was; the repository puts the named ids back into the slots they already
 * occupied between them. See
 * [fr.mandarine.todolist.data.RoomTodoRepository.reorder].
 *
 * The duplicate check is a guard against a drag bug producing an id twice, which
 * would silently drop a different item off the page: two ids writing to the same
 * slot leaves one position unassigned.
 *
 * @throws IllegalArgumentException if [listId] is blank or any id appears twice.
 */
class ReorderTodosUseCase(private val repository: TodoRepository) {
    operator fun invoke(listId: String, orderedActiveIds: List<String>) {
        require(listId.isNotBlank()) { "a reorder needs the list it happened on" }
        require(orderedActiveIds.distinct().size == orderedActiveIds.size) {
            "the same item named twice in one reorder"
        }
        repository.reorder(listId, orderedActiveIds)
    }
}
