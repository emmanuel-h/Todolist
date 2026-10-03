package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Before
import org.junit.Test

class DailyNotificationWorkerTest {

    private val repository: TodoListRepository = mockk()
    private val computeUseCase: ComputePendingNotificationsUseCase = mockk()
    private val listNotifier: ListNotifier = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private lateinit var worker: DailyNotificationWorker

    @Before
    fun setUp() {
        worker = DailyNotificationWorker(repository, computeUseCase, listNotifier, notificationScheduler)
    }

    @Test
    fun `should get all lists from repository when execute is called`() {
        every { repository.getAll() } returns emptyList()
        every { computeUseCase(emptyList()) } returns emptyList()
        worker.execute(ReminderSlot.AppWide)
        verify { repository.getAll() }
    }

    @Test
    fun `should pass only app-wide lists to compute use case when slot is AppWide`() {
        val appWideList = TodoList("1", "AppWide")
        val ownTimeList = TodoList("2", "OwnTime", reminderTime = LocalTime.of(7, 30))
        every { repository.getAll() } returns listOf(appWideList, ownTimeList)
        every { computeUseCase(listOf(appWideList)) } returns emptyList()

        worker.execute(ReminderSlot.AppWide)

        verify { computeUseCase(listOf(appWideList)) }
    }

    @Test
    fun `should pass only matching own-time lists to compute use case when slot is At`() {
        val appWideList = TodoList("1", "AppWide")
        val slotTime = LocalTime.of(7, 30)
        val ownTimeList = TodoList("2", "OwnTime", reminderTime = slotTime)
        val otherOwnTimeList = TodoList("3", "OtherTime", reminderTime = LocalTime.of(9, 15))
        every { repository.getAll() } returns listOf(appWideList, ownTimeList, otherOwnTimeList)
        every { computeUseCase(listOf(ownTimeList)) } returns emptyList()

        worker.execute(ReminderSlot.At(slotTime))

        verify { computeUseCase(listOf(ownTimeList)) }
    }

    @Test
    fun `should post computed notifications when execute is called`() {
        val list = TodoList("1", "Work", dueDate = LocalDate.now())
        val notifications = listOf(ListNotification.DueDateToday(list))
        every { repository.getAll() } returns listOf(list)
        every { computeUseCase(listOf(list)) } returns notifications
        worker.execute(ReminderSlot.AppWide)
        verify { listNotifier.postNotifications(notifications) }
    }

    @Test
    fun `should post empty notifications list when no lists have matching dates`() {
        every { repository.getAll() } returns emptyList()
        every { computeUseCase(emptyList()) } returns emptyList()
        worker.execute(ReminderSlot.AppWide)
        verify { listNotifier.postNotifications(emptyList()) }
    }

    /**
     * A run that does not re-aim the next one is how a fixed twenty-four hour
     * period walks off the reader's hour and never comes back.
     */
    @Test
    fun `should aim the next check with AppWide slot when execute is called with AppWide`() {
        every { repository.getAll() } returns emptyList()
        every { computeUseCase(emptyList()) } returns emptyList()

        worker.execute(ReminderSlot.AppWide)

        verify { notificationScheduler.rescheduleDailyCheck(ReminderSlot.AppWide) }
    }

    @Test
    fun `should aim the next check with At slot when execute is called with At`() {
        val slotTime = LocalTime.of(7, 30)
        val slot = ReminderSlot.At(slotTime)
        every { repository.getAll() } returns emptyList()
        every { computeUseCase(emptyList()) } returns emptyList()

        worker.execute(slot)

        verify { notificationScheduler.rescheduleDailyCheck(ReminderSlot.At(slotTime)) }
    }

    @Test
    fun `should post before aiming the next check when execute is called`() {
        every { repository.getAll() } returns emptyList()
        every { computeUseCase(emptyList()) } returns emptyList()

        worker.execute(ReminderSlot.AppWide)

        verifyOrder {
            listNotifier.postNotifications(emptyList())
            notificationScheduler.rescheduleDailyCheck(ReminderSlot.AppWide)
        }
    }
}
