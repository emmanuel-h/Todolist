package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTodoListsWithStatusUseCaseCountTest {

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
    fun `should return zero activeCount and zero completedCount for list with no items`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertEquals(0, result[0].activeCount)
        assertEquals(0, result[0].completedCount)
    }

    @Test
    fun `should return activeCount one and zero completedCount for list with single active item`() {
        val list = TodoList("list-1", "Groceries")
        val item = TodoItem("item-1", "Milk", "list-1", isCompleted = false)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(item)

        val result = useCase()

        assertEquals(1, result[0].activeCount)
        assertEquals(0, result[0].completedCount)
    }

    @Test
    fun `should return correct activeCount and zero completedCount for list with multiple active items`() {
        val list = TodoList("list-1", "Groceries")
        val item1 = TodoItem("item-1", "Milk", "list-1", isCompleted = false)
        val item2 = TodoItem("item-2", "Bread", "list-1", isCompleted = false)
        val item3 = TodoItem("item-3", "Eggs", "list-1", isCompleted = false)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(item1, item2, item3)

        val result = useCase()

        assertEquals(3, result[0].activeCount)
        assertEquals(0, result[0].completedCount)
    }

    @Test
    fun `should return zero activeCount and completedCount one for list with single completed item`() {
        val list = TodoList("list-1", "Groceries")
        val item = TodoItem("item-1", "Milk", "list-1", isCompleted = true, completedAt = 1000L)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(item)

        val result = useCase()

        assertEquals(0, result[0].activeCount)
        assertEquals(1, result[0].completedCount)
    }

    @Test
    fun `should return zero activeCount and correct completedCount for list with multiple completed items`() {
        val list = TodoList("list-1", "Groceries")
        val item1 = TodoItem("item-1", "Milk", "list-1", isCompleted = true, completedAt = 1000L)
        val item2 = TodoItem("item-2", "Bread", "list-1", isCompleted = true, completedAt = 2000L)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(item1, item2)

        val result = useCase()

        assertEquals(0, result[0].activeCount)
        assertEquals(2, result[0].completedCount)
    }

    @Test
    fun `should return correct activeCount and completedCount for list with mixed items`() {
        val list = TodoList("list-1", "Groceries")
        val active1 = TodoItem("item-1", "Milk", "list-1", isCompleted = false)
        val active2 = TodoItem("item-2", "Bread", "list-1", isCompleted = false)
        val active3 = TodoItem("item-3", "Eggs", "list-1", isCompleted = false)
        val completed1 = TodoItem("item-4", "Butter", "list-1", isCompleted = true, completedAt = 1000L)
        val completed2 = TodoItem("item-5", "Coffee", "list-1", isCompleted = true, completedAt = 2000L)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(active1, active2, active3, completed1, completed2)

        val result = useCase()

        assertEquals(3, result[0].activeCount)
        assertEquals(2, result[0].completedCount)
    }

    @Test
    fun `should compute activeCount and completedCount independently for each list`() {
        val listA = TodoList("list-a", "Groceries")
        val listB = TodoList("list-b", "Work")
        val activeA = TodoItem("item-1", "Milk", "list-a", isCompleted = false)
        val completedB = TodoItem("item-2", "Report", "list-b", isCompleted = true, completedAt = 1000L)
        every { todoListRepository.getAll() } returns listOf(listA, listB)
        every { todoRepository.getAllByListId("list-a") } returns listOf(activeA)
        every { todoRepository.getAllByListId("list-b") } returns listOf(completedB)

        val result = useCase()

        assertEquals(1, result[0].activeCount)
        assertEquals(0, result[0].completedCount)
        assertEquals(0, result[1].activeCount)
        assertEquals(1, result[1].completedCount)
    }

    @Test
    fun `should return activeCount two and completedCount one for list with two active and one completed`() {
        val list = TodoList("list-1", "Tasks")
        val active1 = TodoItem("item-1", "Task A", "list-1", isCompleted = false)
        val active2 = TodoItem("item-2", "Task B", "list-1", isCompleted = false)
        val completed = TodoItem("item-3", "Task C", "list-1", isCompleted = true, completedAt = 500L)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(active1, active2, completed)

        val result = useCase()

        assertEquals(2, result[0].activeCount)
        assertEquals(1, result[0].completedCount)
    }
}
