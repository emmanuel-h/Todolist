package fr.mandarine.todolist.data

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.ListNotification
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.ui.TodoListsActivity
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidListNotifierTest {

    private val application = ApplicationProvider.getApplicationContext<TodoListApplication>()
    private val notificationManager = application.getSystemService(NotificationManager::class.java)
    private val notifier = AndroidListNotifier(application)

    @Test
    fun `should not post any notification when list is empty`() {
        notifier.postNotifications(emptyList())

        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `should not create channel when notifications list is empty`() {
        notifier.postNotifications(emptyList())

        assertTrue(shadowOf(notificationManager).notificationChannels.isEmpty())
    }

    @Test
    fun `should create the reminder channel when posting`() {
        val list = TodoList("list-1", "Groceries", dueDate = LocalDate.now())

        notifier.postNotifications(listOf(ListNotification.DueDateToday(list)))

        val channels = shadowOf(notificationManager).notificationChannels
        assertEquals(1, channels.size)
        assertEquals(AndroidListNotifier.CHANNEL_ID, (channels.first() as android.app.NotificationChannel).id)
    }

    @Test
    fun `should post one notification tagged with the list id`() {
        val list = TodoList("list-1", "Groceries", dueDate = LocalDate.now())

        notifier.postNotifications(listOf(ListNotification.DueDateToday(list)))

        assertEquals(1, shadowOf(notificationManager).size())
        assertNotNull(shadowOf(notificationManager).getNotification("list-1", AndroidListNotifier.NOTIFICATION_ID))
    }

    @Test
    fun `should post one notification per list when multiple entries are provided`() {
        val listA = TodoList("a", "ListA", dueDate = LocalDate.now())
        val listB = TodoList("b", "ListB", targetDate = LocalDate.now().plusDays(1))

        notifier.postNotifications(
            listOf(ListNotification.DueDateToday(listA), ListNotification.TargetDateTomorrow(listB))
        )

        assertEquals(2, shadowOf(notificationManager).size())
        assertNotNull(shadowOf(notificationManager).getNotification("a", AndroidListNotifier.NOTIFICATION_ID))
        assertNotNull(shadowOf(notificationManager).getNotification("b", AndroidListNotifier.NOTIFICATION_ID))
    }

    @Test
    fun `should set list name as notification title`() {
        val list = TodoList("list-1", "Groceries", dueDate = LocalDate.now())

        notifier.postNotifications(listOf(ListNotification.DueDateToday(list)))

        val notification = shadowOf(notificationManager).getNotification("list-1", AndroidListNotifier.NOTIFICATION_ID)
        assertEquals("Groceries", notification.extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun `should show alarm emoji with numeric due date for DueDateToday notification`() {
        withLocale(Locale.US) {
            val list = TodoList("list-1", "Work", dueDate = LocalDate.of(2026, 8, 10))

            notifier.postNotifications(listOf(ListNotification.DueDateToday(list)))

            val notification = shadowOf(notificationManager).getNotification("list-1", AndroidListNotifier.NOTIFICATION_ID)
            assertEquals("⏰ 8/10", notification.extras.getString(Notification.EXTRA_TEXT))
        }
    }

    @Test
    fun `should show calendar emoji with numeric target date for TargetDateTomorrow notification`() {
        withLocale(Locale.US) {
            val list = TodoList("list-2", "Plans", targetDate = LocalDate.of(2026, 8, 11))

            notifier.postNotifications(listOf(ListNotification.TargetDateTomorrow(list)))

            val notification = shadowOf(notificationManager).getNotification("list-2", AndroidListNotifier.NOTIFICATION_ID)
            assertEquals("📅 8/11", notification.extras.getString(Notification.EXTRA_TEXT))
        }
    }

    @Test
    fun `should order day before month in notification date when format locale does`() {
        withLocale(Locale.FRANCE) {
            val list = TodoList("list-1", "Travail", dueDate = LocalDate.of(2026, 8, 10))

            notifier.postNotifications(listOf(ListNotification.DueDateToday(list)))

            val notification = shadowOf(notificationManager).getNotification("list-1", AndroidListNotifier.NOTIFICATION_ID)
            assertEquals("⏰ 10/08", notification.extras.getString(Notification.EXTRA_TEXT))
        }
    }

    private fun withLocale(locale: Locale, block: () -> Unit) {
        val previous = Locale.getDefault(Locale.Category.FORMAT)
        Locale.setDefault(Locale.Category.FORMAT, locale)
        try {
            block()
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous)
        }
    }

    /**
     * Both screens now live in one window, so the task the tap builds is that one
     * window carrying the list it should open on top of its own page of lists —
     * not two activities stacked on each other.
     */
    @Test
    fun `should open the one window the notebook is read in for the tap intent`() {
        val list = TodoList("list-42", "Test List", dueDate = LocalDate.now())

        notifier.postNotifications(listOf(ListNotification.DueDateToday(list)))

        val notification = shadowOf(notificationManager).getNotification("list-42", AndroidListNotifier.NOTIFICATION_ID)
        val savedIntents = shadowOf(notification.contentIntent).savedIntents
        assertEquals(1, savedIntents.size)
        assertEquals(ComponentName(application, TodoListsActivity::class.java), savedIntents[0].component)
    }

    @Test
    fun `should carry list id and name extras and a unique data uri in the tap intent`() {
        val list = TodoList("list-42", "Test List", dueDate = LocalDate.now())

        notifier.postNotifications(listOf(ListNotification.DueDateToday(list)))

        val notification = shadowOf(notificationManager).getNotification("list-42", AndroidListNotifier.NOTIFICATION_ID)
        val tapIntent = shadowOf(notification.contentIntent).savedIntents.last()
        assertEquals("list-42", tapIntent.getStringExtra("LIST_ID"))
        assertEquals("Test List", tapIntent.getStringExtra("LIST_NAME"))
        assertEquals("todolist://list/list-42", tapIntent.data.toString())
    }
}
