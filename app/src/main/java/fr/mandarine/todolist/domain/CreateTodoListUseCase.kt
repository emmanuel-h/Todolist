package fr.mandarine.todolist.domain

import java.time.LocalDate
import java.util.UUID

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
