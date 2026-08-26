package fr.mandarine.todolist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.AnimationEvent
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodoListViewModel(
    private val addTodoUseCase: AddTodoUseCase,
    private val getTodosUseCase: GetTodosUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTodoUseCase: DeleteTodoUseCase,
    private val editTodoUseCase: EditTodoUseCase,
    private val reorderTodosUseCase: ReorderTodosUseCase,
    private val getTodoListsUseCase: GetTodoListsUseCase,
    private val listId: String,
    private val dispatcher: CoroutineDispatcher,
    private val writeScope: CoroutineScope? = null
) : ViewModel() {

    private val _state = MutableStateFlow<TodoListState>(TodoListState.Empty)
    val state: StateFlow<TodoListState> = _state

    private val _animationEvents = MutableSharedFlow<AnimationEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val animationEvents: SharedFlow<AnimationEvent> = _animationEvents

    fun refresh() {
        applyAndPublish { }
    }

    fun addTodo(title: String) {
        applyAndPublishWithEvent {
            val item = addTodoUseCase(title, listId)
            AnimationEvent.ItemAdded(item.id)
        }
    }

    fun submitInlineInput(title: String): Boolean {
        if (title.isBlank()) return false
        applyAndPublishWithEvent {
            val item = addTodoUseCase(title, listId)
            AnimationEvent.ItemAdded(item.id)
        }
        return true
    }

    fun toggleTodo(todoId: String) {
        var wasCompleted = false
        val current = state.value
        if (current is TodoListState.Content) {
            for (item in current.completedItems) {
                if (item.id == todoId) {
                    wasCompleted = true
                    break
                }
            }
        }
        applyAndPublishWithEvent {
            toggleTodoUseCase(todoId)
            if (wasCompleted) AnimationEvent.ItemRestored(todoId) else AnimationEvent.ItemCompleted(todoId)
        }
    }

    /**
     * A delete is written on a scope the composition root owns, not on this
     * view model's own. The page of items lives on an entry of the back stack
     * and its scope dies with the entry, so a row torn off and then walked away
     * from within its undo slip never reached the repository at all.
     */
    fun deleteTodo(todoId: String) {
        applyAndPublishWithEvent(writeScope ?: viewModelScope) {
            deleteTodoUseCase(todoId)
            AnimationEvent.ItemDeleted(todoId)
        }
    }

    fun editTodo(todoId: String, newTitle: String) {
        applyAndPublish { editTodoUseCase(todoId, newTitle) }
    }

    fun reorderTodos(orderedActiveIds: List<String>) {
        applyAndPublish { reorderTodosUseCase(listId, orderedActiveIds) }
    }

    private fun applyAndPublish(action: () -> Unit) {
        viewModelScope.launch(dispatcher) {
            action()
            _state.value = buildState()
        }
    }

    private fun applyAndPublishWithEvent(
        scope: CoroutineScope = viewModelScope,
        action: () -> AnimationEvent
    ) {
        scope.launch(dispatcher) {
            val event = action()
            _animationEvents.emit(event)
            _state.value = buildState()
        }
    }

    private fun buildState(): TodoListState {
        if (!listExists()) return TodoListState.NotFound
        val items = getTodosUseCase(listId)
        if (items.isEmpty()) return TodoListState.Empty
        val activeItems = mutableListOf<TodoItem>()
        val completedItems = mutableListOf<TodoItem>()
        for (item in items) {
            if (item.isCompleted) completedItems.add(item) else activeItems.add(item)
        }
        completedItems.sortWith { a, b -> compareValues(b.completedAt, a.completedAt) }
        return TodoListState.Content(activeItems, completedItems)
    }

    private fun listExists(): Boolean {
        for (list in getTodoListsUseCase()) {
            if (list.id == listId) return true
        }
        return false
    }
}
