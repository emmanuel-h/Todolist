package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.SystemClock
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoRepository

class InMemoryTodoRepository(private val clock: Clock = SystemClock()) : TodoRepository {

    private val items = mutableListOf<TodoItem>()

    override fun getAllByListId(listId: String): List<TodoItem> =
        items.filter { it.listId == listId }

    override fun add(item: TodoItem) {
        items.add(item)
    }

    override fun toggle(todoId: String) {
        val index = items.indexOfFirst { it.id == todoId }
        if (index < 0) return
        val current = items[index]
        val nowCompleted = !current.isCompleted
        val completedAt = if (nowCompleted) clock.now() else null
        items[index] = current.copy(isCompleted = nowCompleted, completedAt = completedAt)
    }

    override fun deleteAllByListId(listId: String) {
        items.removeAll { it.listId == listId }
    }
}
