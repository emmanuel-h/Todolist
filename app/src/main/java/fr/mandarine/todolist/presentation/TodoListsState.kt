package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TodoList

sealed class TodoListsState {
    data object Empty : TodoListsState()
    data class Content(val lists: List<TodoList>) : TodoListsState()
}
