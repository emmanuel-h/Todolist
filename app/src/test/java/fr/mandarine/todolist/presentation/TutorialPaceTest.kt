package fr.mandarine.todolist.presentation

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialPaceTest {

    @Test
    fun `should rest for the whole beat when the reader has said nothing`() = runTest {
        val pace = TutorialPace()

        pace.beat(700)

        assertEquals(700L, testScheduler.currentTime)
    }

    @Test
    fun `should take no rest at all once the reader has seen enough`() = runTest {
        val pace = TutorialPace()
        pace.hurry()

        pace.beat(700)

        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun `should end a rest already being taken when the reader says they have seen enough`() =
        runTest {
            val pace = TutorialPace()
            val resting = launch { pace.beat(700) }

            advanceTimeBy(200)
            pace.hurry()
            resting.join()

            assertEquals(200L, testScheduler.currentTime)
        }

    @Test
    fun `should rest again for the whole beat once the next scene has settled`() = runTest {
        val pace = TutorialPace()
        pace.hurry()
        pace.settle()

        pace.beat(700)

        assertEquals(700L, testScheduler.currentTime)
    }

    @Test
    fun `should not be hurrying before the reader has asked`() {
        assertFalse(TutorialPace().hurrying)
    }

    @Test
    fun `should be hurrying once the reader has asked`() {
        val pace = TutorialPace()

        pace.hurry()

        assertTrue(pace.hurrying)
    }

    @Test
    fun `should stop hurrying once the next scene has settled`() {
        val pace = TutorialPace()
        pace.hurry()

        pace.settle()

        assertFalse(pace.hurrying)
    }
}
