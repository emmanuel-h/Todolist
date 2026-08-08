package fr.mandarine.todolist.domain

import java.time.LocalDate

interface TodoListRepository {
    fun getAll(): List<TodoList>
    fun add(todoList: TodoList)
    fun delete(todoListId: String)
    fun updateName(todoListId: String, name: String)
    fun updateTargetDate(todoListId: String, targetDate: LocalDate?)
    fun reorder(fromIndex: Int, toIndex: Int)
    fun shiftAllPositionsUp()
}
