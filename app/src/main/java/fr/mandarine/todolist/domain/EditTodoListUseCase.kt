package fr.mandarine.todolist.domain

import java.time.LocalDate

/**
 * Rewrites a list's name, date and colour together.
 *
 * All of the editable fields go in one call because they are edited in one dialog,
 * and because the "target **or** due, never both" invariant can only be checked
 * against a complete pair. A caller changing just the colour still has to pass the
 * current name and dates back in — see the `onWriteDate` and `onRenameList`
 * callbacks in `ui/nav/PageStack.kt` for how the screen reads the current values
 * off the summary before calling.
 *
 * @throws IllegalArgumentException if [name] is blank or both dates are given.
 */
class EditTodoListUseCase(private val repository: TodoListRepository) {
    operator fun invoke(todoListId: String, name: String, targetDate: LocalDate?, dueDate: LocalDate? = null, colour: ListColour = ListColour.None) {
        require(name.isNotBlank())
        require(targetDate == null || dueDate == null) {
            "A list cannot have both a target date and a due date"
        }
        repository.update(todoListId, name, targetDate, dueDate, colour)
    }
}
