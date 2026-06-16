package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.TodoList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TodoListsViewModelTest {

    private lateinit var createTodoListUseCase: CreateTodoListUseCase
    private lateinit var deleteTodoListUseCase: DeleteTodoListUseCase
    private lateinit var editTodoListUseCase: EditTodoListUseCase
    private lateinit var getTodoListsUseCase: GetTodoListsUseCase
    private lateinit var reorderTodoListsUseCase: ReorderTodoListsUseCase
    private lateinit var viewModel: TodoListsViewModel

    @Before
    fun setUp() {
        createTodoListUseCase = mockk(relaxed = true)
        deleteTodoListUseCase = mockk(relaxed = true)
        editTodoListUseCase = mockk(relaxed = true)
        getTodoListsUseCase = mockk()
        reorderTodoListsUseCase = mockk(relaxed = true)
        viewModel = TodoListsViewModel(createTodoListUseCase, deleteTodoListUseCase, editTodoListUseCase, getTodoListsUseCase, reorderTodoListsUseCase)
    }

    @Test
    fun `should return empty state when there are no lists`() {
        every { getTodoListsUseCase() } returns emptyList()

        assertEquals(TodoListsState.Empty, viewModel.state)
    }

    @Test
    fun `should return content state when there are lists`() {
        val lists = listOf(TodoList("1", "Groceries"))
        every { getTodoListsUseCase() } returns lists

        assertEquals(TodoListsState.Content(lists), viewModel.state)
    }

    @Test
    fun `should expose lists through content state`() {
        val lists = listOf(TodoList("1", "Groceries"), TodoList("2", "Home"))
        every { getTodoListsUseCase() } returns lists

        val content = viewModel.state as TodoListsState.Content
        assertEquals(lists, content.lists)
    }

    @Test
    fun `should delegate createList to use case with correct name`() {
        viewModel.createList("Groceries")

        verify { createTodoListUseCase("Groceries") }
    }

    @Test
    fun `should delegate deleteList to use case with correct id`() {
        viewModel.deleteList("list-1")

        verify { deleteTodoListUseCase("list-1") }
    }
}
