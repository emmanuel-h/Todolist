package fr.mandarine.todolist.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

    @Test
    fun `should use provided check time when now is before it`() {
        val checkTime = LocalTime.of(14, 0)
        val now = millisAt(LocalDate.of(2026, 8, 5), LocalTime.of(6, 30))
        val delay = DailyCheckSchedule.millisUntilNextCheck(now, zone, checkTime)
        assertEquals(7L * 60 * 60 * 1000 + 30L * 60 * 1000, delay)
    }

    @Test
    fun `should use provided check time when now is after it`() {
        val checkTime = LocalTime.of(10, 0)
        val now = millisAt(LocalDate.of(2026, 8, 5), LocalTime.of(12, 0))
        val delay = DailyCheckSchedule.millisUntilNextCheck(now, zone, checkTime)
        assertEquals(22L * 60 * 60 * 1000, delay)
    }

    @Test
    fun `should use provided check time when now is exactly at it`() {
        val checkTime = LocalTime.of(20, 0)
        val now = millisAt(LocalDate.of(2026, 8, 5), LocalTime.of(20, 0))
        val delay = DailyCheckSchedule.millisUntilNextCheck(now, zone, checkTime)
        assertEquals(24L * 60 * 60 * 1000, delay)
    }

    @Test
    fun `should expose a named default check time of 08 00`() {
        assertEquals(8, DailyCheckSchedule.DEFAULT_CHECK_TIME.hour)
        assertEquals(0, DailyCheckSchedule.DEFAULT_CHECK_TIME.minute)
    }

    /**
     * Spring forward. 02:00 does not exist on this date in Paris, and the check is
     * at 08:00 so the day is simply an hour shorter — the gap from the evening
     * before is 23 hours, not 24. A schedule that counts elapsed hours instead of
     * naming the moment lands an hour late from here on.
     */
    @Test
    fun `should shorten the wait across the spring forward change`() {
        val evening = ZonedDateTime.of(2026, 3, 28, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 3, 29, 8, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(expected - evening, DailyCheckSchedule.millisUntilNextCheck(evening, zone))
        assertEquals(TWELVE_HOURS - ONE_HOUR, expected - evening)
    }

    /**
     * Fall back. 02:00 to 03:00 happens twice, so the same evening-to-morning gap
     * is an hour longer.
     */
    @Test
    fun `should lengthen the wait across the fall back change`() {
        val evening = ZonedDateTime.of(2026, 10, 24, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 10, 25, 8, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(expected - evening, DailyCheckSchedule.millisUntilNextCheck(evening, zone))
        assertEquals(TWELVE_HOURS + ONE_HOUR, expected - evening)
    }

    private companion object {
        const val ONE_HOUR = 3_600_000L
        const val TWELVE_HOURS = 12 * ONE_HOUR
    }
}
