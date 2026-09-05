package fr.mandarine.todolist.data

import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.TodoListApplication
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPreferencesReminderTimeRepositoryTest {

    private val application = ApplicationProvider.getApplicationContext<TodoListApplication>()
    private val repository = SharedPreferencesReminderTimeRepository(application)

    @Test
    fun `should return 08 00 when no time has been stored`() {
        val time = repository.getReminderTime()

        assertEquals(8, time.hour)
        assertEquals(0, time.minute)
    }

    @Test
    fun `should return stored time when minute of day has been set`() {
        repository.setReminderTime(870)

        val time = repository.getReminderTime()
        assertEquals(14, time.hour)
        assertEquals(30, time.minute)
    }

    @Test
    fun `should return midnight when minute of day is 0`() {
        repository.setReminderTime(0)

        val time = repository.getReminderTime()
        assertEquals(LocalTime.of(0, 0), time)
        assertEquals(0, time.hour)
        assertEquals(0, time.minute)
    }

    @Test
    fun `should return 23 59 when minute of day is 1439`() {
        repository.setReminderTime(1439)

        val time = repository.getReminderTime()
        assertEquals(LocalTime.of(23, 59), time)
        assertEquals(23, time.hour)
        assertEquals(59, time.minute)
    }

    @Test
    fun `should throw when minute of day is negative`() {
        var thrown: IllegalArgumentException? = null
        try {
            repository.setReminderTime(-1)
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertEquals(true, thrown != null)
    }

    @Test
    fun `should throw when minute of day exceeds 1439`() {
        var thrown: IllegalArgumentException? = null
        try {
            repository.setReminderTime(1440)
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertEquals(true, thrown != null)
    }

    @Test
    fun `should overwrite previously stored time`() {
        repository.setReminderTime(480)
        repository.setReminderTime(600)

        val time = repository.getReminderTime()
        assertEquals(10, time.hour)
        assertEquals(0, time.minute)
    }
}
