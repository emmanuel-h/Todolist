package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import org.junit.Before
import org.junit.Test

class TodoListsViewModelTargetDateTest {

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
    fun `should pass target date to use case when createList is called with a date`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        viewModel.createList("Groceries", targetDate)

        verify { createTodoListUseCase("Groceries", targetDate) }
    }

    @Test
    fun `should pass null target date to use case when createList is called without a date`() {
        viewModel.createList("Groceries", null)

        verify { createTodoListUseCase("Groceries", null) }
    }

    @Test
    fun `should pass target date to use case when editList is called with a date`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        viewModel.editList("list-1", "Groceries", targetDate)

        verify { editTodoListUseCase("list-1", "Groceries", targetDate) }
    }

    @Test
    fun `should pass null target date to use case when editList is called with null`() {
        viewModel.editList("list-1", "Groceries", null)

        verify { editTodoListUseCase("list-1", "Groceries", null) }
    }

    @Test
    fun `should not call editTodoListUseCase when name is blank even with target date`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        viewModel.editList("list-1", "   ", targetDate)

        verify(exactly = 0) { editTodoListUseCase(any(), any(), any()) }
    }

    @Test
    fun `should use null as default target date when createList is called with name only`() {
        viewModel.createList("Groceries")

        verify { createTodoListUseCase("Groceries", null) }
    }
}
