package fr.mandarine.todolist.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object DailyCheckSchedule {
    val CHECK_TIME: LocalTime = LocalTime.of(8, 0)

    fun millisUntilNextCheck(nowMillis: Long, zone: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val todayCheck = now.toLocalDate().atTime(CHECK_TIME).atZone(zone)
        val next = if (todayCheck.isAfter(now)) {
            todayCheck
        } else {
            now.toLocalDate().plusDays(1).atTime(CHECK_TIME).atZone(zone)
        }
        return next.toInstant().toEpochMilli() - nowMillis
    }
}
