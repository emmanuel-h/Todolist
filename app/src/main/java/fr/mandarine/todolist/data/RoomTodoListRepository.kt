package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository
import java.time.LocalDate

class RoomTodoListRepository(private val dao: TodoListDao) : TodoListRepository {

    override fun getAll(): List<TodoList> =
        dao.getAll().map { entity ->
            TodoList(
                entity.id,
                entity.name,
                entity.position,
                entity.targetDate?.let { LocalDate.ofEpochDay(it) },
                entity.dueDate?.let { LocalDate.ofEpochDay(it) }
            )
        }

    override fun add(todoList: TodoList) {
        dao.insert(TodoListEntity(todoList.id, todoList.name, todoList.position, todoList.targetDate?.toEpochDay(), todoList.dueDate?.toEpochDay()))
    }

    override fun delete(todoListId: String) {
        dao.deleteById(todoListId)
    }

    override fun updateName(todoListId: String, name: String) {
        dao.updateName(todoListId, name)
    }

    override fun updateTargetDate(todoListId: String, targetDate: LocalDate?) {
        dao.updateTargetDate(todoListId, targetDate?.toEpochDay())
    }

    override fun updateDueDate(todoListId: String, dueDate: LocalDate?) {
        dao.updateDueDate(todoListId, dueDate?.toEpochDay())
    }

    override fun reorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val sorted = dao.getAll().sortedBy { it.position }.toMutableList()
        if (sorted.isEmpty()) return
        val item = sorted.removeAt(fromIndex)
        sorted.add(toIndex, item)
        for (position in sorted.indices) {
            dao.updatePosition(sorted[position].id, position)
        }
    }

    override fun shiftAllPositionsUp() {
        dao.incrementAllPositions()
    }
}
