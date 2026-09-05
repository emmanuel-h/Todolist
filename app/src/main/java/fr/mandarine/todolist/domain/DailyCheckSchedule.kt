package fr.mandarine.todolist.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object DailyCheckSchedule {
    val DEFAULT_CHECK_TIME: LocalTime = LocalTime.of(8, 0)

    fun millisUntilNextCheck(nowMillis: Long, zone: ZoneId, checkTime: LocalTime = DEFAULT_CHECK_TIME): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val todayCheck = now.toLocalDate().atTime(checkTime).atZone(zone)
        val next = if (todayCheck.isAfter(now)) {
            todayCheck
        } else {
            now.toLocalDate().plusDays(1).atTime(checkTime).atZone(zone)
        }
        return next.toInstant().toEpochMilli() - nowMillis
    }
}
