package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TodoListViewModelTest {

    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var viewModel: TodoListViewModel

    @Before
    fun setUp() {
        addTodoUseCase = mockk(relaxed = true)
        getTodosUseCase = mockk()
        viewModel = TodoListViewModel(addTodoUseCase, getTodosUseCase)
    }

    @Test
    fun `should return empty state when there are no todos`() {
        every { getTodosUseCase() } returns emptyList()

        assertEquals(TodoListState.Empty, viewModel.state)
    }

    @Test
    fun `should return content state when there are todos`() {
        val items = listOf(TodoItem("1", "Item 1"))
        every { getTodosUseCase() } returns items

        assertEquals(TodoListState.Content(items), viewModel.state)
    }

    @Test
    fun `should expose items list through content state`() {
        val items = listOf(TodoItem("1", "Item 1"))
        every { getTodosUseCase() } returns items

        val content = viewModel.state as TodoListState.Content
        assertEquals(items, content.items)
    }

    @Test
    fun `should delegate add to use case with correct title`() {
        viewModel.addTodo("Buy milk")

        verify { addTodoUseCase("Buy milk") }
    }
}
