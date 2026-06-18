package fr.mandarine.todolist.domain

data class TodoListSummary(
    val list: TodoList,
    val allDone: Boolean,
    val activeCount: Int = 0,
    val completedCount: Int = 0
)
