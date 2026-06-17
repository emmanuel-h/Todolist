package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
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
    fun `should reflect updated order in activeSummaries after reorderLists`() {
        val list1 = TodoList("1", "Groceries", position = 0)
        val list2 = TodoList("2", "Work", position = 1)
        val summary1 = TodoListSummary(list1, allDone = false)
        val summary2 = TodoListSummary(list2, allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary1, summary2)

        viewModel.reorderLists(1, 0)

        val content = viewModel.state as TodoListsState.Content
        assertEquals(listOf(summary1, summary2), content.activeSummaries)
        assertEquals(emptyList<TodoListSummary>(), content.doneSummaries)
    }
}
