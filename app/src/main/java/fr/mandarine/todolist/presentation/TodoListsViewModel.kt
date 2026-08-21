package fr.mandarine.todolist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.todolist.domain.AnimationEvent
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodoListsViewModel(
    private val createTodoListUseCase: CreateTodoListUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val editTodoListUseCase: EditTodoListUseCase,
    private val getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase,
    private val reorderTodoListsUseCase: ReorderTodoListsUseCase,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow<TodoListsState>(TodoListsState.Empty)
    val state: StateFlow<TodoListsState> = _state

    private val _animationEvents = MutableSharedFlow<AnimationEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val animationEvents: SharedFlow<AnimationEvent> = _animationEvents

    fun refresh() {
        applyAndPublish { }
    }

    fun createList(name: String, targetDate: LocalDate? = null, dueDate: LocalDate? = null) {
        applyAndPublishWithEvent {
            createTodoListUseCase(name, targetDate, dueDate)
            AnimationEvent.ListAdded
        }
    }

    fun submitInlineInput(name: String): Boolean {
        if (name.isBlank()) return false
        applyAndPublishWithEvent {
            createTodoListUseCase(name, null)
            AnimationEvent.ListAdded
        }
        return true
    }

    fun deleteList(todoListId: String) {
        applyAndPublish { deleteTodoListUseCase(todoListId) }
    }

    fun editList(todoListId: String, newName: String, targetDate: LocalDate?, dueDate: LocalDate? = null) {
        if (newName.isBlank()) return
        applyAndPublish { editTodoListUseCase(todoListId, newName, targetDate, dueDate) }
    }

    fun reorderLists(fromIndex: Int, toIndex: Int) {
        applyAndPublish { reorderTodoListsUseCase(fromIndex, toIndex) }
    }

    private fun applyAndPublish(action: () -> Unit) {
        viewModelScope.launch(dispatcher) {
            action()
            _state.value = buildState()
        }
    }

    private fun applyAndPublishWithEvent(action: () -> AnimationEvent) {
        viewModelScope.launch(dispatcher) {
            val event = action()
            _animationEvents.emit(event)
            _state.value = buildState()
        }
    }

    private fun buildState(): TodoListsState {
        val summaries = getTodoListsWithStatusUseCase()
        if (summaries.isEmpty()) return TodoListsState.Empty
        return TodoListsState.Content(
            activeSummaries = summaries.filter { !it.allDone },
            doneSummaries = summaries.filter { it.allDone }
        )
    }
}
