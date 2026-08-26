package fr.mandarine.todolist.domain

import java.time.LocalDate

interface TodoListRepository {
    fun getAll(): List<TodoList>
    fun add(todoList: TodoList)
    fun addAtTop(todoList: TodoList)
    fun delete(todoListId: String)
    fun update(todoListId: String, name: String, targetDate: LocalDate?, dueDate: LocalDate?)
    fun reorder(orderedActiveIds: List<String>)
}
