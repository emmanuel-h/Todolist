package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.AnimationEvent
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A row torn off is the reader's decision the moment it tears; the undo slip is a
 * grace period, not a maybe. The page of items lives on an entry of the back
 * stack, so its own scope is exactly the thing that cannot be trusted to outlive
 * that decision — the composition root hands down one that can.
 */
class DurableDeleteTest {

    private val durable = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun `should write an item delete on the scope the composition root handed down`() {
        val deleteTodoUseCase = mockk<DeleteTodoUseCase>(relaxed = true)

        itemsViewModel(deleteTodoUseCase, durable).deleteTodo("item-1")

        verify { deleteTodoUseCase("item-1") }
    }

    @Test
    fun `should republish the page after a durable item delete`() {
        val getTodosUseCase = mockk<GetTodosUseCase>()
        every { getTodosUseCase("list-1") } returns listOf(TodoItem("kept", "Kept", "list-1"))
        val viewModel = itemsViewModel(mockk(relaxed = true), durable, getTodosUseCase)

        viewModel.deleteTodo("item-1")

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf("kept"), content.activeItems.map { it.id })
    }

    @Test
    fun `should still announce a durable item delete`() {
        val viewModel = itemsViewModel(mockk(relaxed = true), durable)
        val events = mutableListOf<AnimationEvent>()
        val job = durable.launch { viewModel.animationEvents.toList(events) }

        viewModel.deleteTodo("item-1")
        job.cancel()

        assertTrue(events.any { it is AnimationEvent.ItemDeleted })
    }

    @Test
    fun `should write an item delete on its own scope when none was handed down`() {
        val deleteTodoUseCase = mockk<DeleteTodoUseCase>(relaxed = true)

        itemsViewModel(deleteTodoUseCase, writeScope = null).deleteTodo("item-1")

        verify { deleteTodoUseCase("item-1") }
    }

    @Test
    fun `should write a list delete on the scope the composition root handed down`() {
        val deleteTodoListUseCase = mockk<DeleteTodoListUseCase>(relaxed = true)

        listsViewModel(deleteTodoListUseCase, durable).deleteList("list-1")

        verify { deleteTodoListUseCase("list-1") }
    }

    @Test
    fun `should republish the page after a durable list delete`() {
        val getTodoListsWithStatusUseCase = mockk<GetTodoListsWithStatusUseCase>()
        every { getTodoListsWithStatusUseCase() } returns
            listOf(TodoListSummary(TodoList("kept", "Kept"), allDone = false))
        val viewModel = listsViewModel(
            mockk(relaxed = true),
            durable,
            getTodoListsWithStatusUseCase
        )

        viewModel.deleteList("list-1")

        val content = viewModel.state.value as TodoListsState.Content
        assertEquals(listOf("kept"), content.activeSummaries.map { it.list.id })
    }

    @Test
    fun `should write a list delete on its own scope when none was handed down`() {
        val deleteTodoListUseCase = mockk<DeleteTodoListUseCase>(relaxed = true)

        listsViewModel(deleteTodoListUseCase, writeScope = null).deleteList("list-1")

        verify { deleteTodoListUseCase("list-1") }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun itemsViewModel(
        deleteTodoUseCase: DeleteTodoUseCase,
        writeScope: CoroutineScope?,
        getTodosUseCase: GetTodosUseCase = mockk<GetTodosUseCase>().also {
            every { it("list-1") } returns emptyList()
        }
    ): TodoListViewModel {
        val getTodoListsUseCase = mockk<GetTodoListsUseCase>()
        every { getTodoListsUseCase() } returns listOf(TodoList("list-1", "List"))
        return TodoListViewModel(
            mockk<AddTodoUseCase>(relaxed = true),
            getTodosUseCase,
            mockk<ToggleTodoUseCase>(relaxed = true),
            deleteTodoUseCase,
            mockk<EditTodoUseCase>(relaxed = true),
            mockk<ReorderTodosUseCase>(relaxed = true),
            getTodoListsUseCase,
            listId = "list-1",
            dispatcher = Dispatchers.Unconfined,
            writeScope = writeScope
        )
    }

    private fun listsViewModel(
        deleteTodoListUseCase: DeleteTodoListUseCase,
        writeScope: CoroutineScope?,
        getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase =
            mockk<GetTodoListsWithStatusUseCase>().also {
                every { it() } returns emptyList()
            }
    ): TodoListsViewModel = TodoListsViewModel(
        mockk<CreateTodoListUseCase>(relaxed = true),
        deleteTodoListUseCase,
        mockk<EditTodoListUseCase>(relaxed = true),
        getTodoListsWithStatusUseCase,
        mockk<ReorderTodoListsUseCase>(relaxed = true),
        dispatcher = Dispatchers.Unconfined,
        writeScope = writeScope
    )
}
