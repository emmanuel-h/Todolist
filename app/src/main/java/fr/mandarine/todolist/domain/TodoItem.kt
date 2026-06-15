package fr.mandarine.todolist.domain

data class TodoItem(
    val id: String,
    val title: String,
    val listId: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val position: Int = 0
)
