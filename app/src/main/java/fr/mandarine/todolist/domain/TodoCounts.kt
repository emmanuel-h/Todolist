package fr.mandarine.todolist.domain

data class TodoCounts(
    val listId: String,
    val activeCount: Int,
    val completedCount: Int
)
