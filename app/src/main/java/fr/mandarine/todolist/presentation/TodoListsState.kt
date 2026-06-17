package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TodoListSummary

sealed class TodoListsState {
    data object Empty : TodoListsState()
    data class Content(
        val activeSummaries: List<TodoListSummary>,
        val doneSummaries: List<TodoListSummary>
    ) : TodoListsState()
}
