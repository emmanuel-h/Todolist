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
        dao.getAllByListId(listId).map { TodoItem(it.id, it.title, it.listId, it.completed, it.completedAt) }

    override fun add(item: TodoItem) {
        dao.insert(TodoItemEntity(item.id, item.title, item.listId, item.isCompleted, item.completedAt))
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
}
