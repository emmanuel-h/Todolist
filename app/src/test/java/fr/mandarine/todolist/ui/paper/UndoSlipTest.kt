package fr.mandarine.todolist.ui.paper

import org.junit.Assert.assertEquals
import org.junit.Test

class UndoSlipTest {

    @Test
    fun `should hand back a whole scrap the instant the row is torn off`() {
        assertEquals(1f, slipLeft(elapsed = 0L, window = WINDOW), EXACT)
    }

    @Test
    fun `should spend the scrap in step with the clock the deletion is timed by`() {
        assertEquals(0.75f, slipLeft(elapsed = 2_250L, window = WINDOW), EXACT)
        assertEquals(0.5f, slipLeft(elapsed = 4_500L, window = WINDOW), EXACT)
        assertEquals(0.25f, slipLeft(elapsed = 6_750L, window = WINDOW), EXACT)
    }

    @Test
    fun `should leave nothing of the scrap the moment the window closes`() {
        assertEquals(0f, slipLeft(elapsed = WINDOW, window = WINDOW), EXACT)
    }

    /**
     * A tick served late reports more time than the window ever held. Reading the
     * clock instead of counting ticks is what keeps the scrap honest, and the
     * clamp is what stops a late reading from turning it inside out.
     */
    @Test
    fun `should leave nothing of the scrap when a tick lands after the window closed`() {
        assertEquals(0f, slipLeft(elapsed = WINDOW * 2, window = WINDOW), EXACT)
    }

    @Test
    fun `should hand back a whole scrap when a frame is timed before the tear`() {
        assertEquals(1f, slipLeft(elapsed = -100L, window = WINDOW), EXACT)
    }

    private companion object {
        const val WINDOW = 9_000L
        const val EXACT = 0.0001f
    }
}
