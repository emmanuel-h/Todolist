package fr.mandarine.todolist.domain

import java.time.LocalTime

/**
 * The one setting the app has: what time of day the reminder check runs.
 *
 * Stored as a minute-of-day `Int` rather than a `LocalTime` because the backing
 * store is `SharedPreferences`, which holds primitives — see
 * [fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepository]. Reads come
 * back as a [LocalTime] so nothing downstream does `minutes / 60` arithmetic.
 */
interface ReminderTimeRepository {
    fun getReminderTime(): LocalTime

    /** [minuteOfDay] must be in `0..1439`; the implementation enforces it. */
    fun setReminderTime(minuteOfDay: Int)
}
