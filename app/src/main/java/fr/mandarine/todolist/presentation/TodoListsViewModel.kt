package fr.mandarine.todolist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.todolist.domain.AnimationEvent
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The page of lists, as state.
 *
 * ### The shape every method follows
 * There is no observable database query in this app. Every mutation goes through
 * one of the two private helpers below, and both do the same two things in order:
 * perform the write, then **re-read everything** and publish a fresh
 * [TodoListsState]. So the screen is never updated optimistically and never drifts
 * from what is stored; the cost is a full read per action, which for a notebook-
 * sized amount of data is nothing.
 *
 * [refresh] is that same cycle with an empty action — a plain re-read. The activity
 * calls it from `onResume`, which is what picks up a change made on the items page.
 *
 * ### Two flows, two jobs
 * - [state] is a `StateFlow`: what the page *is*. It always has a current value and
 *   a late collector immediately gets it.
 * - [animationEvents] is a `SharedFlow`: what just *happened*. Events must not be
 *   replayed to a late collector — a rotation should not re-run the flourish — and
 *   they are not part of the page's state, so they are a separate channel.
 *   `DROP_OLDEST` means a burst of taps never suspends the writer waiting for the
 *   UI to catch up.
 *
 * ### The two scopes
 * `viewModelScope` is cancelled when this ViewModel dies. That is right for reads
 * and for anything the reader can see the result of. It is wrong for a delete, and
 * [deleteList] explains why.
 */
class TodoListsViewModel(
    private val createTodoListUseCase: CreateTodoListUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val editTodoListUseCase: EditTodoListUseCase,
    private val getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase,
    private val reorderTodoListsUseCase: ReorderTodoListsUseCase,
    private val dispatcher: CoroutineDispatcher,
    private val writeScope: CoroutineScope? = null
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

    fun createList(name: String, targetDate: LocalDate? = null, dueDate: LocalDate? = null, colour: ListColour = ListColour.None) {
        applyAndPublishWithEvent {
            createTodoListUseCase(name, targetDate, dueDate, colour)
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

    /**
     * A delete is written on a scope the composition root owns, not on this
     * view model's own. A window torn down mid-slip — a rotation is one — took the
     * delete with it and the row came back on the next read.
     */
    fun deleteList(todoListId: String) {
        applyAndPublish(writeScope ?: viewModelScope) { deleteTodoListUseCase(todoListId) }
    }

    fun editList(todoListId: String, newName: String, targetDate: LocalDate?, dueDate: LocalDate? = null, colour: ListColour = ListColour.None) {
        if (newName.isBlank()) return
        applyAndPublish { editTodoListUseCase(todoListId, newName, targetDate, dueDate, colour) }
    }

    fun reorderLists(orderedActiveIds: List<String>) {
        applyAndPublish { reorderTodoListsUseCase(orderedActiveIds) }
    }

    /**
     * Do the thing, then re-read and publish. [scope] defaults to this ViewModel's
     * own; a caller passes the composition root's scope when the write must outlive
     * the screen.
     *
     * The dispatcher is `AppContainer.databaseDispatcher`, a single-threaded
     * executor, so this is where the repository's blocking calls become legal and
     * where writes get serialised against each other.
     */
    private fun applyAndPublish(
        scope: CoroutineScope = viewModelScope,
        action: () -> Unit
    ) {
        scope.launch(dispatcher) {
            action()
            _state.value = buildState()
        }
    }

    /**
     * As [applyAndPublish], but the action also names something that just happened
     * for the UI to animate. The event is emitted **before** the new state, so the
     * page knows what is arriving before it is handed the page it arrives on.
     */
    private fun applyAndPublishWithEvent(action: () -> AnimationEvent) {
        viewModelScope.launch(dispatcher) {
            val event = action()
            _animationEvents.emit(event)
            _state.value = buildState()
        }
    }

    /**
     * The single definition of what the page of lists is: read every list with its
     * status, and split finished from unfinished. Both halves are computed here so
     * no composable ever filters while drawing.
     */
    private fun buildState(): TodoListsState {
        val summaries = getTodoListsWithStatusUseCase()
        if (summaries.isEmpty()) return TodoListsState.Empty
        return TodoListsState.Content(
            activeSummaries = summaries.filter { !it.allDone },
            doneSummaries = summaries.filter { it.allDone }
        )
    }
}
