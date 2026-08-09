package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import io.mockk.verify
import java.time.LocalDate
import org.junit.Before
import org.junit.Test

class TodoListsViewModelDueDateTest {

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
            reorderTodoListsUseCase,
            Dispatchers.Unconfined
        )
    }

    @Test
    fun `should pass due date to use case when createList is called with a due date`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        viewModel.createList("Groceries", dueDate = dueDate)

        verify { createTodoListUseCase("Groceries", null, dueDate) }
    }

    @Test
    fun `should pass null due date to use case when createList is called without a due date`() {
        viewModel.createList("Groceries")

        verify { createTodoListUseCase("Groceries", null, null) }
    }

    @Test
    fun `should pass due date to use case when editList is called with a due date`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        viewModel.editList("list-1", "Groceries", targetDate = null, dueDate = dueDate)

        verify { editTodoListUseCase("list-1", "Groceries", null, dueDate) }
    }

    @Test
    fun `should pass null due date to use case when editList is called with null due date`() {
        viewModel.editList("list-1", "Groceries", targetDate = null, dueDate = null)

        verify { editTodoListUseCase("list-1", "Groceries", null, null) }
    }

    @Test
    fun `should not call editTodoListUseCase when name is blank even with due date`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        viewModel.editList("list-1", "   ", targetDate = null, dueDate = dueDate)

        verify(exactly = 0) { editTodoListUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `should use null as default due date when createList is called with name and target date only`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        viewModel.createList("Groceries", targetDate = targetDate)

        verify { createTodoListUseCase("Groceries", targetDate, null) }
    }

    @Test
    fun `should use null as default due date when editList is called without due date`() {
        viewModel.editList("list-1", "Groceries", targetDate = null)

        verify { editTodoListUseCase("list-1", "Groceries", null, null) }
    }

    private fun currentState(): TodoListsState {
        viewModel.refresh()
        return viewModel.state.value
    }
}
