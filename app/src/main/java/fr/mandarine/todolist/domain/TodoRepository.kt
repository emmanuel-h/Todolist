package fr.mandarine.todolist.domain

interface TodoRepository {
    fun getAll(): List<TodoItem>
    fun add(item: TodoItem)
}
