package fr.mandarine.todolist.domain

import java.util.UUID

class AddTodoUseCase(
    private val repository: TodoRepository,
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    operator fun invoke(title: String, listId: String): TodoItem {
        require(title.isNotBlank())
        val item = TodoItem(id = generateId(), title = title, listId = listId)
        repository.add(item)
        return item
    }
}
