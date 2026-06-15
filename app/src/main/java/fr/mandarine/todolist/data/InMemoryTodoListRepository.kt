package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository

class InMemoryTodoListRepository : TodoListRepository {

    private val lists = mutableListOf<TodoList>()

    override fun getAll(): List<TodoList> = lists.toList()

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
}
