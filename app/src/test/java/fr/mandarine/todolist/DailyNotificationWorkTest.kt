package fr.mandarine.todolist

import android.app.NotificationManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.data.TodoListEntity
import fr.mandarine.todolist.domain.NotificationScheduler
import io.mockk.mockk
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyNotificationWorkTest {

    private lateinit var application: TodoListApplication
    private lateinit var database: TodoDatabase

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(application, TodoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        application.container = AppContainer(
            application,
            databaseFactory = { database },
            schedulerFactory = { NotificationScheduler { } }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun runWorker(): ListenableWorker.Result =
        DailyNotificationWork(application, mockk<WorkerParameters>(relaxed = true)).doWork()

    @Test
    fun `should post notification and succeed when a list is due today`() {
        database.todoListDao().insert(
            TodoListEntity("1", "Groceries", 0, null, LocalDate.now().toEpochDay())
        )

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        val notificationManager = application.getSystemService(NotificationManager::class.java)
        assertEquals(1, shadowOf(notificationManager).size())
    }

    @Test
    fun `should post nothing and succeed when no list matches today`() {
        database.todoListDao().insert(TodoListEntity("1", "Groceries", 0, null, null))

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        val notificationManager = application.getSystemService(NotificationManager::class.java)
        assertEquals(0, shadowOf(notificationManager).size())
    }
}
