package fr.mandarine.todolist.domain

interface TodoListRepository {
    fun getAll(): List<TodoList>
    fun add(todoList: TodoList)
    fun delete(todoListId: String)
    fun updateName(todoListId: String, name: String)
    fun reorder(fromIndex: Int, toIndex: Int)
}
