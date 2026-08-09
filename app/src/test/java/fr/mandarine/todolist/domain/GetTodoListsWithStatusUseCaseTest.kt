package fr.mandarine.todolist.domain

import fr.mandarine.todolist.FakeClock
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
        every { todoRepository.countsByList() } returns emptyList()
        useCase = GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, FakeClock())
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

        val result = useCase()

        assertEquals(1, result.size)
        assertFalse(result[0].allDone)
    }

    @Test
    fun `should return allDone false for a list with only active items`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 1, 0))

        val result = useCase()

        assertFalse(result[0].allDone)
    }

    @Test
    fun `should return allDone false for a list with mixed active and completed items`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 1, 1))

        val result = useCase()

        assertFalse(result[0].allDone)
    }

    @Test
    fun `should return allDone true for a list where all items are completed`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 0, 2))

        val result = useCase()

        assertTrue(result[0].allDone)
    }

    @Test
    fun `should return allDone true for a list with a single completed item`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 0, 1))

        val result = useCase()

        assertTrue(result[0].allDone)
    }

    @Test
    fun `should carry the TodoList inside the summary`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(list, result[0].list)
    }

    @Test
    fun `should compute allDone independently for each list`() {
        val listA = TodoList("list-a", "All Done List")
        val listB = TodoList("list-b", "Partial List")
        every { todoListRepository.getAll() } returns listOf(listA, listB)
        every { todoRepository.countsByList() } returns listOf(
            TodoCounts("list-a", 0, 1),
            TodoCounts("list-b", 1, 1)
        )

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

        val result = useCase()

        assertEquals(list1, result[0].list)
        assertEquals(list2, result[1].list)
    }
}
