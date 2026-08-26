package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AnimationEvent
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TodoListsViewModelAnimationTest {

    private lateinit var createTodoListUseCase: CreateTodoListUseCase
    private lateinit var deleteTodoListUseCase: DeleteTodoListUseCase
    private lateinit var editTodoListUseCase: EditTodoListUseCase
    private lateinit var getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase
    private lateinit var reorderTodoListsUseCase: ReorderTodoListsUseCase
    private lateinit var viewModel: TodoListsViewModel

    @Before
    fun setUp() {
        createTodoListUseCase = mockk(relaxed = true)
        deleteTodoListUseCase = mockk(relaxed = true)
        editTodoListUseCase = mockk(relaxed = true)
        getTodoListsWithStatusUseCase = mockk()
        every { getTodoListsWithStatusUseCase() } returns emptyList()
        reorderTodoListsUseCase = mockk(relaxed = true)
        viewModel = TodoListsViewModel(
            createTodoListUseCase,
            deleteTodoListUseCase,
            editTodoListUseCase,
            getTodoListsWithStatusUseCase,
            reorderTodoListsUseCase,
            Dispatchers.Unconfined
        )
    }

    private fun collectEvents(block: () -> Unit): List<AnimationEvent> {
        val events = mutableListOf<AnimationEvent>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val job = scope.launch { viewModel.animationEvents.collect { events += it } }
        block()
        job.cancel()
        scope.cancel()
        return events
    }

    @Test
    fun `should emit ListAdded when createList is called`() {
        val events = collectEvents { viewModel.createList("Groceries") }

        assertEquals(listOf(AnimationEvent.ListAdded), events)
    }

    @Test
    fun `should emit ListAdded when createList is called with a target date`() {
        val events = collectEvents { viewModel.createList("Work", java.time.LocalDate.of(2026, 9, 1)) }

        assertEquals(listOf(AnimationEvent.ListAdded), events)
    }

    @Test
    fun `should emit ListAdded when submitInlineInput is called with a non-blank name`() {
        val events = collectEvents { viewModel.submitInlineInput("Weekend") }

        assertEquals(listOf(AnimationEvent.ListAdded), events)
    }

    @Test
    fun `should not emit any event when submitInlineInput is called with blank name`() {
        val events = collectEvents { viewModel.submitInlineInput("   ") }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when submitInlineInput is called with empty name`() {
        val events = collectEvents { viewModel.submitInlineInput("") }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when refresh is called`() {
        val events = collectEvents { viewModel.refresh() }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when deleteList is called`() {
        val events = collectEvents { viewModel.deleteList("list-1") }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when editList is called`() {
        val events = collectEvents { viewModel.editList("list-1", "Renamed", null) }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when editList is called with blank name`() {
        val events = collectEvents { viewModel.editList("list-1", "  ", null) }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when reorderLists is called`() {
        val events = collectEvents { viewModel.reorderLists(listOf("a", "b")) }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should update state after createList emits animation event`() {
        val list = TodoList("1", "Groceries")
        val summary = TodoListSummary(list, allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary)

        val events = collectEvents { viewModel.createList("Groceries") }

        assertEquals(listOf(AnimationEvent.ListAdded), events)
        assertEquals(
            TodoListsState.Content(listOf(summary), emptyList()),
            viewModel.state.value
        )
    }

    @Test
    fun `should update state after submitInlineInput emits animation event`() {
        val list = TodoList("2", "Weekend")
        val summary = TodoListSummary(list, allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary)

        val events = collectEvents { viewModel.submitInlineInput("Weekend") }

        assertEquals(listOf(AnimationEvent.ListAdded), events)
        assertEquals(
            TodoListsState.Content(listOf(summary), emptyList()),
            viewModel.state.value
        )
    }
}
