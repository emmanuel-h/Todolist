package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TodoItem

sealed class TodoListState {
    object Empty : TodoListState()
    data class Content(
        val activeItems: List<TodoItem>,
        val completedItems: List<TodoItem>
    ) : TodoListState()
}
