package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.SetListReminderTimeUseCase
import fr.mandarine.todolist.domain.SyncDailyChecksUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TodoListsViewModelReminderTimeTest {

    private lateinit var setListReminderTimeUseCase: SetListReminderTimeUseCase
    private lateinit var syncDailyChecksUseCase: SyncDailyChecksUseCase
    private lateinit var getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase
    private lateinit var viewModel: TodoListsViewModel

    @Before
    fun setUp() {
        setListReminderTimeUseCase = mockk(relaxed = true)
        syncDailyChecksUseCase = mockk(relaxed = true)
        getTodoListsWithStatusUseCase = mockk(relaxed = true)
        every { getTodoListsWithStatusUseCase() } returns emptyList()
        viewModel = TodoListsViewModel(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            getTodoListsWithStatusUseCase,
            mockk(relaxed = true),
            Dispatchers.Unconfined,
            setListReminderTimeUseCase = setListReminderTimeUseCase,
            syncDailyChecksUseCase = syncDailyChecksUseCase
        )
    }

    @Test
    fun `should delegate to setListReminderTimeUseCase when setListReminderTime is called`() {
        viewModel.setListReminderTime("list-1", 450)

        verify { setListReminderTimeUseCase("list-1", 450) }
    }

    @Test
    fun `should pass null to use case when setListReminderTime is called with null`() {
        viewModel.setListReminderTime("list-2", null)

        verify { setListReminderTimeUseCase("list-2", null) }
    }

    @Test
    fun `should call syncDailyChecksUseCase after setting the reminder time`() {
        viewModel.setListReminderTime("list-1", 450)

        verify { syncDailyChecksUseCase() }
    }

    @Test
    fun `should sync after setting when setListReminderTime is called`() {
        viewModel.setListReminderTime("list-1", 450)

        verifyOrder {
            setListReminderTimeUseCase("list-1", 450)
            syncDailyChecksUseCase()
        }
    }

    @Test
    fun `should republish state after setting the reminder time`() {
        val listWithTime = TodoList("1", "Groceries", reminderTime = LocalTime.of(7, 30))
        every { getTodoListsWithStatusUseCase() } returns listOf(
            TodoListSummary(listWithTime, allDone = false)
        )

        viewModel.setListReminderTime("1", 450)

        val content = viewModel.state.value as TodoListsState.Content
        val published = content.activeSummaries.first().list
        assertEquals("1", published.id)
        assertEquals(LocalTime.of(7, 30), published.reminderTime)
        assertEquals(7, published.reminderTime?.hour)
        assertEquals(30, published.reminderTime?.minute)
    }

    @Test
    fun `should republish list without reminder time after clearing it`() {
        val listNoTime = TodoList("2", "Home")
        every { getTodoListsWithStatusUseCase() } returns listOf(
            TodoListSummary(listNoTime, allDone = false)
        )

        viewModel.setListReminderTime("2", null)

        val content = viewModel.state.value as TodoListsState.Content
        assertNull(content.activeSummaries.first().list.reminderTime)
    }
}
