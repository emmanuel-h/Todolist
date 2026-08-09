package fr.mandarine.todolist.domain

import fr.mandarine.todolist.FakeClock
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
        every { todoRepository.countsByList() } returns emptyList()
        useCase = GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, FakeClock())
    }

    @Test
    fun `should return zero activeCount and zero completedCount for list with no items`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(0, result[0].activeCount)
        assertEquals(0, result[0].completedCount)
    }

    @Test
    fun `should return activeCount one and zero completedCount for list with single active item`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 1, 0))

        val result = useCase()

        assertEquals(1, result[0].activeCount)
        assertEquals(0, result[0].completedCount)
    }

    @Test
    fun `should return correct activeCount and zero completedCount for list with multiple active items`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 3, 0))

        val result = useCase()

        assertEquals(3, result[0].activeCount)
        assertEquals(0, result[0].completedCount)
    }

    @Test
    fun `should return zero activeCount and completedCount one for list with single completed item`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 0, 1))

        val result = useCase()

        assertEquals(0, result[0].activeCount)
        assertEquals(1, result[0].completedCount)
    }

    @Test
    fun `should return zero activeCount and correct completedCount for list with multiple completed items`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 0, 2))

        val result = useCase()

        assertEquals(0, result[0].activeCount)
        assertEquals(2, result[0].completedCount)
    }

    @Test
    fun `should return correct activeCount and completedCount for list with mixed items`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 3, 2))

        val result = useCase()

        assertEquals(3, result[0].activeCount)
        assertEquals(2, result[0].completedCount)
    }

    @Test
    fun `should compute activeCount and completedCount independently for each list`() {
        val listA = TodoList("list-a", "Groceries")
        val listB = TodoList("list-b", "Work")
        every { todoListRepository.getAll() } returns listOf(listA, listB)
        every { todoRepository.countsByList() } returns listOf(
            TodoCounts("list-a", 1, 0),
            TodoCounts("list-b", 0, 1)
        )

        val result = useCase()

        assertEquals(1, result[0].activeCount)
        assertEquals(0, result[0].completedCount)
        assertEquals(0, result[1].activeCount)
        assertEquals(1, result[1].completedCount)
    }

    @Test
    fun `should return activeCount two and completedCount one for list with two active and one completed`() {
        val list = TodoList("list-1", "Tasks")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 2, 1))

        val result = useCase()

        assertEquals(2, result[0].activeCount)
        assertEquals(1, result[0].completedCount)
    }
}
