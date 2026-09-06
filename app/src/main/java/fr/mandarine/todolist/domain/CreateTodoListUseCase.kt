package fr.mandarine.todolist.domain

import java.time.LocalDate
import java.util.UUID

/**
 * Writes a new list at the **top** of the page — see [TodoListRepository.addAtTop].
 * A list just written is the one being thought about, so it goes where the eye
 * already is rather than at the bottom of a long page.
 *
 * The `position = 0` passed here is nominal: `addAtTop` shifts everything else down
 * and inserts at zero in one transaction, so the value in the object is just made
 * to agree with where it will land.
 *
 * [generateId] is injectable so tests get predictable ids; see [AddTodoUseCase] for
 * the `operator fun invoke` idiom used by every use case here.
 *
 * @throws IllegalArgumentException if [name] is blank, or if both dates are given —
 * the same rule [TodoList] enforces in its `init`, checked here so the failure
 * names the operation rather than the object.
 */
class CreateTodoListUseCase(
    private val repository: TodoListRepository,
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    operator fun invoke(name: String, targetDate: LocalDate? = null, dueDate: LocalDate? = null, colour: ListColour = ListColour.None): TodoList {
        require(name.isNotBlank())
        require(targetDate == null || dueDate == null) {
            "A list cannot have both a target date and a due date"
        }
        val todoList = TodoList(id = generateId(), name = name, position = 0, targetDate = targetDate, dueDate = dueDate, colour = colour)
        repository.addAtTop(todoList)
        return todoList
    }
}
