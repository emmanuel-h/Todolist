package fr.mandarine.todolist.domain

import java.util.UUID

class CreateTodoListUseCase(
    private val repository: TodoListRepository,
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    operator fun invoke(name: String): TodoList {
        require(name.isNotBlank())
        val todoList = TodoList(id = generateId(), name = name)
        repository.add(todoList)
        return todoList
    }
}
