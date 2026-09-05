package fr.mandarine.todolist.data

import android.content.Context
import fr.mandarine.todolist.domain.ReminderTimeRepository
import java.time.LocalTime

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
