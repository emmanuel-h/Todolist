package fr.mandarine.todolist.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.TodoListApplication
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nothing in the app writes the hour any more, so these tests write the stored
 * preference the way the bell used to. The file name and key are spelled out on
 * purpose: they are what an hour chosen before #106 was stored under, and renaming
 * either would quietly drop it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPreferencesReminderTimeRepositoryTest {

    private val application = ApplicationProvider.getApplicationContext<TodoListApplication>()
    private val repository = SharedPreferencesReminderTimeRepository(application)

    private fun stored(minuteOfDay: Int) {
        application.getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
            .edit().putInt("reminder_minute_of_day", minuteOfDay).commit()
    }

    @Test
    fun `should return 08 00 when no time has been stored`() {
        val time = repository.getReminderTime()

        assertEquals(8, time.hour)
        assertEquals(0, time.minute)
    }

    @Test
    fun `should return the hour a reader chose before the bell was removed`() {
        stored(870)

        val time = repository.getReminderTime()

        assertEquals(14, time.hour)
        assertEquals(30, time.minute)
    }

    @Test
    fun `should return midnight when the stored minute of day is 0`() {
        stored(0)

        val time = repository.getReminderTime()

        assertEquals(0, time.hour)
        assertEquals(0, time.minute)
    }

    @Test
    fun `should return 23 59 when the stored minute of day is 1439`() {
        stored(1439)

        val time = repository.getReminderTime()

        assertEquals(23, time.hour)
        assertEquals(59, time.minute)
    }
}
