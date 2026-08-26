package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository
import java.time.LocalDate

class RoomTodoListRepository(private val dao: TodoListDao) : TodoListRepository {

    override fun getAll(): List<TodoList> =
        dao.getAll().map { entity ->
            val dueDate = entity.dueDate?.let { LocalDate.ofEpochDay(it) }
            val targetDate = if (dueDate == null) entity.targetDate?.let { LocalDate.ofEpochDay(it) } else null
            TodoList(entity.id, entity.name, entity.position, targetDate, dueDate)
        }

    override fun add(todoList: TodoList) {
        dao.insert(toEntity(todoList))
    }

    override fun addAtTop(todoList: TodoList) {
        dao.insertAtTop(toEntity(todoList))
    }

    private fun toEntity(todoList: TodoList) =
        TodoListEntity(todoList.id, todoList.name, todoList.position, todoList.targetDate?.toEpochDay(), todoList.dueDate?.toEpochDay())

    override fun delete(todoListId: String) {
        dao.deleteById(todoListId)
    }

    override fun update(todoListId: String, name: String, targetDate: LocalDate?, dueDate: LocalDate?) {
        dao.update(todoListId, name, targetDate?.toEpochDay(), dueDate?.toEpochDay())
    }

    /**
     * The page names the rows it is showing, in the order it is showing them, and
     * nothing else. Those rows are laid back into the slots they already occupy
     * between them, so a list the page is not showing — one finished, or one held
     * behind an undo slip — keeps the place it had rather than being renumbered
     * around a row that was never on screen.
     */
    override fun reorder(orderedActiveIds: List<String>) {
        if (orderedActiveIds.isEmpty()) return
        val sorted = dao.getAll().sortedBy { it.position }
        val named = orderedActiveIds.toSet()
        val slots = sorted.withIndex().filter { it.value.id in named }.map { it.index }
        if (slots.size != orderedActiveIds.size) return
        val result = sorted.map { it.id }.toMutableList()
        slots.forEachIndexed { slot, at -> result[at] = orderedActiveIds[slot] }
        dao.updatePositions(result)
    }
}
