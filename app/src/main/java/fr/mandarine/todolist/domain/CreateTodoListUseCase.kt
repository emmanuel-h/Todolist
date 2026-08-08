package fr.mandarine.todolist.domain

import java.time.LocalDate
import java.util.UUID

class CreateTodoListUseCase(
    private val repository: TodoListRepository,
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    operator fun invoke(name: String, targetDate: LocalDate? = null): TodoList {
        require(name.isNotBlank())
        val todoList = TodoList(id = generateId(), name = name, position = 0, targetDate = targetDate)
        repository.shiftAllPositionsUp()
        repository.add(todoList)
        return todoList
    }
}
