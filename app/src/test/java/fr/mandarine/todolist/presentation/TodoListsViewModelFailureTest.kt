package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * As `TodoListViewModelFailureTest`, for the page of lists.
 */
class TodoListsViewModelFailureTest {

    private lateinit var createTodoListUseCase: CreateTodoListUseCase
    private lateinit var deleteTodoListUseCase: DeleteTodoListUseCase
    private lateinit var editTodoListUseCase: EditTodoListUseCase
    private lateinit var getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase
    private lateinit var reorderTodoListsUseCase: ReorderTodoListsUseCase
    private lateinit var viewModel: TodoListsViewModel

    private val stored = TodoListSummary(TodoList("1", "Groceries"), allDone = false)

    @Before
    fun setUp() {
        createTodoListUseCase = mockk(relaxed = true)
        deleteTodoListUseCase = mockk(relaxed = true)
        editTodoListUseCase = mockk(relaxed = true)
        reorderTodoListsUseCase = mockk(relaxed = true)
        getTodoListsWithStatusUseCase = mockk()
        every { getTodoListsWithStatusUseCase() } returns listOf(stored)
        viewModel = TodoListsViewModel(
            createTodoListUseCase,
            deleteTodoListUseCase,
            editTodoListUseCase,
            getTodoListsWithStatusUseCase,
            reorderTodoListsUseCase,
            Dispatchers.Unconfined
        )
        viewModel.refresh()
    }

    @Test
    fun `should keep the page as stored when the delete is refused`() {
        every { deleteTodoListUseCase(any()) } throws IllegalStateException("disk full")

        viewModel.deleteList("1")

        assertEquals(TodoListsState.Content(listOf(stored), emptyList()), viewModel.state.value)
    }

    @Test
    fun `should keep the page as stored when the edit is refused`() {
        every { editTodoListUseCase(any(), any(), any(), any(), any()) } throws
            IllegalStateException("disk full")

        viewModel.editList("1", "Renamed", null, null, ListColour.None)

        assertEquals(TodoListsState.Content(listOf(stored), emptyList()), viewModel.state.value)
    }

    @Test
    fun `should keep the page as stored when the reorder is refused`() {
        every { reorderTodoListsUseCase(any()) } throws IllegalStateException("disk full")

        viewModel.reorderLists(listOf("1"))

        assertEquals(TodoListsState.Content(listOf(stored), emptyList()), viewModel.state.value)
    }

    @Test
    fun `should keep the page as stored when the write behind an event is refused`() {
        every { createTodoListUseCase(any()) } throws IllegalStateException("disk full")

        viewModel.createList("Chores")

        assertEquals(TodoListsState.Content(listOf(stored), emptyList()), viewModel.state.value)
    }

    @Test
    fun `should keep the last readable page when the re-read is refused`() {
        val before = viewModel.state.value
        every { getTodoListsWithStatusUseCase() } throws IllegalStateException("database gone")

        viewModel.refresh()

        assertEquals(before, viewModel.state.value)
    }
}
