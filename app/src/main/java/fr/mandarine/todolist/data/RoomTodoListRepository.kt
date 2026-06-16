package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository

class RoomTodoListRepository(private val dao: TodoListDao) : TodoListRepository {

    override fun getAll(): List<TodoList> =
        dao.getAll().map { TodoList(it.id, it.name, it.position) }

    override fun add(todoList: TodoList) {
        dao.insert(TodoListEntity(todoList.id, todoList.name, todoList.position))
    }

    override fun delete(todoListId: String) {
        dao.deleteById(todoListId)
    }

    override fun updateName(todoListId: String, name: String) {
        dao.updateName(todoListId, name)
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
