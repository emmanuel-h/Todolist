package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test

class TodoListsViewModelColourTest {

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

    @Test
    fun `should default to None colour when createList is called without colour`() {
        viewModel.createList("Groceries")

        verify { createTodoListUseCase("Groceries", null, null, ListColour.None) }
    }

    @Test
    fun `should pass Mint colour to use case when createList is called with Mint`() {
        viewModel.createList("Groceries", colour = ListColour.Mint)

        verify { createTodoListUseCase("Groceries", null, null, ListColour.Mint) }
    }

    @Test
    fun `should pass Sky colour to use case when createList is called with Sky`() {
        viewModel.createList("Groceries", colour = ListColour.Sky)

        verify { createTodoListUseCase("Groceries", null, null, ListColour.Sky) }
    }

    @Test
    fun `should pass Butter colour to use case when createList is called with Butter`() {
        viewModel.createList("Groceries", colour = ListColour.Butter)

        verify { createTodoListUseCase("Groceries", null, null, ListColour.Butter) }
    }

    @Test
    fun `should default to None colour when editList is called without colour`() {
        viewModel.editList("list-1", "Groceries", null)

        verify { editTodoListUseCase("list-1", "Groceries", null, null, ListColour.None) }
    }

    @Test
    fun `should pass Peach colour to use case when editList is called with Peach`() {
        viewModel.editList("list-1", "Groceries", null, colour = ListColour.Peach)

        verify { editTodoListUseCase("list-1", "Groceries", null, null, ListColour.Peach) }
    }

    @Test
    fun `should pass Lilac colour to use case when editList is called with Lilac`() {
        viewModel.editList("list-1", "Groceries", null, colour = ListColour.Lilac)

        verify { editTodoListUseCase("list-1", "Groceries", null, null, ListColour.Lilac) }
    }

    @Test
    fun `should pass Rose colour to use case when editList is called with Rose`() {
        viewModel.editList("list-1", "Groceries", null, colour = ListColour.Rose)

        verify { editTodoListUseCase("list-1", "Groceries", null, null, ListColour.Rose) }
    }
}
