package fr.mandarine.todolist

import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.data.AndroidListNotifier
import fr.mandarine.todolist.data.RoomTodoListRepository
import fr.mandarine.todolist.data.RoomTodoRepository
import fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepository
import fr.mandarine.todolist.data.WorkManagerNotificationScheduler
import fr.mandarine.todolist.domain.SystemClock
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppContainerTest {

    private val application = ApplicationProvider.getApplicationContext<TodoListApplication>()

    @Test
    fun `should wire room-backed repositories by default`() {
        assertTrue(application.container.todoListRepository is RoomTodoListRepository)
        assertTrue(application.container.todoRepository is RoomTodoRepository)
    }

    @Test
    fun `should wire android notifier and work manager scheduler by default`() {
        assertTrue(application.container.listNotifier is AndroidListNotifier)
        assertTrue(application.container.notificationScheduler is WorkManagerNotificationScheduler)
    }

    @Test
    fun `should expose a system clock`() {
        assertTrue(application.container.clock is SystemClock)
    }

    @Test
    fun `should wire shared preferences reminder time repository by default`() {
        assertTrue(application.container.reminderTimeRepository is SharedPreferencesReminderTimeRepository)
    }

}
