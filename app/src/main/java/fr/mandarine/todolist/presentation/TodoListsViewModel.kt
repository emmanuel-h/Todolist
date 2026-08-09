package fr.mandarine.todolist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
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

    fun refresh() {
        applyAndPublish { }
    }

    fun createList(name: String, targetDate: LocalDate? = null, dueDate: LocalDate? = null) {
        applyAndPublish { createTodoListUseCase(name, targetDate, dueDate) }
    }

    fun submitInlineInput(name: String): Boolean {
        if (name.isBlank()) return false
        applyAndPublish { createTodoListUseCase(name, null) }
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

    private fun buildState(): TodoListsState {
        val summaries = getTodoListsWithStatusUseCase()
        if (summaries.isEmpty()) return TodoListsState.Empty
        return TodoListsState.Content(
            activeSummaries = summaries.filter { !it.allDone },
            doneSummaries = summaries.filter { it.allDone }
        )
    }
}
