package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.geometry.Offset
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

    /**
     * Halfway across the page the sheet still has all of its ink. It is given up in
     * the last tenth of the carry, once the sheet has all but reached the line it
     * is being put down on.
     */
    @Test
    fun `should interpolate the flight halfway through the second phase`() {
        val state = stickyNotePeelAt(1.5f)

        assertEquals(-10f, state.rotationDegrees, tolerance)
        assertEquals(1.04f, state.scale, tolerance)
        assertEquals(1f, state.alpha, tolerance)
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
    fun `should keep the sheet inked until it has all but arrived`() {
        assertEquals(1f, stickyNotePeelAt(STICKY_PEEL_LIFTED + 0.9f).alpha, tolerance)
    }

    @Test
    fun `should give the sheet's ink up over the last of the carry`() {
        val nearly = stickyNotePeelAt(STICKY_PEEL_LIFTED + 0.95f).alpha

        assertTrue("half its ink at the line, not $nearly", nearly < 1f)
        assertTrue("no ink left before the line, $nearly", nearly > 0f)
    }

    @Test
    fun `should carry a taken sheet from where the pad stands to where it is put down`() {
        val carried = stickyNoteCarryTo(
            landing = Offset(40f, 300f),
            seat = Offset(900f, 1800f),
            sheetInset = 8f,
            drift = Offset(-12f, -28f)
        )

        assertEquals(-868f, carried.x, tolerance)
        assertEquals(-1508f, carried.y, tolerance)
    }

    @Test
    fun `should drift a taken sheet the pad's own length when nowhere is named to put it`() {
        val carried = stickyNoteCarryTo(
            landing = null,
            seat = Offset(900f, 1800f),
            sheetInset = 8f,
            drift = Offset(-12f, -28f)
        )

        assertEquals(-12f, carried.x, tolerance)
        assertEquals(-28f, carried.y, tolerance)
    }

    @Test
    fun `should never scale the sheet below the pad it is taken from`() {
        assertTrue(stickyNoteSettleAt(STICKY_SETTLE_START).scale < 1f)
        assertTrue(stickyNotePeelAt(STICKY_PEEL_LIFTED).scale > 1f)
    }
}
