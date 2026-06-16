package fr.mandarine.todolist.domain

import java.util.UUID

class CreateTodoListUseCase(
    private val repository: TodoListRepository,
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    operator fun invoke(name: String): TodoList {
        require(name.isNotBlank())
        val position = repository.getAll().size
        val todoList = TodoList(id = generateId(), name = name, position = position)
        repository.add(todoList)
        return todoList
    }
}
