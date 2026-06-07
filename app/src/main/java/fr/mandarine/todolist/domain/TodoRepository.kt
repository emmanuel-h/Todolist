package fr.mandarine.todolist.domain

interface TodoRepository {
    fun getAllByListId(listId: String): List<TodoItem>
    fun add(item: TodoItem)
    fun toggle(todoId: String)
    fun deleteAllByListId(listId: String)
}
