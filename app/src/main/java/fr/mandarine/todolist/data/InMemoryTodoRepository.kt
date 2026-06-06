package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoRepository

class InMemoryTodoRepository : TodoRepository {

    private val items = mutableListOf<TodoItem>()

    override fun getAll(): List<TodoItem> = items.toList()

    override fun add(item: TodoItem) {
        items.add(item)
    }
}
