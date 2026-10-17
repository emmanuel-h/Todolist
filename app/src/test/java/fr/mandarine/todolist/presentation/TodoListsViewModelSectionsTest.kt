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
    fun `should order active lists by horizon, overdue first and undated last`() {
        val undated = summary("undated", ListHorizon.UNDATED)
        val later = summary("later", ListHorizon.LATER, targetDate = day.plusDays(20))
        val overdue = summary("overdue", ListHorizon.OVERDUE, dueDate = day.minusDays(2))
        val today = summary("today", ListHorizon.TODAY, targetDate = day)
        val week = summary("week", ListHorizon.THIS_WEEK, dueDate = day.plusDays(3))
        every { getTodoListsWithStatusUseCase() } returns listOf(undated, later, overdue, today, week)

        val state = currentState() as TodoListsState.Content

        assertEquals(
            listOf("overdue", "today", "week", "later", "undated"),
            state.activeSummaries.map { it.list.id }
        )
    }

    @Test
    fun `should order dated lists within a horizon by their date whichever kind it is`() {
        val far = summary("far", ListHorizon.LATER, targetDate = day.plusDays(40))
        val near = summary("near", ListHorizon.LATER, dueDate = day.plusDays(10))
        val middle = summary("middle", ListHorizon.LATER, targetDate = day.plusDays(20))
        every { getTodoListsWithStatusUseCase() } returns listOf(far, near, middle)

        val state = currentState() as TodoListsState.Content

        assertEquals(listOf("near", "middle", "far"), state.activeSummaries.map { it.list.id })
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
}
