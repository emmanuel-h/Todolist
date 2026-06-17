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

class TodoListsViewModelTest {

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
        viewModel = TodoListsViewModel(createTodoListUseCase, deleteTodoListUseCase, editTodoListUseCase, getTodoListsWithStatusUseCase, reorderTodoListsUseCase)
    }

    @Test
    fun `should return empty state when there are no lists`() {
        every { getTodoListsWithStatusUseCase() } returns emptyList()

        assertEquals(TodoListsState.Empty, viewModel.state)
    }

    @Test
    fun `should return content state when there are lists`() {
        val summary = TodoListSummary(TodoList("1", "Groceries"), allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary)

        assertEquals(
            TodoListsState.Content(activeSummaries = listOf(summary), doneSummaries = emptyList()),
            viewModel.state
        )
    }

    @Test
    fun `should place active summaries in activeSummaries`() {
        val summary = TodoListSummary(TodoList("1", "Groceries"), allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary)

        val content = viewModel.state as TodoListsState.Content
        assertEquals(listOf(summary), content.activeSummaries)
        assertEquals(emptyList<TodoListSummary>(), content.doneSummaries)
    }

    @Test
    fun `should place done summaries in doneSummaries`() {
        val summary = TodoListSummary(TodoList("1", "Groceries"), allDone = true)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary)

        val content = viewModel.state as TodoListsState.Content
        assertEquals(emptyList<TodoListSummary>(), content.activeSummaries)
        assertEquals(listOf(summary), content.doneSummaries)
    }

    @Test
    fun `should split mixed summaries into activeSummaries and doneSummaries`() {
        val active = TodoListSummary(TodoList("1", "Groceries"), allDone = false)
        val done = TodoListSummary(TodoList("2", "Home"), allDone = true)
        every { getTodoListsWithStatusUseCase() } returns listOf(active, done)

        val content = viewModel.state as TodoListsState.Content
        assertEquals(listOf(active), content.activeSummaries)
        assertEquals(listOf(done), content.doneSummaries)
    }

    @Test
    fun `should preserve original order within activeSummaries`() {
        val active1 = TodoListSummary(TodoList("1", "Alpha"), allDone = false)
        val active2 = TodoListSummary(TodoList("2", "Beta"), allDone = false)
        val active3 = TodoListSummary(TodoList("3", "Gamma"), allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(active1, active2, active3)

        val content = viewModel.state as TodoListsState.Content
        assertEquals(listOf(active1, active2, active3), content.activeSummaries)
    }

    @Test
    fun `should preserve original order within doneSummaries`() {
        val done1 = TodoListSummary(TodoList("1", "Alpha"), allDone = true)
        val done2 = TodoListSummary(TodoList("2", "Beta"), allDone = true)
        val done3 = TodoListSummary(TodoList("3", "Gamma"), allDone = true)
        every { getTodoListsWithStatusUseCase() } returns listOf(done1, done2, done3)

        val content = viewModel.state as TodoListsState.Content
        assertEquals(listOf(done1, done2, done3), content.doneSummaries)
    }

    @Test
    fun `should preserve interleaved order when splitting mixed list`() {
        val active1 = TodoListSummary(TodoList("1", "Active1"), allDone = false)
        val done1 = TodoListSummary(TodoList("2", "Done1"), allDone = true)
        val active2 = TodoListSummary(TodoList("3", "Active2"), allDone = false)
        val done2 = TodoListSummary(TodoList("4", "Done2"), allDone = true)
        every { getTodoListsWithStatusUseCase() } returns listOf(active1, done1, active2, done2)

        val content = viewModel.state as TodoListsState.Content
        assertEquals(listOf(active1, active2), content.activeSummaries)
        assertEquals(listOf(done1, done2), content.doneSummaries)
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
