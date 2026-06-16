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

class TodoListsViewModelReorderTest {

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
        getTodoListsUseCase = mockk(relaxed = true)
        reorderTodoListsUseCase = mockk(relaxed = true)
        viewModel = TodoListsViewModel(
            createTodoListUseCase,
            deleteTodoListUseCase,
            editTodoListUseCase,
            getTodoListsUseCase,
            reorderTodoListsUseCase
        )
    }

    @Test
    fun `should delegate reorderLists to use case with fromIndex and toIndex`() {
        viewModel.reorderLists(0, 2)

        verify { reorderTodoListsUseCase(0, 2) }
    }

    @Test
    fun `should delegate reorderLists to use case with another pair of indices`() {
        viewModel.reorderLists(1, 3)

        verify { reorderTodoListsUseCase(1, 3) }
    }

    @Test
    fun `should delegate reorderLists when moving upward`() {
        viewModel.reorderLists(2, 0)

        verify { reorderTodoListsUseCase(2, 0) }
    }

    @Test
    fun `should reflect updated order in state after reorderLists`() {
        val list1 = TodoList("1", "Groceries", position = 0)
        val list2 = TodoList("2", "Work", position = 1)
        every { getTodoListsUseCase() } returns listOf(list1, list2)

        viewModel.reorderLists(1, 0)

        val content = viewModel.state as TodoListsState.Content
        assertEquals(listOf(list1, list2), content.lists)
    }
}
