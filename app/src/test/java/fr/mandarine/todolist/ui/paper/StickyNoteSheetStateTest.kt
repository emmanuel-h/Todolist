package fr.mandarine.todolist.ui.paper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StickyNoteSheetStateTest {

    private val tolerance = 0.0001f

    @Test
    fun `should leave the sheet untouched when the peel has not started`() {
        val state = stickyNotePeelAt(STICKY_PEEL_REST)

        assertEquals(0f, state.rotationDegrees, tolerance)
        assertEquals(1f, state.scale, tolerance)
        assertEquals(1f, state.alpha, tolerance)
        assertEquals(0f, state.travelFraction, tolerance)
        assertEquals(STICKY_FLAT, state.foldFraction, tolerance)
    }

    @Test
    fun `should lift the sheet without moving it when the peel reaches the lift point`() {
        val state = stickyNotePeelAt(STICKY_PEEL_LIFTED)

        assertEquals(-6f, state.rotationDegrees, tolerance)
        assertEquals(1.08f, state.scale, tolerance)
        assertEquals(1f, state.alpha, tolerance)
        assertEquals(0f, state.travelFraction, tolerance)
        assertEquals(STICKY_FOLDED, state.foldFraction, tolerance)
    }

    @Test
    fun `should interpolate the lift halfway through the first phase`() {
        val state = stickyNotePeelAt(0.5f)

        assertEquals(-3f, state.rotationDegrees, tolerance)
        assertEquals(1.04f, state.scale, tolerance)
        assertEquals(0.5f, state.foldFraction, tolerance)
    }

    @Test
    fun `should carry the sheet away and fade it out when the peel completes`() {
        val state = stickyNotePeelAt(STICKY_PEEL_GONE)

        assertEquals(-14f, state.rotationDegrees, tolerance)
        assertEquals(1f, state.scale, tolerance)
        assertEquals(0f, state.alpha, tolerance)
        assertEquals(1f, state.travelFraction, tolerance)
        assertEquals(STICKY_FLAT, state.foldFraction, tolerance)
    }

    @Test
    fun `should interpolate the flight halfway through the second phase`() {
        val state = stickyNotePeelAt(1.5f)

        assertEquals(-10f, state.rotationDegrees, tolerance)
        assertEquals(1.04f, state.scale, tolerance)
        assertEquals(0.5f, state.alpha, tolerance)
        assertEquals(0.5f, state.travelFraction, tolerance)
        assertEquals(0.5f, state.foldFraction, tolerance)
    }

    @Test
    fun `should clamp a peel progress that runs past either end`() {
        assertEquals(
            stickyNotePeelAt(STICKY_PEEL_REST).rotationDegrees,
            stickyNotePeelAt(-3f).rotationDegrees,
            tolerance
        )
        assertEquals(
            stickyNotePeelAt(STICKY_PEEL_GONE).alpha,
            stickyNotePeelAt(9f).alpha,
            tolerance
        )
    }

    @Test
    fun `should start the replacement sheet lifted transparent and undersized`() {
        val state = stickyNoteSettleAt(STICKY_SETTLE_START)

        assertEquals(-6f, state.rotationDegrees, tolerance)
        assertEquals(0.9f, state.scale, tolerance)
        assertEquals(0f, state.alpha, tolerance)
        assertEquals(0f, state.travelFraction, tolerance)
    }

    @Test
    fun `should land the replacement sheet at the resting angle and full size`() {
        val state = stickyNoteSettleAt(STICKY_SETTLE_DONE)

        assertEquals(0f, state.rotationDegrees, tolerance)
        assertEquals(1f, state.scale, tolerance)
        assertEquals(1f, state.alpha, tolerance)
    }

    @Test
    fun `should interpolate the settle halfway`() {
        val state = stickyNoteSettleAt(0.5f)

        assertEquals(-3f, state.rotationDegrees, tolerance)
        assertEquals(0.95f, state.scale, tolerance)
        assertEquals(0.5f, state.alpha, tolerance)
    }

    @Test
    fun `should clamp a settle progress that runs past either end`() {
        assertEquals(0f, stickyNoteSettleAt(-2f).alpha, tolerance)
        assertEquals(1f, stickyNoteSettleAt(4f).alpha, tolerance)
    }

    @Test
    fun `should lay the replacement sheet flat rather than folded`() {
        assertEquals(STICKY_FLAT, stickyNoteSettleAt(STICKY_SETTLE_START).foldFraction, tolerance)
        assertEquals(STICKY_FLAT, stickyNoteSettleAt(STICKY_SETTLE_DONE).foldFraction, tolerance)
    }

    @Test
    fun `should fold the sheet up before it travels and lay it down as it goes`() {
        val folded = stickyNotePeelAt(STICKY_PEEL_LIFTED).foldFraction

        assertTrue(stickyNotePeelAt(0.5f).foldFraction > stickyNotePeelAt(0.25f).foldFraction)
        assertTrue(stickyNotePeelAt(1.5f).foldFraction < folded)
        assertEquals(0f, stickyNotePeelAt(0.5f).travelFraction, tolerance)
    }

    @Test
    fun `should never scale the sheet below the pad it is taken from`() {
        assertTrue(stickyNoteSettleAt(STICKY_SETTLE_START).scale < 1f)
        assertTrue(stickyNotePeelAt(STICKY_PEEL_LIFTED).scale > 1f)
    }
}
