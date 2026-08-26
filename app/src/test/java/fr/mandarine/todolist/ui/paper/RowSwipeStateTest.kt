package fr.mandarine.todolist.ui.paper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RowSwipeStateTest {

    private val travel = 100f
    private val flick = 300f

    @Test
    fun `should follow the finger while the mark is still being drawn`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(60f)

        assertEquals(60f, swipe.offset, 0.01f)
    }

    @Test
    fun `should grow heavy once the mark is fully drawn`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(200f)

        assertTrue(swipe.offset > travel)
        assertTrue(swipe.offset < 200f)
    }

    @Test
    fun `should resist the same either way`() {
        val pulled = weightedSwipe(-200f, travel)

        assertEquals(-weightedSwipe(200f, travel), pulled, 0.01f)
    }

    @Test
    fun `should refuse to uncover a mark a row does not offer`() {
        val swipe = RowSwipeState(travel, reveals = false)

        swipe.drag(80f)

        assertEquals(0f, swipe.offset, 0.01f)
        assertFalse(swipe.travelling)
    }

    @Test
    fun `should still tear off a row that offers no mark the other way`() {
        val swipe = RowSwipeState(travel, reveals = false)

        swipe.drag(-80f)

        assertEquals(RowSwipe.Delete, swipe.landing(velocity = 0f, flick = flick))
    }

    @Test
    fun `should lock the row once it is well past the mark`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(travel * 0.6f - 1f)
        assertFalse(swipe.locked)

        swipe.drag(2f)
        assertTrue(swipe.locked)
    }

    /**
     * Easing a row back towards home is how the reader changes their mind, and it
     * has to work from anywhere — having to drag it all the way past the middle to
     * be let off is not a way out, it is a second gesture.
     */
    @Test
    fun `should let a row eased back towards home off the swipe`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(80f)
        swipe.drag(-50f)

        assertFalse(swipe.locked)
        assertEquals(RowSwipe.Rest, swipe.landing(velocity = 0f, flick = flick))
    }

    @Test
    fun `should still answer a row the reader kept held over`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(90f)
        swipe.drag(-15f)

        assertEquals(RowSwipe.Reveal, swipe.landing(velocity = 0f, flick = flick))
    }

    @Test
    fun `should spring home from a drag that never locked`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(20f)

        assertEquals(RowSwipe.Rest, swipe.landing(velocity = 0f, flick = flick))
    }

    @Test
    fun `should take the mark it was drawing when the finger lifts past the lock`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(80f)

        assertEquals(RowSwipe.Reveal, swipe.landing(velocity = 0f, flick = flick))
    }

    @Test
    fun `should take a flick that got a good way over without reaching the lock`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(-40f)

        assertEquals(RowSwipe.Delete, swipe.landing(velocity = -900f, flick = flick))
    }

    /**
     * A flick has to have been going somewhere. Taking any fast movement at all
     * meant a row barely touched could be torn off by a twitch.
     */
    @Test
    fun `should ignore a flick from a row that was barely moved`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(-10f)

        assertEquals(RowSwipe.Rest, swipe.landing(velocity = -900f, flick = flick))
    }

    @Test
    /**
     * A finger leaving a swipe very often flicks back a hair. Reading the row where
     * it happened to be at that instant let the flick land it on the other side of
     * the page and do the opposite of what the reader had just watched themselves
     * uncover — the one thing a gesture must never do.
     */
    fun `should still do what a row was pulled for when the finger flicks back off it`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(-80f)

        assertEquals(RowSwipe.Delete, swipe.landing(velocity = 900f, flick = flick))
    }

    @Test
    fun `should never do the opposite of the way a row was pulled`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(-80f)
        swipe.drag(90f)

        assertEquals(RowSwipe.Rest, swipe.landing(velocity = 900f, flick = flick))
    }

    @Test
    fun `should call a swipe off when the row is dragged back past where it started`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(80f)
        swipe.drag(-95f)

        assertEquals(RowSwipe.Rest, swipe.landing(velocity = 0f, flick = flick))
    }

    @Test
    fun `should forget the last swipe when a new one begins`() {
        val swipe = RowSwipeState(travel, reveals = true)
        swipe.drag(-80f)

        swipe.begin()

        assertEquals(RowSwipe.Rest, swipe.landing(velocity = 0f, flick = flick))
    }

    @Test
    fun `should leave an untouched row alone`() {
        val swipe = RowSwipeState(travel, reveals = true)

        assertEquals(RowSwipe.Rest, swipe.landing(velocity = 900f, flick = flick))
    }
}
