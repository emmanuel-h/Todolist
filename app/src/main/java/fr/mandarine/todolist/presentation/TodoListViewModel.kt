package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TodoListViewModel(
    private val addTodoUseCase: AddTodoUseCase,
    private val getTodosUseCase: GetTodosUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTodoUseCase: DeleteTodoUseCase,
    private val editTodoUseCase: EditTodoUseCase,
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

    fun deleteTodo(todoId: String) {
        deleteTodoUseCase(todoId)
        _state.value = buildState()
    }

    fun editTodo(todoId: String, newTitle: String) {
        editTodoUseCase(todoId, newTitle)
        _state.value = buildState()
    }

    private fun buildState(): TodoListState {
        val items = getTodosUseCase(listId)
        if (items.isEmpty()) return TodoListState.Empty
        val activeItems = items.filter { !it.isCompleted }
        val completedItems = items.filter { it.isCompleted }.sortedByDescending { it.completedAt }
        return TodoListState.Content(activeItems, completedItems)
    }
}
