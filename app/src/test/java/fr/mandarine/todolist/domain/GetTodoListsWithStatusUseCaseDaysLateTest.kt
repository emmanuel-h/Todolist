package fr.mandarine.todolist.domain

import fr.mandarine.todolist.FakeClock
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTodoListsWithStatusUseCaseDaysLateTest {

    private lateinit var todoListRepository: TodoListRepository
    private lateinit var todoRepository: TodoRepository
    private lateinit var useCase: GetTodoListsWithStatusUseCase

    private val todayEpochDay = 200L
    private val today = LocalDate.ofEpochDay(todayEpochDay)

    @Before
    fun setUp() {
        todoListRepository = mockk()
        todoRepository = mockk()
        every { todoRepository.countsByList() } returns emptyList()
        every { todoRepository.previewsByList(GetTodoListsWithStatusUseCase.PREVIEW_ITEMS) } returns emptyList()
        useCase = GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, FakeClock(todayDate = today))
    }

    @Test
    fun `should return daysLate zero when list has no date`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(0, result[0].daysLate)
    }

    @Test
    fun `should return daysLate zero when target date is today`() {
        val list = TodoList("list-1", "Groceries", targetDate = today)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(0, result[0].daysLate)
    }

    @Test
    fun `should return daysLate zero when target date is in the future`() {
        val list = TodoList("list-1", "Groceries", targetDate = today.plusDays(5))
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(0, result[0].daysLate)
    }

    @Test
    fun `should return daysLate one when target date is yesterday`() {
        val list = TodoList("list-1", "Groceries", targetDate = today.minusDays(1))
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(1, result[0].daysLate)
    }

    @Test
    fun `should return daysLate three when target date is three days ago`() {
        val list = TodoList("list-1", "Groceries", targetDate = today.minusDays(3))
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(3, result[0].daysLate)
    }

    @Test
    fun `should return daysLate zero when due date is today`() {
        val list = TodoList("list-1", "Groceries", dueDate = today)
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(0, result[0].daysLate)
    }

    @Test
    fun `should return daysLate zero when due date is in the future`() {
        val list = TodoList("list-1", "Groceries", dueDate = today.plusDays(10))
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(0, result[0].daysLate)
    }

    @Test
    fun `should return daysLate seven when due date is seven days ago`() {
        val list = TodoList("list-1", "Groceries", dueDate = today.minusDays(7))
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(7, result[0].daysLate)
    }

    @Test
    fun `should compute daysLate independently for each list`() {
        val listLate = TodoList("list-1", "Late", targetDate = today.minusDays(4))
        val listOnTime = TodoList("list-2", "OnTime", targetDate = today.plusDays(2))
        val listUndated = TodoList("list-3", "Undated")
        every { todoListRepository.getAll() } returns listOf(listLate, listOnTime, listUndated)

        val result = useCase()

        assertEquals(4, result[0].daysLate)
        assertEquals(0, result[1].daysLate)
        assertEquals(0, result[2].daysLate)
    }

    @Test
    fun `should prefer target date over due date when both would be computed`() {
        val list = TodoList("list-1", "HasTarget", targetDate = today.minusDays(2))
        every { todoListRepository.getAll() } returns listOf(list)

        val result = useCase()

        assertEquals(2, result[0].daysLate)
    }
}
