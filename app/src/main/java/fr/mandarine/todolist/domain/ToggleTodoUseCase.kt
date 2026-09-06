package fr.mandarine.todolist.domain

/**
 * Flips one item between done and not-done. There is no "complete" and "uncomplete"
 * pair because the UI control is a single ring that is either inked or not.
 *
 * The interesting work is in the repository, not here: ticking an item stamps
 * `completedAt` and freezes its position, and un-ticking clears the stamp and moves
 * it back to the foot of the active run. See
 * [fr.mandarine.todolist.data.RoomTodoRepository.toggle].
 */
class ToggleTodoUseCase(private val repository: TodoRepository) {
    operator fun invoke(todoId: String) {
        repository.toggle(todoId)
    }
}
