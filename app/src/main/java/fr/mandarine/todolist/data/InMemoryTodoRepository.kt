package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.SystemClock
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoRepository

class InMemoryTodoRepository(private val clock: Clock = SystemClock()) : TodoRepository {

    private val items = mutableListOf<TodoItem>()

    override fun getAllByListId(listId: String): List<TodoItem> =
        items.filter { it.listId == listId }.sortedBy { it.position }

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

    override fun delete(todoId: String) {
        items.removeAll { it.id == todoId }
    }

    override fun updateTitle(todoId: String, title: String) {
        val index = items.indexOfFirst { it.id == todoId }
        if (index < 0) return
        items[index] = items[index].copy(title = title)
    }

    override fun deleteAllByListId(listId: String) {
        items.removeAll { it.listId == listId }
    }

    override fun reorder(listId: String, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val activeItems = items.filter { it.listId == listId && !it.isCompleted }
            .sortedBy { it.position }
            .toMutableList()
        if (activeItems.isEmpty()) return
        val item = activeItems.removeAt(fromIndex)
        activeItems.add(toIndex, item)
        for (position in activeItems.indices) {
            val globalIndex = items.indexOfFirst { it.id == activeItems[position].id }
            items[globalIndex] = items[globalIndex].copy(position = position)
        }
    }
}
