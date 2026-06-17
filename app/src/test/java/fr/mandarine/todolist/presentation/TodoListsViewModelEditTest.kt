package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class TodoListsViewModelEditTest {

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
        getTodoListsWithStatusUseCase = mockk(relaxed = true)
        reorderTodoListsUseCase = mockk(relaxed = true)
        viewModel = TodoListsViewModel(
            createTodoListUseCase,
            deleteTodoListUseCase,
            editTodoListUseCase,
            getTodoListsWithStatusUseCase,
            reorderTodoListsUseCase
        )
    }

    @Test
    fun `should delegate editList to use case with correct id and name`() {
        viewModel.editList("list-1", "Groceries")

        verify { editTodoListUseCase("list-1", "Groceries") }
    }

    @Test
    fun `should delegate editList with another id and name`() {
        viewModel.editList("list-42", "Work tasks")

        verify { editTodoListUseCase("list-42", "Work tasks") }
    }

    @Test
    fun `should not call use case when name is blank`() {
        viewModel.editList("list-1", "   ")

        verify(exactly = 0) { editTodoListUseCase(any(), any()) }
    }

    @Test
    fun `should not call use case when name is empty`() {
        viewModel.editList("list-1", "")

        verify(exactly = 0) { editTodoListUseCase(any(), any()) }
    }
}
