package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.AnimationEvent
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.ToggleTodoUseCase
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

class TodoListViewModelAnimationTest {

    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var toggleTodoUseCase: ToggleTodoUseCase
    private lateinit var deleteTodoUseCase: DeleteTodoUseCase
    private lateinit var editTodoUseCase: EditTodoUseCase
    private lateinit var reorderTodosUseCase: ReorderTodosUseCase
    private lateinit var getTodoListsUseCase: GetTodoListsUseCase
    private lateinit var viewModel: TodoListViewModel

    @Before
    fun setUp() {
        addTodoUseCase = mockk()
        getTodosUseCase = mockk()
        toggleTodoUseCase = mockk(relaxed = true)
        deleteTodoUseCase = mockk(relaxed = true)
        editTodoUseCase = mockk(relaxed = true)
        reorderTodosUseCase = mockk(relaxed = true)
        getTodoListsUseCase = mockk()
        every { getTodoListsUseCase() } returns listOf(TodoList("list-1", "List"))
        every { getTodosUseCase("list-1") } returns emptyList()
        every { addTodoUseCase(any(), any()) } returns TodoItem("new-id", "title", "list-1")
        viewModel = TodoListViewModel(
            addTodoUseCase,
            getTodosUseCase,
            toggleTodoUseCase,
            deleteTodoUseCase,
            editTodoUseCase,
            reorderTodosUseCase,
            getTodoListsUseCase,
            listId = "list-1",
            dispatcher = Dispatchers.Unconfined
        )
        viewModel.refresh()
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
    fun `should emit ItemAdded with item id when addTodo is called`() {
        every { addTodoUseCase("Buy milk", "list-1") } returns TodoItem("item-42", "Buy milk", "list-1")

        val events = collectEvents { viewModel.addTodo("Buy milk") }

        assertEquals(listOf(AnimationEvent.ItemAdded("item-42")), events)
    }

    @Test
    fun `should emit ItemAdded with item id when submitInlineInput is called with non-blank title`() {
        every { addTodoUseCase("Apples", "list-1") } returns TodoItem("item-77", "Apples", "list-1")

        val events = collectEvents { viewModel.submitInlineInput("Apples") }

        assertEquals(listOf(AnimationEvent.ItemAdded("item-77")), events)
    }

    @Test
    fun `should not emit any event when submitInlineInput is called with blank title`() {
        val events = collectEvents { viewModel.submitInlineInput("   ") }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when submitInlineInput is called with empty title`() {
        val events = collectEvents { viewModel.submitInlineInput("") }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should emit ItemCompleted with item id when toggleTodo is called on an active item`() {
        val activeItem = TodoItem("item-1", "Active", "list-1")
        every { getTodosUseCase("list-1") } returns listOf(activeItem)
        viewModel.refresh()

        val events = collectEvents { viewModel.toggleTodo("item-1") }

        assertEquals(listOf(AnimationEvent.ItemCompleted("item-1")), events)
    }

    @Test
    fun `should emit ItemRestored with item id when toggleTodo is called on a completed item`() {
        val completedItem = TodoItem("item-2", "Done", "list-1", isCompleted = true, completedAt = 1000L)
        every { getTodosUseCase("list-1") } returns listOf(completedItem)
        viewModel.refresh()

        val events = collectEvents { viewModel.toggleTodo("item-2") }

        assertEquals(listOf(AnimationEvent.ItemRestored("item-2")), events)
    }

    @Test
    fun `should emit ItemCompleted when toggleTodo is called and state is empty (item not found in completed)`() {
        every { getTodosUseCase("list-1") } returns emptyList()
        viewModel.refresh()

        val events = collectEvents { viewModel.toggleTodo("ghost-id") }

        assertEquals(listOf(AnimationEvent.ItemCompleted("ghost-id")), events)
    }

    @Test
    fun `should emit ItemCompleted when toggleTodo is called and state is not Content`() {
        every { getTodoListsUseCase() } returns emptyList()
        viewModel.refresh()

        val events = collectEvents { viewModel.toggleTodo("item-x") }

        assertEquals(listOf(AnimationEvent.ItemCompleted("item-x")), events)
    }

    @Test
    fun `should emit ItemDeleted with item id when deleteTodo is called`() {
        val events = collectEvents { viewModel.deleteTodo("item-99") }

        assertEquals(listOf(AnimationEvent.ItemDeleted("item-99")), events)
    }

    @Test
    fun `should not emit any event when editTodo is called`() {
        val events = collectEvents { viewModel.editTodo("item-1", "New title") }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when refresh is called`() {
        val events = collectEvents { viewModel.refresh() }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should not emit any event when reorderTodos is called`() {
        val events = collectEvents { viewModel.reorderTodos(0, 1) }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `should emit ItemCompleted with the exact id passed to toggleTodo for an active item`() {
        val activeItem = TodoItem("exact-id-abc", "Task", "list-1")
        every { getTodosUseCase("list-1") } returns listOf(activeItem)
        viewModel.refresh()

        val events = collectEvents { viewModel.toggleTodo("exact-id-abc") }

        assertEquals(AnimationEvent.ItemCompleted("exact-id-abc"), events.single())
    }

    @Test
    fun `should emit ItemRestored with the exact id passed to toggleTodo for a completed item`() {
        val completedItem = TodoItem("exact-id-xyz", "Done task", "list-1", isCompleted = true, completedAt = 500L)
        every { getTodosUseCase("list-1") } returns listOf(completedItem)
        viewModel.refresh()

        val events = collectEvents { viewModel.toggleTodo("exact-id-xyz") }

        assertEquals(AnimationEvent.ItemRestored("exact-id-xyz"), events.single())
    }

    @Test
    fun `should emit ItemDeleted with the exact id passed to deleteTodo`() {
        val events = collectEvents { viewModel.deleteTodo("specific-id-789") }

        assertEquals(AnimationEvent.ItemDeleted("specific-id-789"), events.single())
    }

    @Test
    fun `should emit ItemCompleted when toggleTodo called on active item while completed items exist`() {
        val activeItem = TodoItem("item-active", "Active", "list-1")
        val completedItem = TodoItem("item-done", "Done", "list-1", isCompleted = true, completedAt = 1000L)
        every { getTodosUseCase("list-1") } returns listOf(activeItem, completedItem)
        viewModel.refresh()

        val events = collectEvents { viewModel.toggleTodo("item-active") }

        assertEquals(listOf(AnimationEvent.ItemCompleted("item-active")), events)
    }
}
