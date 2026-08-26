package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.SystemClock
import fr.mandarine.todolist.domain.TodoCounts
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoRepository

class RoomTodoRepository(
    private val dao: TodoItemDao,
    private val clock: Clock = SystemClock()
) : TodoRepository {

    override fun getAllByListId(listId: String): List<TodoItem> =
        dao.getAllByListId(listId).map { TodoItem(it.id, it.title, it.listId, it.completed, it.completedAt, it.position) }

    override fun countsByList(): List<TodoCounts> =
        dao.countsByList().map { TodoCounts(it.listId, it.activeCount, it.completedCount) }

    override fun add(item: TodoItem) {
        dao.insert(TodoItemEntity(item.id, item.title, item.listId, item.isCompleted, item.completedAt, item.position))
    }

    override fun toggle(todoId: String) {
        val entity = dao.getById(todoId) ?: return
        val nowCompleted = !entity.completed
        val completedAt = if (nowCompleted) clock.now() else null
        val position = if (nowCompleted) entity.position else nextActivePositionIn(entity.listId)
        dao.updateCompletedAndPosition(todoId, nowCompleted, completedAt, position)
    }

    private fun nextActivePositionIn(listId: String): Int {
        val active = dao.getAllByListId(listId).filter { !it.completed }
        return if (active.isEmpty()) 0 else active.last().position + 1
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

    /**
     * The page names the rows it is showing, in the order it is showing them, and
     * nothing else. Those rows are laid back into the slots they already occupy
     * between them, so an item the page is not showing — one held behind an undo
     * slip — keeps the place it had rather than being renumbered around a row
     * that was never on screen. Completed items are named by nobody and are never
     * touched.
     */
    override fun reorder(listId: String, orderedActiveIds: List<String>) {
        if (orderedActiveIds.isEmpty()) return
        val active = dao.getAllByListId(listId)
            .filter { !it.completed }
            .sortedBy { it.position }
        val named = orderedActiveIds.toSet()
        val slots = active.withIndex().filter { it.value.id in named }.map { it.index }
        if (slots.size != orderedActiveIds.size) return
        val result = active.map { it.id }.toMutableList()
        slots.forEachIndexed { slot, at -> result[at] = orderedActiveIds[slot] }
        dao.updatePositions(result)
    }
}
