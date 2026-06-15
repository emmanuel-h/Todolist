package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.SystemClock
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoRepository

class RoomTodoRepository(
    private val dao: TodoItemDao,
    private val clock: Clock = SystemClock()
) : TodoRepository {

    override fun getAllByListId(listId: String): List<TodoItem> =
        dao.getAllByListId(listId).map { TodoItem(it.id, it.title, it.listId, it.completed, it.completedAt, it.position) }

    override fun add(item: TodoItem) {
        dao.insert(TodoItemEntity(item.id, item.title, item.listId, item.isCompleted, item.completedAt, item.position))
    }

    override fun toggle(todoId: String) {
        val entity = dao.getById(todoId) ?: return
        val nowCompleted = !entity.completed
        val completedAt = if (nowCompleted) clock.now() else null
        dao.updateCompleted(todoId, nowCompleted, completedAt)
    }

    override fun delete(todoId: String) {
        dao.deleteById(todoId)
    }

    override fun updateTitle(todoId: String, title: String) {
        dao.updateTitle(todoId, title)
    }

    override fun deleteAllByListId(listId: String) {
        dao.deleteAllByListId(listId)
    }

    override fun reorder(listId: String, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val activeItems = dao.getAllByListId(listId)
            .filter { !it.completed }
            .sortedBy { it.position }
            .toMutableList()
        if (activeItems.isEmpty()) return
        val item = activeItems.removeAt(fromIndex)
        activeItems.add(toIndex, item)
        for (position in activeItems.indices) {
            dao.updatePosition(activeItems[position].id, position)
        }
    }
}
