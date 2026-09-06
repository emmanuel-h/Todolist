package fr.mandarine.todolist.data

import android.content.Context
import fr.mandarine.todolist.domain.ReminderTimeRepository
import java.time.LocalTime

/**
 * The reminder hour, kept in `SharedPreferences` rather than in the database.
 *
 * It is a single scalar preference with no relationship to any list, so a table for
 * it would be a table with one row. `SharedPreferences` is also readable
 * synchronously without touching the Room dispatcher, which matters because the
 * ViewModel reads it in its constructor to seed the initial state.
 *
 * Stored as minutes since midnight (`0..1439`) because the file format holds
 * primitives. The conversion to and from [LocalTime] is confined to this class, so
 * nothing above it does `/ 60` arithmetic.
 *
 * `apply()` writes asynchronously and returns immediately; `commit()` would block
 * the caller for a fsync. A lost write here costs a reminder at the wrong hour once,
 * which does not justify a synchronous disk write on the main thread.
 *
 * The default of 08:00 is repeated in
 * [fr.mandarine.todolist.domain.DailyCheckSchedule.DEFAULT_CHECK_TIME]; that one is
 * the fallback for a scheduler with no repository, this one is what a reader with
 * no stored preference gets.
 */
class SharedPreferencesReminderTimeRepository(context: Context) : ReminderTimeRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getReminderTime(): LocalTime {
        val minuteOfDay = prefs.getInt(KEY_MINUTE_OF_DAY, DEFAULT_MINUTE_OF_DAY)
        return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    }

    override fun setReminderTime(minuteOfDay: Int) {
        require(minuteOfDay in 0..1439)
        prefs.edit().putInt(KEY_MINUTE_OF_DAY, minuteOfDay).apply()
    }

    companion object {
        private const val PREFS_NAME = "reminder_settings"
        private const val KEY_MINUTE_OF_DAY = "reminder_minute_of_day"
        private const val DEFAULT_MINUTE_OF_DAY = 8 * 60
    }
}
