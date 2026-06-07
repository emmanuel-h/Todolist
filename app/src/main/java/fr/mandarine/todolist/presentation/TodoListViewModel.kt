package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TodoListViewModel(
    private val addTodoUseCase: AddTodoUseCase,
    private val getTodosUseCase: GetTodosUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val listId: String
) {
    private val _state = MutableStateFlow(buildState())
    val state: StateFlow<TodoListState> = _state

    fun addTodo(title: String) {
        addTodoUseCase(title, listId)
    }

    fun submitInlineInput(title: String): Boolean {
        if (title.isBlank()) return false
        addTodoUseCase(title, listId)
        _state.value = buildState()
        return true
    }

    fun toggleTodo(todoId: String) {
        toggleTodoUseCase(todoId)
        _state.value = buildState()
    }

    private fun buildState(): TodoListState {
        val items = getTodosUseCase(listId)
        return if (items.isEmpty()) TodoListState.Empty else TodoListState.Content(items)
    }
}
