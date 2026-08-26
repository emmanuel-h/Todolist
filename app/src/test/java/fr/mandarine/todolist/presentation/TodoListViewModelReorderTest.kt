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
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TodoListViewModelReorderTest {

    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var toggleTodoUseCase: ToggleTodoUseCase
    private lateinit var deleteTodoUseCase: DeleteTodoUseCase
    private lateinit var editTodoUseCase: EditTodoUseCase
    private lateinit var reorderTodosUseCase: ReorderTodosUseCase
    private lateinit var getTodoListsUseCase: GetTodoListsUseCase
    private lateinit var viewModel: TodoListViewModel

    @Before
    fun setUp() {
        addTodoUseCase = mockk(relaxed = true)
        getTodosUseCase = mockk()
        toggleTodoUseCase = mockk(relaxed = true)
        deleteTodoUseCase = mockk(relaxed = true)
        editTodoUseCase = mockk(relaxed = true)
        reorderTodosUseCase = mockk(relaxed = true)
        getTodoListsUseCase = mockk()
        every { getTodoListsUseCase() } returns listOf(TodoList("list-1", "List"))
        every { getTodosUseCase("list-1") } returns emptyList()
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
    fun `should hand the named order to the use case with the list it happened on`() {
        viewModel.reorderTodos(listOf("a", "b", "c"))

        verify { reorderTodosUseCase("list-1", listOf("a", "b", "c")) }
    }

    @Test
    fun `should hand a different named order to the use case`() {
        viewModel.reorderTodos(listOf("c", "a", "b"))

        verify { reorderTodosUseCase("list-1", listOf("c", "a", "b")) }
    }

    @Test
    fun `should refresh state after reorderTodos`() {
        val item1 = TodoItem("1", "Second", "list-1", position = 0)
        val item2 = TodoItem("2", "First", "list-1", position = 1)
        every { getTodosUseCase("list-1") } returns listOf(item1, item2)

        viewModel.reorderTodos(listOf("2", "1"))

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf(item1, item2), content.activeItems)
    }

    @Test
    fun `should pass the list it happened on to the use case`() {
        viewModel.reorderTodos(listOf("a", "b"))

        verify { reorderTodosUseCase("list-1", listOf("a", "b")) }
    }
}
