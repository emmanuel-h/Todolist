package fr.mandarine.todolist.domain

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
}
