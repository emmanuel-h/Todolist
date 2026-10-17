package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ListHorizon
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TodoListsViewModelSectionsTest {

    private lateinit var getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase
    private lateinit var viewModel: TodoListsViewModel

    private val day = LocalDate.of(2026, 10, 17)

    @Before
    fun setUp() {
        getTodoListsWithStatusUseCase = mockk()
        every { getTodoListsWithStatusUseCase() } returns emptyList()
        viewModel = TodoListsViewModel(
            createTodoListUseCase = mockk(relaxed = true),
            deleteTodoListUseCase = mockk(relaxed = true),
            editTodoListUseCase = mockk(relaxed = true),
            getTodoListsWithStatusUseCase = getTodoListsWithStatusUseCase,
            reorderTodoListsUseCase = mockk(relaxed = true),
            dispatcher = Dispatchers.Unconfined,
            setListReminderTimeUseCase = mockk(relaxed = true),
            syncDailyChecksUseCase = mockk(relaxed = true)
        )
    }

    private fun currentState(): TodoListsState {
        viewModel.refresh()
        return viewModel.state.value
    }

    private fun summary(
        id: String,
        horizon: ListHorizon,
        targetDate: LocalDate? = null,
        dueDate: LocalDate? = null,
        allDone: Boolean = false
    ) = TodoListSummary(
        list = TodoList(id, id, targetDate = targetDate, dueDate = dueDate),
        allDone = allDone,
        horizon = horizon
    )

    @Test
    fun `should keep active lists in reader order as returned by use case`() {
        val undated = summary("undated", ListHorizon.UNDATED)
        val later = summary("later", ListHorizon.LATER, targetDate = day.plusDays(20))
        val overdue = summary("overdue", ListHorizon.OVERDUE, dueDate = day.minusDays(2))
        val today = summary("today", ListHorizon.TODAY, targetDate = day)
        val week = summary("week", ListHorizon.THIS_WEEK, dueDate = day.plusDays(3))
        every { getTodoListsWithStatusUseCase() } returns listOf(undated, later, overdue, today, week)

        val state = currentState() as TodoListsState.Content

        assertEquals(
            listOf("undated", "later", "overdue", "today", "week"),
            state.activeSummaries.map { it.list.id }
        )
    }

    @Test
    fun `should order agenda by horizon then day`() {
        val undated = summary("undated", ListHorizon.UNDATED)
        val later = summary("later", ListHorizon.LATER, targetDate = day.plusDays(20))
        val overdue = summary("overdue", ListHorizon.OVERDUE, dueDate = day.minusDays(2))
        val today = summary("today", ListHorizon.TODAY, targetDate = day)
        val week = summary("week", ListHorizon.THIS_WEEK, dueDate = day.plusDays(3))
        every { getTodoListsWithStatusUseCase() } returns listOf(undated, later, overdue, today, week)

        val state = currentState() as TodoListsState.Content

        assertEquals(
            listOf("overdue", "today", "week", "later"),
            state.agenda.map { it.list.id }
        )
    }

    @Test
    fun `should order dated lists within agenda by their date whichever kind it is`() {
        val far = summary("far", ListHorizon.LATER, targetDate = day.plusDays(40))
        val near = summary("near", ListHorizon.LATER, dueDate = day.plusDays(10))
        val middle = summary("middle", ListHorizon.LATER, targetDate = day.plusDays(20))
        every { getTodoListsWithStatusUseCase() } returns listOf(far, near, middle)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf("near", "middle", "far"), state.agenda.map { it.list.id })
    }

    @Test
    fun `should keep undated lists in the order the reader left them`() {
        val first = summary("first", ListHorizon.UNDATED)
        val second = summary("second", ListHorizon.UNDATED)
        val third = summary("third", ListHorizon.UNDATED)
        every { getTodoListsWithStatusUseCase() } returns listOf(first, second, third)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf("first", "second", "third"), state.activeSummaries.map { it.list.id })
    }

    @Test
    fun `should group active lists into one section per horizon that has any`() {
        val overdue = summary("overdue", ListHorizon.OVERDUE, dueDate = day.minusDays(1))
        val undatedA = summary("a", ListHorizon.UNDATED)
        val undatedB = summary("b", ListHorizon.UNDATED)
        every { getTodoListsWithStatusUseCase() } returns listOf(undatedA, overdue, undatedB)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf(ListHorizon.OVERDUE, ListHorizon.UNDATED), state.sections.map { it.horizon })
        assertEquals(listOf("overdue"), state.sections[0].summaries.map { it.list.id })
        assertEquals(listOf("a", "b"), state.sections[1].summaries.map { it.list.id })
    }

    @Test
    fun `should leave finished lists out of the dated sections`() {
        val done = summary("done", ListHorizon.TODAY, targetDate = day, allDone = true)
        val open = summary("open", ListHorizon.TODAY, targetDate = day)
        every { getTodoListsWithStatusUseCase() } returns listOf(done, open)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf("open"), state.sections.single().summaries.map { it.list.id })
        assertEquals(listOf("done"), state.doneSummaries.map { it.list.id })
    }

    @Test
    fun `should draw no section when every list is finished`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(
            summary("done", ListHorizon.UNDATED, allDone = true)
        )

        val state = currentState() as TodoListsState.Content

        assertEquals(emptyList<ListSection>(), state.sections)
    }

    @Test
    fun `should include overdue lists in thisWeek`() {
        val overdue = summary("overdue", ListHorizon.OVERDUE, dueDate = day.minusDays(2))
        every { getTodoListsWithStatusUseCase() } returns listOf(overdue)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf("overdue"), state.thisWeek.map { it.list.id })
    }

    @Test
    fun `should include today lists in thisWeek`() {
        val today = summary("today", ListHorizon.TODAY, targetDate = day)
        every { getTodoListsWithStatusUseCase() } returns listOf(today)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf("today"), state.thisWeek.map { it.list.id })
    }

    @Test
    fun `should include this_week lists in thisWeek`() {
        val week = summary("week", ListHorizon.THIS_WEEK, dueDate = day.plusDays(3))
        every { getTodoListsWithStatusUseCase() } returns listOf(week)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf("week"), state.thisWeek.map { it.list.id })
    }

    @Test
    fun `should exclude later lists from thisWeek`() {
        val later = summary("later", ListHorizon.LATER, targetDate = day.plusDays(20))
        every { getTodoListsWithStatusUseCase() } returns listOf(later)

        val state = currentState() as TodoListsState.Content

        assertEquals(emptyList<TodoListSummary>(), state.thisWeek)
    }

    @Test
    fun `should exclude undated lists from thisWeek`() {
        val undated = summary("undated", ListHorizon.UNDATED)
        every { getTodoListsWithStatusUseCase() } returns listOf(undated)

        val state = currentState() as TodoListsState.Content

        assertEquals(emptyList<TodoListSummary>(), state.thisWeek)
    }

    @Test
    fun `should return undatedCount of zero when all lists are dated`() {
        val today = summary("today", ListHorizon.TODAY, targetDate = day)
        every { getTodoListsWithStatusUseCase() } returns listOf(today)

        val state = currentState() as TodoListsState.Content

        assertEquals(0, state.undatedCount)
    }

    @Test
    fun `should return correct undatedCount for mixed dated and undated lists`() {
        val undatedA = summary("a", ListHorizon.UNDATED)
        val undatedB = summary("b", ListHorizon.UNDATED)
        val dated = summary("c", ListHorizon.TODAY, targetDate = day)
        every { getTodoListsWithStatusUseCase() } returns listOf(undatedA, undatedB, dated)

        val state = currentState() as TodoListsState.Content

        assertEquals(2, state.undatedCount)
    }

    @Test
    fun `should return undatedCount including all undated active lists`() {
        val undated1 = summary("a", ListHorizon.UNDATED)
        val undated2 = summary("b", ListHorizon.UNDATED)
        val undated3 = summary("c", ListHorizon.UNDATED)
        every { getTodoListsWithStatusUseCase() } returns listOf(undated1, undated2, undated3)

        val state = currentState() as TodoListsState.Content

        assertEquals(3, state.undatedCount)
    }

    @Test
    fun `should not count finished undated lists in undatedCount`() {
        val undatedDone = summary("done", ListHorizon.UNDATED, allDone = true)
        val undatedActive = summary("active", ListHorizon.UNDATED)
        every { getTodoListsWithStatusUseCase() } returns listOf(undatedDone, undatedActive)

        val state = currentState() as TodoListsState.Content

        assertEquals(1, state.undatedCount)
    }

    @Test
    fun `should keep agenda in horizon then date order across multiple horizons`() {
        val overdue1 = summary("o1", ListHorizon.OVERDUE, dueDate = day.minusDays(3))
        val overdue2 = summary("o2", ListHorizon.OVERDUE, dueDate = day.minusDays(1))
        val todaySummary = summary("t", ListHorizon.TODAY, targetDate = day)
        val week = summary("w", ListHorizon.THIS_WEEK, dueDate = day.plusDays(2))
        val later = summary("l", ListHorizon.LATER, targetDate = day.plusDays(30))
        every { getTodoListsWithStatusUseCase() } returns listOf(later, week, overdue2, todaySummary, overdue1)

        val state = currentState() as TodoListsState.Content

        assertEquals(
            listOf("o1", "o2", "t", "w", "l"),
            state.agenda.map { it.list.id }
        )
    }

    @Test
    fun `should return empty agenda when all active lists are undated`() {
        val undated = summary("undated", ListHorizon.UNDATED)
        every { getTodoListsWithStatusUseCase() } returns listOf(undated)

        val state = currentState() as TodoListsState.Content

        assertEquals(emptyList<TodoListSummary>(), state.agenda)
    }

    @Test
    fun `should include all three near horizons in thisWeek order`() {
        val overdue = summary("o", ListHorizon.OVERDUE, dueDate = day.minusDays(1))
        val today = summary("t", ListHorizon.TODAY, targetDate = day)
        val week = summary("w", ListHorizon.THIS_WEEK, dueDate = day.plusDays(4))
        every { getTodoListsWithStatusUseCase() } returns listOf(week, today, overdue)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf("o", "t", "w"), state.thisWeek.map { it.list.id })
    }
}
