package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoRepository

class RoomTodoRepository(private val dao: TodoItemDao) : TodoRepository {

    override fun getAllByListId(listId: String): List<TodoItem> =
        dao.getAllByListId(listId).map { TodoItem(it.id, it.title, it.listId) }

    override fun add(item: TodoItem) {
        dao.insert(TodoItemEntity(item.id, item.title, item.listId))
    }

    override fun deleteAllByListId(listId: String) {
        dao.deleteAllByListId(listId)
    }
}
