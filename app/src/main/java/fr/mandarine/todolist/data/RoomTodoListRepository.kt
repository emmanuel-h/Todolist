package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository
import java.time.LocalDate

/**
 * The Room-backed [TodoListRepository]. Maps [TodoListEntity] to and from
 * `fr.mandarine.todolist.domain.TodoList`.
 *
 * Two conversions are worth knowing about:
 * - dates are stored as epoch **days** and converted with `LocalDate.ofEpochDay` /
 *   `toEpochDay`;
 * - the colour is stored as the enum constant's `name` and read back with
 *   `ListColour.valueOf`, which **throws** on an unknown name. A colour constant
 *   removed from the enum without a migration therefore crashes on read rather than
 *   silently falling back.
 *
 * On read, a row that somehow holds both dates is resolved in favour of the hard
 * one: `targetDate` is only taken when `dueDate` is null. Nothing should ever write
 * such a row — the domain forbids it — but the storage layer has no constraint
 * enforcing it, and [TodoList] would throw from its `init` if handed both.
 */
class RoomTodoListRepository(private val dao: TodoListDao) : TodoListRepository {

    override fun getAll(): List<TodoList> =
        dao.getAll().map { entity ->
            val dueDate = entity.dueDate?.let { LocalDate.ofEpochDay(it) }
            val targetDate = if (dueDate == null) entity.targetDate?.let { LocalDate.ofEpochDay(it) } else null
            val colour = ListColour.valueOf(entity.colour)
            TodoList(entity.id, entity.name, entity.position, targetDate, dueDate, colour)
        }

    override fun add(todoList: TodoList) {
        dao.insert(toEntity(todoList))
    }

    override fun addAtTop(todoList: TodoList) {
        dao.insertAtTop(toEntity(todoList))
    }

    private fun toEntity(todoList: TodoList) =
        TodoListEntity(todoList.id, todoList.name, todoList.position, todoList.targetDate?.toEpochDay(), todoList.dueDate?.toEpochDay(), todoList.colour.name)

    override fun delete(todoListId: String) {
        dao.deleteById(todoListId)
    }

    override fun update(todoListId: String, name: String, targetDate: LocalDate?, dueDate: LocalDate?, colour: ListColour) {
        dao.update(todoListId, name, targetDate?.toEpochDay(), dueDate?.toEpochDay(), colour.name)
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
