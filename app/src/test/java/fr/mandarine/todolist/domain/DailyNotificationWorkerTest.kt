package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DailyNotificationWorkerTest {

    private val repository: TodoListRepository = mockk()
    private val computeUseCase: ComputePendingNotificationsUseCase = mockk()
    private val listNotifier: ListNotifier = mockk(relaxed = true)
    private lateinit var worker: DailyNotificationWorker

    @Before
    fun setUp() {
        worker = DailyNotificationWorker(repository, computeUseCase, listNotifier)
    }

    @Test
    fun `should get all lists from repository when execute is called`() {
        every { repository.getAll() } returns emptyList()
        every { computeUseCase(emptyList()) } returns emptyList()
        worker.execute()
        verify { repository.getAll() }
    }

    @Test
    fun `should compute notifications from lists when execute is called`() {
        val lists = listOf(TodoList("1", "Work"))
        every { repository.getAll() } returns lists
        every { computeUseCase(lists) } returns emptyList()
        worker.execute()
        verify { computeUseCase(lists) }
    }

    @Test
    fun `should post computed notifications when execute is called`() {
        val list = TodoList("1", "Work", dueDate = LocalDate.now())
        val notifications = listOf(ListNotification.DueDateToday(list))
        every { repository.getAll() } returns listOf(list)
        every { computeUseCase(listOf(list)) } returns notifications
        worker.execute()
        verify { listNotifier.postNotifications(notifications) }
    }

    @Test
    fun `should post empty notifications list when no lists have matching dates`() {
        every { repository.getAll() } returns emptyList()
        every { computeUseCase(emptyList()) } returns emptyList()
        worker.execute()
        verify { listNotifier.postNotifications(emptyList()) }
    }
}
