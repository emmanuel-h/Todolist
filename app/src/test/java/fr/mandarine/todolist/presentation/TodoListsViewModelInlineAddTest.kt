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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TodoListsViewModelInlineAddTest {

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
        every { getTodoListsUseCase() } returns emptyList()
        viewModel = TodoListsViewModel(
            createTodoListUseCase,
            deleteTodoListUseCase,
            editTodoListUseCase,
            getTodoListsUseCase,
            reorderTodoListsUseCase
        )
    }

    @Test
    fun `should return true and delegate to use case when submitInlineInput is called with non-blank name`() {
        val result = viewModel.submitInlineInput("Groceries")

        assertTrue(result)
        verify { createTodoListUseCase("Groceries") }
    }

    @Test
    fun `should return false and not call use case when submitInlineInput is called with blank name`() {
        val result = viewModel.submitInlineInput("   ")

        assertFalse(result)
        verify(exactly = 0) { createTodoListUseCase(any()) }
    }

    @Test
    fun `should return false and not call use case when submitInlineInput is called with empty name`() {
        val result = viewModel.submitInlineInput("")

        assertFalse(result)
        verify(exactly = 0) { createTodoListUseCase(any()) }
    }

    @Test
    fun `should reflect new list in state after submitInlineInput`() {
        val list = TodoList("1", "Groceries")
        every { getTodoListsUseCase() } returns listOf(list)

        viewModel.submitInlineInput("Groceries")

        val content = viewModel.state as TodoListsState.Content
        assertEquals(listOf(list), content.lists)
    }

    @Test
    fun `should delegate submitInlineInput with trimmed name to use case`() {
        viewModel.submitInlineInput("Work")

        verify { createTodoListUseCase("Work") }
    }
}
