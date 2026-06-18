package fr.mandarine.todolist.domain

import java.util.UUID

class AddTodoUseCase(
    private val repository: TodoRepository,
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    operator fun invoke(title: String, listId: String): TodoItem {
        require(title.isNotBlank())
        val position = repository.getAllByListId(listId).filter { !it.isCompleted }.size
        val item = TodoItem(id = generateId(), title = title, listId = listId, position = position)
        repository.add(item)
        return item
    }
}
