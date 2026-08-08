package fr.mandarine.todolist.data

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.ListNotification
import fr.mandarine.todolist.domain.TodoList
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AndroidListNotifierTest {

    private val context = mockk<Context>(relaxed = true)
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val notifManagerCompat = mockk<NotificationManagerCompat>(relaxed = true)
    private val pendingIntentMock = mockk<PendingIntent>()
    private val builtNotification = mockk<Notification>()

    @Before
    fun setUp() {
        every { context.getSystemService(NotificationManager::class.java) } returns notificationManager
        every { context.getString(R.string.notification_channel_name) } returns "Reminders"
        every { context.getString(R.string.notification_due_today) } returns "Due today"
        every { context.getString(R.string.notification_target_tomorrow) } returns "Scheduled for tomorrow"

        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(context) } returns notifManagerCompat

        mockkConstructor(NotificationChannelCompat.Builder::class)
        every {
            anyConstructed<NotificationChannelCompat.Builder>().setName(any())
        } answers { self as NotificationChannelCompat.Builder }
        every {
            anyConstructed<NotificationChannelCompat.Builder>().build()
        } returns mockk(relaxed = true)

        mockkStatic(PendingIntent::class)
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns pendingIntentMock

        mockkConstructor(Intent::class)
        every {
            anyConstructed<Intent>().setFlags(any<Int>())
        } answers { self as Intent }
        every {
            anyConstructed<Intent>().putExtra(any<String>(), any<String>())
        } returns mockk<Intent>()

        mockkConstructor(NotificationCompat.Builder::class)
        every {
            anyConstructed<NotificationCompat.Builder>().setSmallIcon(R.drawable.ic_checklist)
        } answers { self as NotificationCompat.Builder }
        every {
            anyConstructed<NotificationCompat.Builder>().setContentTitle(any())
        } answers { self as NotificationCompat.Builder }
        every {
            anyConstructed<NotificationCompat.Builder>().setContentText(any())
        } answers { self as NotificationCompat.Builder }
        every {
            anyConstructed<NotificationCompat.Builder>().setContentIntent(pendingIntentMock)
        } answers { self as NotificationCompat.Builder }
        every {
            anyConstructed<NotificationCompat.Builder>().setAutoCancel(true)
        } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().build() } returns builtNotification
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should not post any notification when list is empty`() {
        AndroidListNotifier(context).postNotifications(emptyList())
        verify(exactly = 0) { notificationManager.notify(any<Int>(), any<Notification>()) }
    }

    @Test
    fun `should not create channel when notifications list is empty`() {
        AndroidListNotifier(context).postNotifications(emptyList())
        verify(exactly = 0) { notifManagerCompat.createNotificationChannel(any<NotificationChannelCompat>()) }
    }

    @Test
    fun `should post one notification for a DueDateToday entry`() {
        val list = TodoList("list-1", "Groceries", dueDate = LocalDate.now())
        AndroidListNotifier(context).postNotifications(listOf(ListNotification.DueDateToday(list)))
        verify(exactly = 1) { notificationManager.notify(any<Int>(), any<Notification>()) }
    }

    @Test
    fun `should post one notification for a TargetDateTomorrow entry`() {
        val list = TodoList("list-2", "Weekend", targetDate = LocalDate.now().plusDays(1))
        AndroidListNotifier(context).postNotifications(
            listOf(ListNotification.TargetDateTomorrow(list))
        )
        verify(exactly = 1) { notificationManager.notify(any<Int>(), any<Notification>()) }
    }

    @Test
    fun `should post one notification per list when multiple entries are provided`() {
        val listA = TodoList("a", "ListA", dueDate = LocalDate.now())
        val listB = TodoList("b", "ListB", targetDate = LocalDate.now().plusDays(1))
        AndroidListNotifier(context).postNotifications(
            listOf(ListNotification.DueDateToday(listA), ListNotification.TargetDateTomorrow(listB))
        )
        verify(exactly = 2) { notificationManager.notify(any<Int>(), any<Notification>()) }
    }

    @Test
    fun `should use list id hashCode as notification id`() {
        val list = TodoList("my-id", "Test", dueDate = LocalDate.now())
        AndroidListNotifier(context).postNotifications(listOf(ListNotification.DueDateToday(list)))
        verify { notificationManager.notify("my-id".hashCode(), builtNotification) }
    }

    @Test
    fun `should set list name as notification title`() {
        val list = TodoList("1", "Groceries", dueDate = LocalDate.now())
        AndroidListNotifier(context).postNotifications(listOf(ListNotification.DueDateToday(list)))
        verify { anyConstructed<NotificationCompat.Builder>().setContentTitle("Groceries") }
    }

    @Test
    fun `should use due-today text for DueDateToday notification`() {
        val list = TodoList("1", "Work", dueDate = LocalDate.now())
        AndroidListNotifier(context).postNotifications(listOf(ListNotification.DueDateToday(list)))
        verify { context.getString(R.string.notification_due_today) }
        verify { anyConstructed<NotificationCompat.Builder>().setContentText("Due today") }
    }

    @Test
    fun `should use target-tomorrow text for TargetDateTomorrow notification`() {
        val list = TodoList("2", "Plans", targetDate = LocalDate.now().plusDays(1))
        AndroidListNotifier(context).postNotifications(
            listOf(ListNotification.TargetDateTomorrow(list))
        )
        verify { context.getString(R.string.notification_target_tomorrow) }
        verify { anyConstructed<NotificationCompat.Builder>().setContentText("Scheduled for tomorrow") }
    }

    @Test
    fun `should create notification channel when posting non-empty list`() {
        val list = TodoList("1", "Work", dueDate = LocalDate.now())
        AndroidListNotifier(context).postNotifications(listOf(ListNotification.DueDateToday(list)))
        verify { notifManagerCompat.createNotificationChannel(any<NotificationChannelCompat>()) }
    }

    @Test
    fun `should put LIST_ID extra in deep link intent`() {
        val list = TodoList("list-42", "Test List", dueDate = LocalDate.now())
        AndroidListNotifier(context).postNotifications(listOf(ListNotification.DueDateToday(list)))
        verify { anyConstructed<Intent>().putExtra("LIST_ID", "list-42") }
    }

    @Test
    fun `should put LIST_NAME extra in deep link intent`() {
        val list = TodoList("list-42", "Test List", dueDate = LocalDate.now())
        AndroidListNotifier(context).postNotifications(listOf(ListNotification.DueDateToday(list)))
        verify { anyConstructed<Intent>().putExtra("LIST_NAME", "Test List") }
    }

    @Test
    fun `should use notification id as pending intent request code`() {
        val list = TodoList("key", "Label", dueDate = LocalDate.now())
        AndroidListNotifier(context).postNotifications(listOf(ListNotification.DueDateToday(list)))
        verify { PendingIntent.getActivity(any(), "key".hashCode(), any(), any()) }
    }
}
