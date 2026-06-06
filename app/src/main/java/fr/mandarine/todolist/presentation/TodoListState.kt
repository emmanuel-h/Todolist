package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TodoItem

sealed class TodoListState {
    data object Empty : TodoListState()
    data class Content(val items: List<TodoItem>) : TodoListState()
}
