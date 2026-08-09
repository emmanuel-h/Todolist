package fr.mandarine.todolist.data

import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import fr.mandarine.todolist.FakeClock
import fr.mandarine.todolist.TodoListApplication
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkManagerNotificationSchedulerTest {

    private val application = ApplicationProvider.getApplicationContext<TodoListApplication>()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(application)
    }

    @Test
    fun `should enqueue unique periodic work when scheduleDailyCheck is called`() {
        WorkManagerNotificationScheduler(application, FakeClock()).scheduleDailyCheck()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork(WorkManagerNotificationScheduler.WORK_NAME)
            .get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    @Test
    fun `should keep existing schedule when scheduleDailyCheck is called twice`() {
        val scheduler = WorkManagerNotificationScheduler(application, FakeClock())

        scheduler.scheduleDailyCheck()
        scheduler.scheduleDailyCheck()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork(WorkManagerNotificationScheduler.WORK_NAME)
            .get()
        assertEquals(1, infos.size)
    }
}
