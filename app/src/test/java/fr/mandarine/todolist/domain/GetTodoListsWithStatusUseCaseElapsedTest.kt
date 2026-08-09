package fr.mandarine.todolist.domain

import fr.mandarine.todolist.FakeClock
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTodoListsWithStatusUseCaseElapsedTest {

    private lateinit var todoListRepository: TodoListRepository
    private lateinit var todoRepository: TodoRepository
    private lateinit var useCase: GetTodoListsWithStatusUseCase

    private val todayEpochDay = 100L
    private val today = LocalDate.ofEpochDay(todayEpochDay)
    private val yesterday = LocalDate.ofEpochDay(todayEpochDay - 1)
    private val tomorrow = LocalDate.ofEpochDay(todayEpochDay + 1)

    @Before
    fun setUp() {
        todoListRepository = mockk()
        todoRepository = mockk()
        every { todoRepository.countsByList() } returns emptyList()
        useCase = GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, FakeClock(todayDate = LocalDate.ofEpochDay(todayEpochDay)))
    }

    @Test
    fun `should return isTargetDateElapsed false when target date is null`() {
        val list = TodoList("list-1", "Groceries", targetDate = null)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertFalse(result[0].isTargetDateElapsed)
    }

    @Test
    fun `should return isTargetDateElapsed false when target date is today`() {
        val list = TodoList("list-1", "Groceries", targetDate = today)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertFalse(result[0].isTargetDateElapsed)
    }

    @Test
    fun `should return isTargetDateElapsed false when target date is in the future`() {
        val list = TodoList("list-1", "Groceries", targetDate = tomorrow)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertFalse(result[0].isTargetDateElapsed)
    }

    @Test
    fun `should return isTargetDateElapsed true when target date is in the past`() {
        val list = TodoList("list-1", "Groceries", targetDate = yesterday)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertTrue(result[0].isTargetDateElapsed)
    }

    @Test
    fun `should compute isTargetDateElapsed independently for each list`() {
        val listPast = TodoList("list-1", "Past", targetDate = yesterday)
        val listNull = TodoList("list-2", "NullDate", targetDate = null)
        val listFuture = TodoList("list-3", "Future", targetDate = tomorrow)
        every { todoListRepository.getAll() } returns listOf(listPast, listNull, listFuture)

        val result = useCase()

        assertTrue(result[0].isTargetDateElapsed)
        assertFalse(result[1].isTargetDateElapsed)
        assertFalse(result[2].isTargetDateElapsed)
    }

    @Test
    fun `should return isTargetDateElapsed false when list has no target date and items exist`() {
        val list = TodoList("list-1", "Groceries", targetDate = null)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 1, 0))

        val result = useCase()

        assertFalse(result[0].isTargetDateElapsed)
    }

    @Test
    fun `should return isTargetDateElapsed true for past date even when all items are done`() {
        val list = TodoList("list-1", "Groceries", targetDate = yesterday)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.countsByList() } returns listOf(TodoCounts("list-1", 0, 1))

        val result = useCase()

        assertTrue(result[0].isTargetDateElapsed)
    }

    @Test
    fun `should return showTargetYear false when target date is null`() {
        val list = TodoList("list-1", "Groceries", targetDate = null)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertFalse(result[0].showTargetYear)
    }

    @Test
    fun `should return showTargetYear false when target date is in the same year as today`() {
        val sameYear = LocalDate.ofEpochDay(todayEpochDay + 30)
        val list = TodoList("list-1", "Groceries", targetDate = sameYear)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertFalse(result[0].showTargetYear)
    }

    @Test
    fun `should return showTargetYear true when target date is in a different year from today`() {
        val differentYear = today.withYear(today.year + 1)
        val list = TodoList("list-1", "Groceries", targetDate = differentYear)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertTrue(result[0].showTargetYear)
    }

    @Test
    fun `should return showTargetYear true when target date year is before current year`() {
        val pastYear = today.withYear(today.year - 1)
        val list = TodoList("list-1", "Groceries", targetDate = pastYear)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertTrue(result[0].showTargetYear)
    }

    @Test
    fun `should compute showTargetYear independently for each list`() {
        val sameYear = LocalDate.ofEpochDay(todayEpochDay + 10)
        val diffYear = today.withYear(today.year + 2)
        val listA = TodoList("list-1", "SameYear", targetDate = sameYear)
        val listB = TodoList("list-2", "DiffYear", targetDate = diffYear)
        every { todoListRepository.getAll() } returns listOf(listA, listB)

        val result = useCase()

        assertFalse(result[0].showTargetYear)
        assertTrue(result[1].showTargetYear)
    }
}
