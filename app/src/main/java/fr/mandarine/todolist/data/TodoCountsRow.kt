package fr.mandarine.todolist.data

data class TodoCountsRow(
    val listId: String,
    val activeCount: Int,
    val completedCount: Int
)
