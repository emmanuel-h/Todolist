package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository

class InMemoryTodoListRepository : TodoListRepository {

    private val lists = mutableListOf<TodoList>()

    override fun getAll(): List<TodoList> = lists.sortedBy { it.position }

    override fun add(todoList: TodoList) {
        lists.add(todoList)
    }

    override fun delete(todoListId: String) {
        lists.removeAll { it.id == todoListId }
    }

    override fun updateName(todoListId: String, name: String) {
        val index = lists.indexOfFirst { it.id == todoListId }
        if (index >= 0) {
            lists[index] = lists[index].copy(name = name)
        }
    }

    override fun reorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val sorted = lists.sortedBy { it.position }.toMutableList()
        if (sorted.isEmpty()) return
        val item = sorted.removeAt(fromIndex)
        sorted.add(toIndex, item)
        for (position in sorted.indices) {
            val globalIndex = lists.indexOfFirst { it.id == sorted[position].id }
            lists[globalIndex] = lists[globalIndex].copy(position = position)
        }
    }

    override fun shiftAllPositionsUp() {
        for (index in lists.indices) {
            lists[index] = lists[index].copy(position = lists[index].position + 1)
        }
    }
}
