package fr.mandarine.todolist.domain

import fr.mandarine.todolist.FakeClock
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTodoListsWithStatusUseCaseHorizonTest {

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

    private fun horizonOfTarget(daysFromToday: Long): ListHorizon {
        val list = TodoList("list-1", "Groceries", targetDate = today.plusDays(daysFromToday))
        every { todoListRepository.getAll() } returns listOf(list)
        return useCase()[0].horizon
    }

    private fun horizonOfDue(daysFromToday: Long): ListHorizon {
        val list = TodoList("list-1", "Groceries", dueDate = today.plusDays(daysFromToday))
        every { todoListRepository.getAll() } returns listOf(list)
        return useCase()[0].horizon
    }

    @Test
    fun `should place a list without a date on the undated horizon`() {
        every { todoListRepository.getAll() } returns listOf(TodoList("list-1", "Groceries"))

        assertEquals(ListHorizon.UNDATED, useCase()[0].horizon)
    }

    @Test
    fun `should place a target date before today on the overdue horizon`() {
        assertEquals(ListHorizon.OVERDUE, horizonOfTarget(-1))
    }

    @Test
    fun `should place a target date of today on the today horizon`() {
        assertEquals(ListHorizon.TODAY, horizonOfTarget(0))
    }

    @Test
    fun `should place a target date of tomorrow on the this week horizon`() {
        assertEquals(ListHorizon.THIS_WEEK, horizonOfTarget(1))
    }

    @Test
    fun `should place a target date six days ahead on the this week horizon`() {
        assertEquals(ListHorizon.THIS_WEEK, horizonOfTarget(6))
    }

    @Test
    fun `should place a target date seven days ahead on the later horizon`() {
        assertEquals(ListHorizon.LATER, horizonOfTarget(7))
    }

    @Test
    fun `should place a due date before today on the overdue horizon`() {
        assertEquals(ListHorizon.OVERDUE, horizonOfDue(-3))
    }

    @Test
    fun `should place a due date of today on the today horizon`() {
        assertEquals(ListHorizon.TODAY, horizonOfDue(0))
    }

    @Test
    fun `should place a due date six days ahead on the this week horizon`() {
        assertEquals(ListHorizon.THIS_WEEK, horizonOfDue(6))
    }

    @Test
    fun `should place a due date seven days ahead on the later horizon`() {
        assertEquals(ListHorizon.LATER, horizonOfDue(7))
    }

    @Test
    fun `should compute the horizon independently for each list`() {
        val undated = TodoList("list-1", "Undated")
        val dueToday = TodoList("list-2", "Due", dueDate = today)
        val later = TodoList("list-3", "Later", targetDate = today.plusDays(30))
        every { todoListRepository.getAll() } returns listOf(undated, dueToday, later)

        val result = useCase()

        assertEquals(ListHorizon.UNDATED, result[0].horizon)
        assertEquals(ListHorizon.TODAY, result[1].horizon)
        assertEquals(ListHorizon.LATER, result[2].horizon)
    }
}
