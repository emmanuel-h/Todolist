package fr.mandarine.todolist.data

import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import fr.mandarine.todolist.DailyNotificationWork
import fr.mandarine.todolist.FakeClock
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.ReminderTimeRepository
import java.time.LocalTime
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
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
        val scheduler = WorkManagerNotificationScheduler(
            application,
            DailyNotificationWork::class.java,
            FakeReminderTimeRepository(),
            FakeClock()
        )

        scheduler.scheduleDailyCheck()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork(WorkManagerNotificationScheduler.WORK_NAME)
            .get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    /**
     * Laying the schedule again is what every launch does, so it must not leave two
     * checks behind, and with the hour unchanged it must still point at the moment
     * it already pointed at.
     */
    @Test
    fun `should keep one check pointing at the same moment when the hour has not changed`() {
        val scheduler = WorkManagerNotificationScheduler(
            application,
            DailyNotificationWork::class.java,
            FakeReminderTimeRepository(),
            FakeClock()
        )

        scheduler.scheduleDailyCheck()
        val first = nextRunOfTheCheck()

        scheduler.scheduleDailyCheck()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork(WorkManagerNotificationScheduler.WORK_NAME)
            .get()
        assertEquals(1, infos.size)
        // WorkManager stamps the absolute moment at enqueue, so laying the same
        // schedule twice lands a few real milliseconds apart. Hours apart is the
        // failure being guarded against; milliseconds are the clock ticking.
        assertTrue(
            "moved by ${abs(nextRunOfTheCheck() - first)} ms",
            abs(nextRunOfTheCheck() - first) < SAME_MOMENT_MILLIS
        )
    }

    /**
     * The one that matters, and the one a policy check cannot make: a changed hour
     * has to move the moment the check actually runs. `KEEP` never moved it, and
     * `UPDATE` does not either — it replaces the request but keeps the period
     * already running, so the new delay is dropped on the floor and the reminder
     * still arrives at the old hour. Only re-enqueuing moves it, and on a device
     * that is the difference between the setting working and doing nothing.
     */
    @Test
    fun `should move the next check when the chosen hour changes`() {
        val repository = FakeReminderTimeRepository()
        val scheduler = WorkManagerNotificationScheduler(
            application,
            DailyNotificationWork::class.java,
            repository,
            FakeClock()
        )

        scheduler.scheduleDailyCheck()
        val atEight = nextRunOfTheCheck()

        repository.time = LocalTime.of(23, 0)
        scheduler.scheduleDailyCheck()

        assertNotEquals(atEight, nextRunOfTheCheck())
    }

    private fun nextRunOfTheCheck(): Long =
        WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork(WorkManagerNotificationScheduler.WORK_NAME)
            .get().first().nextScheduleTimeMillis

    private class FakeReminderTimeRepository : ReminderTimeRepository {
        var time: LocalTime = LocalTime.of(8, 0)
        override fun getReminderTime(): LocalTime = time
        override fun setReminderTime(minuteOfDay: Int) {
            time = LocalTime.of(minuteOfDay / MINUTES_IN_HOUR, minuteOfDay % MINUTES_IN_HOUR)
        }
    }

    private companion object {
        const val MINUTES_IN_HOUR = 60
        const val SAME_MOMENT_MILLIS = 1_000L
    }
}
