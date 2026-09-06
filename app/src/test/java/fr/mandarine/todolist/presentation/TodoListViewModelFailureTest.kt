package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * What happens when storage refuses. Every one of these used to end the process:
 * the write runs on a launched coroutine, and a launched coroutine that throws is
 * handed to the thread's default handler.
 */
class TodoListViewModelFailureTest {

    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var toggleTodoUseCase: ToggleTodoUseCase
    private lateinit var deleteTodoUseCase: DeleteTodoUseCase
    private lateinit var editTodoUseCase: EditTodoUseCase
    private lateinit var reorderTodosUseCase: ReorderTodosUseCase
    private lateinit var getTodoListsUseCase: GetTodoListsUseCase
    private lateinit var viewModel: TodoListViewModel

    private val stored = TodoItem("kept", "Kept", "list-1")

    @Before
    fun setUp() {
        addTodoUseCase = mockk()
        getTodosUseCase = mockk()
        toggleTodoUseCase = mockk(relaxed = true)
        deleteTodoUseCase = mockk(relaxed = true)
        editTodoUseCase = mockk(relaxed = true)
        reorderTodosUseCase = mockk(relaxed = true)
        getTodoListsUseCase = mockk()
        every { getTodoListsUseCase() } returns listOf(TodoList("list-1", "List"))
        every { getTodosUseCase("list-1") } returns listOf(stored)
        viewModel = TodoListViewModel(
            addTodoUseCase,
            getTodosUseCase,
            toggleTodoUseCase,
            deleteTodoUseCase,
            editTodoUseCase,
            reorderTodosUseCase,
            getTodoListsUseCase,
            listId = "list-1",
            dispatcher = Dispatchers.Unconfined
        )
        viewModel.refresh()
    }

    @Test
    fun `should keep the page as stored when the write is refused`() {
        every { addTodoUseCase(any(), any()) } throws IllegalStateException("disk full")

        viewModel.addTodo("Milk")

        assertEquals(
            TodoListState.Content(listOf(stored), emptyList()),
            viewModel.state.value
        )
    }

    @Test
    fun `should keep the page as stored when the edit is refused`() {
        every { editTodoUseCase(any(), any()) } throws IllegalStateException("disk full")

        viewModel.editTodo("kept", "Renamed")

        assertEquals(
            TodoListState.Content(listOf(stored), emptyList()),
            viewModel.state.value
        )
    }

    @Test
    fun `should keep the page as stored when the reorder is refused`() {
        every { reorderTodosUseCase(any(), any()) } throws IllegalStateException("disk full")

        viewModel.reorderTodos(listOf("kept"))

        assertEquals(
            TodoListState.Content(listOf(stored), emptyList()),
            viewModel.state.value
        )
    }

    @Test
    fun `should keep the page as stored when the delete is refused`() {
        every { deleteTodoUseCase(any()) } throws IllegalStateException("disk full")

        viewModel.deleteTodo("kept")

        assertEquals(
            TodoListState.Content(listOf(stored), emptyList()),
            viewModel.state.value
        )
    }

    @Test
    fun `should keep the page as stored when the toggle is refused`() {
        every { toggleTodoUseCase(any()) } throws IllegalStateException("disk full")

        viewModel.toggleTodo("kept")

        assertEquals(
            TodoListState.Content(listOf(stored), emptyList()),
            viewModel.state.value
        )
    }

    /**
     * The page that was readable stays on screen. Publishing what cannot be read is
     * the only other option and there is nothing to publish.
     */
    @Test
    fun `should keep the last readable page when the re-read is refused`() {
        val before = viewModel.state.value
        every { getTodosUseCase("list-1") } throws IllegalStateException("database gone")

        viewModel.refresh()

        assertEquals(before, viewModel.state.value)
    }

    @Test
    fun `should keep the last readable page when the list check is refused`() {
        val before = viewModel.state.value
        every { getTodoListsUseCase() } throws IllegalStateException("database gone")

        viewModel.refresh()

        assertEquals(before, viewModel.state.value)
    }

    /**
     * A row torn off on another screen between the tap and this call is no longer
     * stored, and a tick on nothing is not a restore.
     */
    @Test
    fun `should treat a row that is no longer stored as unticked when it is toggled`() {
        viewModel.toggleTodo("gone")

        assertEquals(
            TodoListState.Content(listOf(stored), emptyList()),
            viewModel.state.value
        )
    }
}
