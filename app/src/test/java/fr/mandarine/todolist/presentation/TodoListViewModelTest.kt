package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TodoListViewModelTest {

    private lateinit var addTodoUseCase: AddTodoUseCase
    private lateinit var getTodosUseCase: GetTodosUseCase
    private lateinit var toggleTodoUseCase: ToggleTodoUseCase
    private lateinit var deleteTodoUseCase: DeleteTodoUseCase
    private lateinit var editTodoUseCase: EditTodoUseCase
    private lateinit var viewModel: TodoListViewModel

    @Before
    fun setUp() {
        addTodoUseCase = mockk(relaxed = true)
        getTodosUseCase = mockk()
        toggleTodoUseCase = mockk(relaxed = true)
        deleteTodoUseCase = mockk(relaxed = true)
        editTodoUseCase = mockk(relaxed = true)
        every { getTodosUseCase("list-1") } returns emptyList()
        viewModel = TodoListViewModel(
            addTodoUseCase,
            getTodosUseCase,
            toggleTodoUseCase,
            deleteTodoUseCase,
            editTodoUseCase,
            listId = "list-1"
        )
    }

    @Test
    fun `should emit empty state when there are no todos for the list`() {
        assertEquals(TodoListState.Empty, viewModel.state.value)
    }

    @Test
    fun `should emit content state when there are todos for the list`() {
        val items = listOf(TodoItem("1", "Item 1", "list-1"))
        every { getTodosUseCase("list-1") } returns items
        viewModel = TodoListViewModel(
            addTodoUseCase, getTodosUseCase, toggleTodoUseCase, deleteTodoUseCase, editTodoUseCase, listId = "list-1"
        )

        assertTrue(viewModel.state.value is TodoListState.Content)
    }

    @Test
    fun `should expose active items in content state when all items are incomplete`() {
        val items = listOf(TodoItem("1", "Item 1", "list-1"), TodoItem("2", "Item 2", "list-1"))
        every { getTodosUseCase("list-1") } returns items
        viewModel = TodoListViewModel(
            addTodoUseCase, getTodosUseCase, toggleTodoUseCase, deleteTodoUseCase, editTodoUseCase, listId = "list-1"
        )

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(items, content.activeItems)
        assertTrue(content.completedItems.isEmpty())
    }

    @Test
    fun `should expose completed items separately in content state when some items are completed`() {
        val active = TodoItem("1", "Item 1", "list-1")
        val completed = TodoItem("2", "Item 2", "list-1", isCompleted = true, completedAt = 1000L)
        every { getTodosUseCase("list-1") } returns listOf(active, completed)
        viewModel = TodoListViewModel(
            addTodoUseCase, getTodosUseCase, toggleTodoUseCase, deleteTodoUseCase, editTodoUseCase, listId = "list-1"
        )

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf(active), content.activeItems)
        assertEquals(listOf(completed), content.completedItems)
    }

    @Test
    fun `should order completed items by completedAt descending`() {
        val first = TodoItem("1", "First", "list-1", isCompleted = true, completedAt = 1000L)
        val second = TodoItem("2", "Second", "list-1", isCompleted = true, completedAt = 2000L)
        every { getTodosUseCase("list-1") } returns listOf(first, second)
        viewModel = TodoListViewModel(
            addTodoUseCase, getTodosUseCase, toggleTodoUseCase, deleteTodoUseCase, editTodoUseCase, listId = "list-1"
        )

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf(second, first), content.completedItems)
    }

    @Test
    fun `should emit content state with only completed items in completed section when all items are done`() {
        val completed1 = TodoItem("1", "Item 1", "list-1", isCompleted = true, completedAt = 1000L)
        val completed2 = TodoItem("2", "Item 2", "list-1", isCompleted = true, completedAt = 2000L)
        every { getTodosUseCase("list-1") } returns listOf(completed1, completed2)
        viewModel = TodoListViewModel(
            addTodoUseCase, getTodosUseCase, toggleTodoUseCase, deleteTodoUseCase, editTodoUseCase, listId = "list-1"
        )

        val content = viewModel.state.value as TodoListState.Content
        assertTrue(content.activeItems.isEmpty())
        assertEquals(listOf(completed2, completed1), content.completedItems)
    }

    @Test
    fun `should only show items belonging to the list`() {
        val items = listOf(TodoItem("1", "Item 1", "list-1"))
        every { getTodosUseCase("list-1") } returns items
        viewModel = TodoListViewModel(
            addTodoUseCase, getTodosUseCase, toggleTodoUseCase, deleteTodoUseCase, editTodoUseCase, listId = "list-1"
        )

        val content = viewModel.state.value as TodoListState.Content
        assertEquals("list-1", content.activeItems.first().listId)
    }

    @Test
    fun `should delegate add to use case with correct title and listId`() {
        viewModel.addTodo("Buy milk")

        verify { addTodoUseCase("Buy milk", "list-1") }
    }

    @Test
    fun `should return true and add item when submitInlineInput is called with non-blank title`() {
        val result = viewModel.submitInlineInput("Buy milk")

        assertTrue(result)
        verify { addTodoUseCase("Buy milk", "list-1") }
    }

    @Test
    fun `should return false and not add item when submitInlineInput is called with blank title`() {
        val result = viewModel.submitInlineInput("   ")

        assertFalse(result)
        verify(exactly = 0) { addTodoUseCase(any(), any()) }
    }

    @Test
    fun `should return false and not add item when submitInlineInput is called with empty title`() {
        val result = viewModel.submitInlineInput("")

        assertFalse(result)
        verify(exactly = 0) { addTodoUseCase(any(), any()) }
    }

    @Test
    fun `should refresh state after submitInlineInput adds a new item`() {
        val item = TodoItem("1", "Item 1", "list-1")
        every { getTodosUseCase("list-1") } returns listOf(item)

        viewModel.submitInlineInput("Item 1")

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf(item), content.activeItems)
    }

    @Test
    fun `should delegate toggleTodo to use case with given id`() {
        viewModel.toggleTodo("item-1")

        verify { toggleTodoUseCase("item-1") }
    }

    @Test
    fun `should refresh state after toggleTodo with completed item in completed section`() {
        val item = TodoItem("1", "Item 1", "list-1", isCompleted = true, completedAt = 1000L)
        every { getTodosUseCase("list-1") } returns listOf(item)

        viewModel.toggleTodo("1")

        val content = viewModel.state.value as TodoListState.Content
        assertTrue(content.completedItems.first().isCompleted)
        assertTrue(content.activeItems.isEmpty())
    }

    @Test
    fun `should emit empty state after toggleTodo when list becomes empty`() {
        every { getTodosUseCase("list-1") } returns emptyList()

        viewModel.toggleTodo("1")

        assertEquals(TodoListState.Empty, viewModel.state.value)
    }

    @Test
    fun `should keep active items in insertion order`() {
        val item1 = TodoItem("1", "First", "list-1")
        val item2 = TodoItem("2", "Second", "list-1")
        val item3 = TodoItem("3", "Third", "list-1")
        every { getTodosUseCase("list-1") } returns listOf(item1, item2, item3)
        viewModel = TodoListViewModel(
            addTodoUseCase, getTodosUseCase, toggleTodoUseCase, deleteTodoUseCase, editTodoUseCase, listId = "list-1"
        )

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf(item1, item2, item3), content.activeItems)
    }

    @Test
    fun `should delegate deleteTodo to use case with given id`() {
        viewModel.deleteTodo("item-1")

        verify { deleteTodoUseCase("item-1") }
    }

    @Test
    fun `should refresh state after deleteTodo`() {
        every { getTodosUseCase("list-1") } returns emptyList()

        viewModel.deleteTodo("item-1")

        assertEquals(TodoListState.Empty, viewModel.state.value)
    }

    @Test
    fun `should emit content state without deleted item after deleteTodo`() {
        val remaining = TodoItem("2", "Still here", "list-1")
        every { getTodosUseCase("list-1") } returns listOf(remaining)

        viewModel.deleteTodo("1")

        val content = viewModel.state.value as TodoListState.Content
        assertEquals(listOf(remaining), content.activeItems)
    }

    @Test
    fun `should delegate editTodo to use case with given id and new title`() {
        viewModel.editTodo("item-1", "Updated title")

        verify { editTodoUseCase("item-1", "Updated title") }
    }

    @Test
    fun `should refresh state after editTodo`() {
        val updated = TodoItem("1", "Updated title", "list-1")
        every { getTodosUseCase("list-1") } returns listOf(updated)

        viewModel.editTodo("1", "Updated title")

        val content = viewModel.state.value as TodoListState.Content
        assertEquals("Updated title", content.activeItems.first().title)
    }
}
