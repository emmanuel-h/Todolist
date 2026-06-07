package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoRepository

class InMemoryTodoRepository : TodoRepository {

    private val items = mutableListOf<TodoItem>()

    override fun getAllByListId(listId: String): List<TodoItem> =
        items.filter { it.listId == listId }

    override fun add(item: TodoItem) {
        items.add(item)
    }

    override fun toggle(todoId: String) {
        val index = items.indexOfFirst { it.id == todoId }
        if (index >= 0) {
            items[index] = items[index].copy(isCompleted = !items[index].isCompleted)
        }
    }

    override fun deleteAllByListId(listId: String) {
        items.removeAll { it.listId == listId }
    }
}
