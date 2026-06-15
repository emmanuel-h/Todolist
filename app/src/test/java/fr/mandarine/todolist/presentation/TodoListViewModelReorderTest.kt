package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import io.mockk.every
import io.mockk.mockk
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
    private lateinit var viewModel: TodoListViewModel

    @Before
    fun setUp() {
        addTodoUseCase = mockk(relaxed = true)
        getTodosUseCase = mockk()
        toggleTodoUseCase = mockk(relaxed = true)
        deleteTodoUseCase = mockk(relaxed = true)
        editTodoUseCase = mockk(relaxed = true)
        reorderTodosUseCase = mockk(relaxed = true)
        every { getTodosUseCase("list-1") } returns emptyList()
        viewModel = TodoListViewModel(
            addTodoUseCase,
            getTodosUseCase,
            toggleTodoUseCase,
            deleteTodoUseCase,
            editTodoUseCase,
            reorderTodosUseCase,
            listId = "list-1"
        )
    }

    @Test
    fun `should delegate reorderTodos to use case with listId fromIndex and toIndex`() {
        viewModel.reorderTodos(0, 2)

        verify { reorderTodosUseCase("list-1", 0, 2) }
    }

    @Test
    fun `should delegate reorderTodos to use case with another fromIndex and toIndex`() {
        viewModel.reorderTodos(1, 3)

        verify { reorderTodosUseCase("list-1", 1, 3) }
    }

    @Test
    fun `should refresh state after reorderTodos`() {
        val item1 = TodoItem("1", "Second", "list-1", position = 0)
        val item2 = TodoItem("2", "First", "list-1", position = 1)
        every { getTodosUseCase("list-1") } returns listOf(item1, item2)

        viewModel.reorderTodos(1, 0)

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf(item1, item2), content.activeItems)
    }

    @Test
    fun `should pass listId to reorderTodos use case`() {
        viewModel.reorderTodos(0, 1)

        verify { reorderTodosUseCase("list-1", 0, 1) }
    }
}
