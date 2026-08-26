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
import kotlinx.coroutines.Dispatchers
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
            reorderTodoListsUseCase,
            Dispatchers.Unconfined
        )
    }

    @Test
    fun `should hand the named order to the use case`() {
        viewModel.reorderLists(listOf("a", "b", "c"))

        verify { reorderTodoListsUseCase(listOf("a", "b", "c")) }
    }

    @Test
    fun `should hand a different named order to the use case`() {
        viewModel.reorderLists(listOf("c", "a", "b"))

        verify { reorderTodoListsUseCase(listOf("c", "a", "b")) }
    }

    @Test
    fun `should hand an order naming a single list to the use case`() {
        viewModel.reorderLists(listOf("only"))

        verify { reorderTodoListsUseCase(listOf("only")) }
    }

    @Test
    fun `should reflect updated order in activeSummaries after reorderLists`() {
        val list1 = TodoList("1", "Groceries", position = 0)
        val list2 = TodoList("2", "Work", position = 1)
        val summary1 = TodoListSummary(list1, allDone = false)
        val summary2 = TodoListSummary(list2, allDone = false)
        every { getTodoListsWithStatusUseCase() } returns listOf(summary1, summary2)

        viewModel.reorderLists(listOf("2", "1"))

        val content = currentState() as TodoListsState.Content
        assertEquals(listOf(summary1, summary2), content.activeSummaries)
        assertEquals(emptyList<TodoListSummary>(), content.doneSummaries)
    }

    private fun currentState(): TodoListsState {
        viewModel.refresh()
        return viewModel.state.value
    }
}
