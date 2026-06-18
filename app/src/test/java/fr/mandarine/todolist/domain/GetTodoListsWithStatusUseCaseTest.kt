package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTodoListsWithStatusUseCaseTest {

    private lateinit var todoListRepository: TodoListRepository
    private lateinit var todoRepository: TodoRepository
    private lateinit var useCase: GetTodoListsWithStatusUseCase

    @Before
    fun setUp() {
        todoListRepository = mockk()
        todoRepository = mockk()
        useCase = GetTodoListsWithStatusUseCase(todoListRepository, todoRepository)
    }

    @Test
    fun `should return empty list when there are no lists`() {
        every { todoListRepository.getAll() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return allDone false for a list with no items`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertEquals(1, result.size)
        assertFalse(result[0].allDone)
    }

    @Test
    fun `should return allDone false for a list with only active items`() {
        val list = TodoList("list-1", "Groceries")
        val item = TodoItem("item-1", "Milk", "list-1", isCompleted = false)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(item)

        val result = useCase()

        assertFalse(result[0].allDone)
    }

    @Test
    fun `should return allDone false for a list with mixed active and completed items`() {
        val list = TodoList("list-1", "Groceries")
        val active = TodoItem("item-1", "Milk", "list-1", isCompleted = false)
        val completed = TodoItem("item-2", "Bread", "list-1", isCompleted = true, completedAt = 1000L)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(active, completed)

        val result = useCase()

        assertFalse(result[0].allDone)
    }

    @Test
    fun `should return allDone true for a list where all items are completed`() {
        val list = TodoList("list-1", "Groceries")
        val item1 = TodoItem("item-1", "Milk", "list-1", isCompleted = true, completedAt = 1000L)
        val item2 = TodoItem("item-2", "Bread", "list-1", isCompleted = true, completedAt = 2000L)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(item1, item2)

        val result = useCase()

        assertTrue(result[0].allDone)
    }

    @Test
    fun `should return allDone true for a list with a single completed item`() {
        val list = TodoList("list-1", "Groceries")
        val item = TodoItem("item-1", "Milk", "list-1", isCompleted = true, completedAt = 1000L)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(item)

        val result = useCase()

        assertTrue(result[0].allDone)
    }

    @Test
    fun `should carry the TodoList inside the summary`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertEquals(list, result[0].list)
    }

    @Test
    fun `should compute allDone independently for each list`() {
        val listA = TodoList("list-a", "All Done List")
        val listB = TodoList("list-b", "Partial List")
        val doneItem = TodoItem("item-1", "Task", "list-a", isCompleted = true, completedAt = 1000L)
        val activeItem = TodoItem("item-2", "Task", "list-b", isCompleted = false)
        val completedItem = TodoItem("item-3", "Task Done", "list-b", isCompleted = true, completedAt = 2000L)
        every { todoListRepository.getAll() } returns listOf(listA, listB)
        every { todoRepository.getAllByListId("list-a") } returns listOf(doneItem)
        every { todoRepository.getAllByListId("list-b") } returns listOf(activeItem, completedItem)

        val result = useCase()

        assertEquals(2, result.size)
        assertTrue(result[0].allDone)
        assertFalse(result[1].allDone)
    }

    @Test
    fun `should preserve list order returned by repository`() {
        val list1 = TodoList("list-1", "First", position = 0)
        val list2 = TodoList("list-2", "Second", position = 1)
        every { todoListRepository.getAll() } returns listOf(list1, list2)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()
        every { todoRepository.getAllByListId("list-2") } returns emptyList()

        val result = useCase()

        assertEquals(list1, result[0].list)
        assertEquals(list2, result[1].list)
    }

}
