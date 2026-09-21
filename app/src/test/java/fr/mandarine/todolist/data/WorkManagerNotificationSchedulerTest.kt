package fr.mandarine.todolist.data

import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import fr.mandarine.todolist.DailyNotificationWork
import fr.mandarine.todolist.FakeClock
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.ReminderSlot
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
    fun `should enqueue one app-wide check when nothing is armed and no own times`() {
        scheduler().ensureDailyChecks(emptySet())

        val infos = appWideInfos()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    /**
     * The moment is asserted absolutely rather than against a recomputation of the
     * same call, so the schedule pointing at the wrong hour is a failure here and
     * not something the assertion follows along with.
     */
    @Test
    fun `should aim the app-wide check at the next occurrence of the chosen hour`() {
        scheduler().ensureDailyChecks(emptySet())

        assertEquals(atLocal(MARCH_16, LocalTime.of(8, 0)), nextRunOfAppWide())
    }

    /**
     * What a launch must never do. A check the device deferred past its hour is
     * still pending, and re-laying it would push it to tomorrow — the list due
     * today then never gets its notification.
     */
    @Test
    fun `should leave a pending app-wide check where it is when the schedule is ensured again`() {
        val repository = FakeReminderTimeRepository()
        val scheduler = scheduler(repository)
        scheduler.ensureDailyChecks(emptySet())
        val armed = nextRunOfAppWide()

        repository.time = LocalTime.of(23, 0)
        scheduler.ensureDailyChecks(emptySet())

        assertEquals(1, appWideInfos().size)
        assertEquals(armed, nextRunOfAppWide())
    }

    @Test
    fun `should move the next app-wide check when the chosen hour changes`() {
        val repository = FakeReminderTimeRepository()
        val scheduler = scheduler(repository)
        scheduler.ensureDailyChecks(emptySet())
        val atEight = nextRunOfAppWide()

        repository.time = LocalTime.of(23, 0)
        scheduler.rescheduleDailyCheck(ReminderSlot.AppWide)

        assertNotEquals(atEight, nextRunOfAppWide())
        assertEquals(atLocal(MARCH_15, LocalTime.of(23, 0)), nextRunOfAppWide())
    }

    @Test
    fun `should keep one app-wide check when the schedule is laid again`() {
        val scheduler = scheduler()

        scheduler.ensureDailyChecks(emptySet())
        scheduler.rescheduleDailyCheck(ReminderSlot.AppWide)
        scheduler.ensureDailyChecks(emptySet())

        assertEquals(1, appWideInfos().size)
    }

    @Test
    fun `should enqueue an own-time check when a list has its own time`() {
        val ownTime = LocalTime.of(7, 30)

        scheduler().ensureDailyChecks(setOf(ownTime))

        val ownInfos = ownTimeInfos()
        assertEquals(1, ownInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, ownInfos.first().state)
    }

    @Test
    fun `should aim the own-time check at the next occurrence of the list time`() {
        val ownTime = LocalTime.of(7, 30)

        scheduler().ensureDailyChecks(setOf(ownTime))

        val minute = 7 * 60 + 30
        assertEquals(atLocal(MARCH_16, ownTime), nextRunOfOwnTime(minute))
    }

    @Test
    fun `should tag the own-time check with its minute of day`() {
        val ownTime = LocalTime.of(7, 30)

        scheduler().ensureDailyChecks(setOf(ownTime))

        val minute = 7 * 60 + 30
        val info = ownTimeInfos().first()
        val minuteTag = "${WorkManagerNotificationScheduler.OWN_TIME_TAG}_$minute"
        assertEquals(true, info.tags.contains(minuteTag))
    }

    @Test
    fun `should enqueue one check per distinct own time`() {
        val time1 = LocalTime.of(7, 30)
        val time2 = LocalTime.of(9, 15)

        scheduler().ensureDailyChecks(setOf(time1, time2))

        assertEquals(2, ownTimeInfos().size)
    }

    @Test
    fun `should enqueue only one check when two lists share the same own time`() {
        val time = LocalTime.of(7, 30)

        scheduler().ensureDailyChecks(setOf(time))

        assertEquals(1, ownTimeInfos().size)
    }

    @Test
    fun `should cancel a stale own-time check when it is no longer in the set`() {
        val sched = scheduler()
        val ownTime = LocalTime.of(7, 30)
        sched.ensureDailyChecks(setOf(ownTime))
        assertEquals(1, ownTimeInfos().size)

        sched.ensureDailyChecks(emptySet())

        assertEquals(0, ownTimeInfos(excludeCancelled = false).filter {
            it.state != WorkInfo.State.CANCELLED
        }.size)
    }

    @Test
    fun `should leave the app-wide check untouched when cancelling a stale own-time check`() {
        val sched = scheduler()
        sched.ensureDailyChecks(setOf(LocalTime.of(7, 30)))
        val appWideBefore = nextRunOfAppWide()

        sched.ensureDailyChecks(emptySet())

        assertEquals(appWideBefore, nextRunOfAppWide())
    }

    @Test
    fun `should move only the named own-time check when rescheduling an At slot`() {
        val time1 = LocalTime.of(7, 30)
        val time2 = LocalTime.of(9, 15)
        val sched = scheduler()
        sched.ensureDailyChecks(setOf(time1, time2))
        val minute1 = 7 * 60 + 30
        val minute2 = 9 * 60 + 15
        val before = nextRunOfOwnTime(minute2)

        sched.rescheduleDailyCheck(ReminderSlot.At(time1))

        assertEquals(before, nextRunOfOwnTime(minute2))
    }

    private fun scheduler(
        repository: ReminderTimeRepository = FakeReminderTimeRepository()
    ) = WorkManagerNotificationScheduler(
        application,
        DailyNotificationWork::class.java,
        repository,
        FakeClock(nowMillis = atLocal(MARCH_15, LocalTime.of(9, 0)))
    )

    private fun appWideInfos(): List<WorkInfo> =
        WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork(WorkManagerNotificationScheduler.WORK_NAME)
            .get()

    private fun ownTimeInfos(excludeCancelled: Boolean = true): List<WorkInfo> =
        WorkManager.getInstance(application)
            .getWorkInfosByTag(WorkManagerNotificationScheduler.OWN_TIME_TAG)
            .get()
            .let { infos ->
                if (excludeCancelled) infos.filter {
                    it.state != WorkInfo.State.CANCELLED && it.state != WorkInfo.State.FAILED && it.state != WorkInfo.State.SUCCEEDED
                }
                else infos
            }

    private fun nextRunOfAppWide(): Long = appWideInfos().first().nextScheduleTimeMillis

    private fun nextRunOfOwnTime(minute: Int): Long {
        val name = "${WorkManagerNotificationScheduler.WORK_NAME}@$minute"
        return WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork(name)
            .get()
            .first()
            .nextScheduleTimeMillis
    }

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
