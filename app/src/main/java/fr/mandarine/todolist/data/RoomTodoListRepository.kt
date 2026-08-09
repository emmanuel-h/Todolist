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

    override fun reorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val sorted = dao.getAll().sortedBy { it.position }.toMutableList()
        if (sorted.isEmpty()) return
        val item = sorted.removeAt(fromIndex)
        sorted.add(toIndex, item)
        dao.updatePositions(sorted.map { it.id })
    }
}
