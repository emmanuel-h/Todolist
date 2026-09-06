package fr.mandarine.todolist.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * How long until the next time the clock next reads [checkTime].
 *
 * `object` is Kotlin's singleton — one instance, created lazily, referred to by the
 * type name (`DailyCheckSchedule.millisUntilNextCheck(...)`). Used here as a
 * namespace for a pure function, so there is nothing to inject and nothing to mock.
 *
 * The zone is a parameter rather than `ZoneId.systemDefault()` read inside, for the
 * same reason [Clock] exists: a function that reads ambient state can only be tested
 * in the ambient state. The caller
 * ([fr.mandarine.todolist.data.WorkManagerNotificationScheduler]) passes the system
 * zone.
 *
 * The comparison is `isAfter`, so if it is *exactly* the check time right now the
 * answer is a full day rather than zero — that avoids enqueuing work with no delay
 * and firing the same check twice.
 */
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
