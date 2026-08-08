package fr.mandarine.todolist.domain

import java.time.LocalDate

class EditTodoListUseCase(private val repository: TodoListRepository) {
    operator fun invoke(todoListId: String, name: String, targetDate: LocalDate?) {
        require(name.isNotBlank())
        repository.updateName(todoListId, name)
        repository.updateTargetDate(todoListId, targetDate)
    }
}
