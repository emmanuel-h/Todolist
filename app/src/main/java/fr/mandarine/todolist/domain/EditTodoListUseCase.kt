package fr.mandarine.todolist.domain

import java.time.LocalDate

class EditTodoListUseCase(private val repository: TodoListRepository) {
    operator fun invoke(todoListId: String, name: String, targetDate: LocalDate?, dueDate: LocalDate? = null) {
        require(name.isNotBlank())
        require(targetDate == null || dueDate == null) {
            "A list cannot have both a target date and a due date"
        }
        repository.updateName(todoListId, name)
        repository.updateTargetDate(todoListId, targetDate)
        repository.updateDueDate(todoListId, dueDate)
    }
}
