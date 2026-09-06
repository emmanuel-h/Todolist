package fr.mandarine.todolist.data

import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import fr.mandarine.todolist.DailyNotificationWork
import fr.mandarine.todolist.FakeClock
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.ReminderTimeRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
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
    private val originalZone: TimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE.id))
        WorkManagerTestInitHelper.initializeTestWorkManager(application)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `should enqueue one check when nothing is armed`() {
        scheduler().ensureDailyCheck()

        val infos = checkInfos()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    /**
     * The moment is asserted absolutely rather than against a recomputation of the
     * same call, so the schedule pointing at the wrong hour is a failure here and
     * not something the assertion follows along with.
     */
    @Test
    fun `should aim the check at the next occurrence of the chosen hour`() {
        scheduler().ensureDailyCheck()

        assertEquals(atLocal(MARCH_16, LocalTime.of(8, 0)), nextRunOfTheCheck())
    }

    /**
     * What a launch must never do. A check the device deferred past its hour is
     * still pending, and re-laying it would push it to tomorrow — the list due
     * today then never gets its notification.
     */
    @Test
    fun `should leave a pending check where it is when the schedule is ensured again`() {
        val repository = FakeReminderTimeRepository()
        val scheduler = scheduler(repository)
        scheduler.ensureDailyCheck()
        val armed = nextRunOfTheCheck()

        repository.time = LocalTime.of(23, 0)
        scheduler.ensureDailyCheck()

        assertEquals(1, checkInfos().size)
        assertEquals(armed, nextRunOfTheCheck())
    }

    @Test
    fun `should move the next check when the chosen hour changes`() {
        val repository = FakeReminderTimeRepository()
        val scheduler = scheduler(repository)
        scheduler.ensureDailyCheck()
        val atEight = nextRunOfTheCheck()

        repository.time = LocalTime.of(23, 0)
        scheduler.rescheduleDailyCheck()

        assertNotEquals(atEight, nextRunOfTheCheck())
        assertEquals(atLocal(MARCH_15, LocalTime.of(23, 0)), nextRunOfTheCheck())
    }

    @Test
    fun `should keep one check when the schedule is laid again`() {
        val scheduler = scheduler()

        scheduler.ensureDailyCheck()
        scheduler.rescheduleDailyCheck()
        scheduler.ensureDailyCheck()

        assertEquals(1, checkInfos().size)
    }

    private fun scheduler(
        repository: ReminderTimeRepository = FakeReminderTimeRepository()
    ) = WorkManagerNotificationScheduler(
        application,
        DailyNotificationWork::class.java,
        repository,
        FakeClock(nowMillis = atLocal(MARCH_15, LocalTime.of(9, 0)))
    )

    private fun checkInfos(): List<WorkInfo> =
        WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork(WorkManagerNotificationScheduler.WORK_NAME)
            .get()

    private fun nextRunOfTheCheck(): Long = checkInfos().first().nextScheduleTimeMillis

    private fun atLocal(date: LocalDate, time: LocalTime): Long =
        ZonedDateTime.of(date, time, ZONE).toInstant().toEpochMilli()

    private class FakeReminderTimeRepository : ReminderTimeRepository {
        var time: LocalTime = LocalTime.of(8, 0)
        override fun getReminderTime(): LocalTime = time
        override fun setReminderTime(minuteOfDay: Int) {
            time = LocalTime.of(minuteOfDay / MINUTES_IN_HOUR, minuteOfDay % MINUTES_IN_HOUR)
        }
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Paris")
        val MARCH_15: LocalDate = LocalDate.of(2026, 3, 15)
        val MARCH_16: LocalDate = LocalDate.of(2026, 3, 16)
        const val MINUTES_IN_HOUR = 60
    }
}
