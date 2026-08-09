package fr.mandarine.todolist.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyCheckScheduleTest {

    private val zone = ZoneId.of("Europe/Paris")

    private fun millisAt(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `should return delay until same day 8am when now is before 8am`() {
        val now = millisAt(LocalDate.of(2026, 8, 5), LocalTime.of(6, 30))
        val delay = DailyCheckSchedule.millisUntilNextCheck(now, zone)
        assertEquals(90L * 60 * 1000, delay)
    }

    @Test
    fun `should return delay until next day 8am when now is after 8am`() {
        val now = millisAt(LocalDate.of(2026, 8, 5), LocalTime.of(9, 0))
        val delay = DailyCheckSchedule.millisUntilNextCheck(now, zone)
        assertEquals(23L * 60 * 60 * 1000, delay)
    }

    @Test
    fun `should return a full day when now is exactly 8am`() {
        val now = millisAt(LocalDate.of(2026, 8, 5), LocalTime.of(8, 0))
        val delay = DailyCheckSchedule.millisUntilNextCheck(now, zone)
        assertEquals(24L * 60 * 60 * 1000, delay)
    }

    @Test
    fun `should return delay crossing midnight when now is just before midnight`() {
        val now = millisAt(LocalDate.of(2026, 8, 5), LocalTime.of(23, 0))
        val delay = DailyCheckSchedule.millisUntilNextCheck(now, zone)
        assertEquals(9L * 60 * 60 * 1000, delay)
    }

    @Test
    fun `should compute delay relative to the given zone`() {
        val utc = ZoneId.of("UTC")
        val now = LocalDate.of(2026, 8, 5).atTime(LocalTime.of(6, 30)).atZone(utc).toInstant().toEpochMilli()
        val delay = DailyCheckSchedule.millisUntilNextCheck(now, utc)
        assertEquals(90L * 60 * 1000, delay)
    }
}
