package fr.mandarine.todolist.domain

interface TodoRepository {
    fun getAllByListId(listId: String): List<TodoItem>
    fun add(item: TodoItem)
    fun toggle(todoId: String)
    fun delete(todoId: String)
    fun updateTitle(todoId: String, title: String)
    fun deleteAllByListId(listId: String)
}
