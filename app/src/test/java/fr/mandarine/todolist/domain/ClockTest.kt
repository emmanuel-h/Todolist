package fr.mandarine.todolist.domain

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTest {

    @Test
    fun `should return current system time in milliseconds`() {
        val clock = SystemClock()
        val before = System.currentTimeMillis()
        val result = clock.now()
        val after = System.currentTimeMillis()

        assertTrue(result >= before)
        assertTrue(result <= after)
    }

    @Test
    fun `should return current local date when asked for today`() {
        val clock = SystemClock()
        val before = LocalDate.now()
        val result = clock.today()
        val after = LocalDate.now()

        assertTrue(!result.isBefore(before))
        assertTrue(!result.isAfter(after))
    }
}
