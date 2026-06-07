package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository

class RoomTodoListRepository(private val dao: TodoListDao) : TodoListRepository {

    override fun getAll(): List<TodoList> =
        dao.getAll().map { TodoList(it.id, it.name) }

    override fun add(todoList: TodoList) {
        dao.insert(TodoListEntity(todoList.id, todoList.name))
    }

    override fun delete(todoListId: String) {
        dao.deleteById(todoListId)
    }
}
