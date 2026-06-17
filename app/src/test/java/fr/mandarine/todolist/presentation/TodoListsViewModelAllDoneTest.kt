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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TodoListsViewModelAllDoneTest {

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
        getTodoListsWithStatusUseCase = mockk()
        reorderTodoListsUseCase = mockk(relaxed = true)
        every { getTodoListsWithStatusUseCase() } returns emptyList()
        viewModel = TodoListsViewModel(
            createTodoListUseCase,
            deleteTodoListUseCase,
            editTodoListUseCase,
            getTodoListsWithStatusUseCase,
            reorderTodoListsUseCase
        )
    }

    @Test
    fun `should return empty state when there are no lists`() {
        every { getTodoListsWithStatusUseCase() } returns emptyList()

        assertEquals(TodoListsState.Empty, viewModel.state)
    }

    @Test
    fun `should return content state with active summaries when list has allDone false`() {
        val list = TodoList("1", "Groceries")
        val summary = TodoListSummary(list, allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary)

        val state = viewModel.state as TodoListsState.Content
        assertEquals(listOf(summary), state.activeSummaries)
        assertEquals(emptyList<TodoListSummary>(), state.doneSummaries)
    }

    @Test
    fun `should expose allDone false in activeSummaries when list has no items`() {
        val list = TodoList("1", "Groceries")
        val summary = TodoListSummary(list, allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary)

        val state = viewModel.state as TodoListsState.Content
        assertFalse(state.activeSummaries[0].allDone)
    }

    @Test
    fun `should expose allDone true in doneSummaries when all items are completed`() {
        val list = TodoList("1", "Groceries")
        val summary = TodoListSummary(list, allDone = true)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary)

        val state = viewModel.state as TodoListsState.Content
        assertTrue(state.doneSummaries[0].allDone)
    }

    @Test
    fun `should separate allDone true into doneSummaries and allDone false into activeSummaries`() {
        val listA = TodoList("a", "All Done")
        val listB = TodoList("b", "Partial")
        val summaryA = TodoListSummary(listA, allDone = true)
        val summaryB = TodoListSummary(listB, allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summaryA, summaryB)

        val state = viewModel.state as TodoListsState.Content
        assertTrue(state.doneSummaries[0].allDone)
        assertFalse(state.activeSummaries[0].allDone)
    }
}
