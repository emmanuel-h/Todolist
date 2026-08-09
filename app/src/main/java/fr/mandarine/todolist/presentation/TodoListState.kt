package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TodoItem

sealed class TodoListState {
    data object NotFound : TodoListState()
    data object Empty : TodoListState()
    data class Content(
        val activeItems: List<TodoItem>,
        val completedItems: List<TodoItem>
    ) : TodoListState()
}
