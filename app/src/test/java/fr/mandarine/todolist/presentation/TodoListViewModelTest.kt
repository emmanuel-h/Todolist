package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TodoListViewModelTest {

    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var toggleTodoUseCase: ToggleTodoUseCase
    private lateinit var viewModel: TodoListViewModel

    @Before
    fun setUp() {
        addTodoUseCase = mockk(relaxed = true)
        getTodosUseCase = mockk()
        toggleTodoUseCase = mockk(relaxed = true)
        every { getTodosUseCase("list-1") } returns emptyList()
        viewModel = TodoListViewModel(addTodoUseCase, getTodosUseCase, toggleTodoUseCase, listId = "list-1")
    }

    @Test
    fun `should emit empty state when there are no todos for the list`() {
        assertEquals(TodoListState.Empty, viewModel.state.value)
    }

    @Test
    fun `should emit content state when there are todos for the list`() {
        val items = listOf(TodoItem("1", "Item 1", "list-1"))
        every { getTodosUseCase("list-1") } returns items
        viewModel = TodoListViewModel(addTodoUseCase, getTodosUseCase, toggleTodoUseCase, listId = "list-1")

        assertEquals(TodoListState.Content(items), viewModel.state.value)
    }

    @Test
    fun `should expose items list through content state`() {
        val items = listOf(TodoItem("1", "Item 1", "list-1"))
        every { getTodosUseCase("list-1") } returns items
        viewModel = TodoListViewModel(addTodoUseCase, getTodosUseCase, toggleTodoUseCase, listId = "list-1")

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(items, content.items)
    }

    @Test
    fun `should delegate add to use case with correct title and listId`() {
        viewModel.addTodo("Buy milk")

        verify { addTodoUseCase("Buy milk", "list-1") }
    }

    @Test
    fun `should only show items belonging to the list`() {
        val items = listOf(TodoItem("1", "Item 1", "list-1"))
        every { getTodosUseCase("list-1") } returns items
        viewModel = TodoListViewModel(addTodoUseCase, getTodosUseCase, toggleTodoUseCase, listId = "list-1")

        val content = viewModel.state.value as TodoListState.Content
        assertEquals("list-1", content.items.first().listId)
    }

    @Test
    fun `should return true and add item when submitInlineInput is called with non-blank title`() {
        val result = viewModel.submitInlineInput("Buy milk")

        assertTrue(result)
        verify { addTodoUseCase("Buy milk", "list-1") }
    }

    @Test
    fun `should return false and not add item when submitInlineInput is called with blank title`() {
        val result = viewModel.submitInlineInput("   ")

        assertFalse(result)
        verify(exactly = 0) { addTodoUseCase(any(), any()) }
    }

    @Test
    fun `should return false and not add item when submitInlineInput is called with empty title`() {
        val result = viewModel.submitInlineInput("")

        assertFalse(result)
        verify(exactly = 0) { addTodoUseCase(any(), any()) }
    }

    @Test
    fun `should refresh state after submitInlineInput adds a new item`() {
        val item = TodoItem("1", "Item 1", "list-1")
        every { getTodosUseCase("list-1") } returns listOf(item)

        viewModel.submitInlineInput("Item 1")

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf(item), content.items)
    }

    @Test
    fun `should delegate toggleTodo to use case with given id`() {
        viewModel.toggleTodo("item-1")

        verify { toggleTodoUseCase("item-1") }
    }

    @Test
    fun `should refresh state after toggleTodo`() {
        val item = TodoItem("1", "Item 1", "list-1", isCompleted = true)
        every { getTodosUseCase("list-1") } returns listOf(item)

        viewModel.toggleTodo("1")

        val content = viewModel.state.value as TodoListState.Content
        assertTrue(content.items.first().isCompleted)
    }

    @Test
    fun `should emit empty state after toggleTodo when list becomes empty`() {
        every { getTodosUseCase("list-1") } returns emptyList()

        viewModel.toggleTodo("1")

        assertEquals(TodoListState.Empty, viewModel.state.value)
    }
}
