package fr.mandarine.todolist.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import fr.mandarine.todolist.domain.Clock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class AndroidNotificationSchedulerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val pendingIntent = mockk<PendingIntent>()

    @Before
    fun setUp() {
        every { context.getSystemService(AlarmManager::class.java) } returns alarmManager
        mockkConstructor(Intent::class)
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pendingIntent
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun epochMillisAt(hourOfDay: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun expected8AMToday(fromMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun expected8AMTomorrow(fromMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

    @Test
    fun `should schedule alarm with RTC_WAKEUP type`() {
        val clock = mockk<Clock> { every { now() } returns epochMillisAt(6) }
        AndroidNotificationScheduler(context, clock).scheduleNextDailyCheck()
        verify { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
    }

    @Test
    fun `should schedule alarm at today 8AM when current time is before 8AM`() {
        val nowMillis = epochMillisAt(6)
        val clock = mockk<Clock> { every { now() } returns nowMillis }
        AndroidNotificationScheduler(context, clock).scheduleNextDailyCheck()
        val expected = expected8AMToday(nowMillis)
        verify { alarmManager.setAndAllowWhileIdle(any(), expected, any()) }
    }

    @Test
    fun `should schedule alarm at tomorrow 8AM when current time is after 8AM`() {
        val nowMillis = epochMillisAt(9)
        val clock = mockk<Clock> { every { now() } returns nowMillis }
        AndroidNotificationScheduler(context, clock).scheduleNextDailyCheck()
        val expected = expected8AMTomorrow(nowMillis)
        verify { alarmManager.setAndAllowWhileIdle(any(), expected, any()) }
    }

    @Test
    fun `should schedule alarm at tomorrow 8AM when current time is exactly 8AM`() {
        val nowMillis = epochMillisAt(8)
        val clock = mockk<Clock> { every { now() } returns nowMillis }
        AndroidNotificationScheduler(context, clock).scheduleNextDailyCheck()
        val expected = expected8AMTomorrow(nowMillis)
        verify { alarmManager.setAndAllowWhileIdle(any(), expected, any()) }
    }

    @Test
    fun `should pass the pending intent to the alarm manager`() {
        val clock = mockk<Clock> { every { now() } returns epochMillisAt(6) }
        AndroidNotificationScheduler(context, clock).scheduleNextDailyCheck()
        verify { alarmManager.setAndAllowWhileIdle(any(), any(), pendingIntent) }
    }

    @Test
    fun `should create broadcast pending intent with the correct context`() {
        val clock = mockk<Clock> { every { now() } returns epochMillisAt(6) }
        AndroidNotificationScheduler(context, clock).scheduleNextDailyCheck()
        verify { PendingIntent.getBroadcast(context, any(), any(), any()) }
    }
}
