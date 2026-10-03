package fr.mandarine.todolist.domain

import java.time.LocalTime

/**
 * The app-wide hour: what time of day the check runs for lists without a time of
 * their own. Nothing in the app writes it any more (#106) — a reader who chose one
 * on the bell keeps it, everyone else reads the default.
 *
 * Stored as a minute-of-day `Int` rather than a `LocalTime` because the backing
 * store is `SharedPreferences`, which holds primitives — see
 * [fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepository]. Reads come
 * back as a [LocalTime] so nothing downstream does `minutes / 60` arithmetic.
 */
interface ReminderTimeRepository {
    fun getReminderTime(): LocalTime
}
