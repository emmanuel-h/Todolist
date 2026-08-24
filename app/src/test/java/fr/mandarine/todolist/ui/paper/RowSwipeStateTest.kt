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
    fun `should lock the row once it is past half the mark`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(travel / 2f - 1f)
        assertFalse(swipe.locked)

        swipe.drag(2f)
        assertTrue(swipe.locked)
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

        swipe.drag(70f)

        assertEquals(RowSwipe.Reveal, swipe.landing(velocity = 0f, flick = flick))
    }

    @Test
    fun `should take a flick that never reached the lock`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(-20f)

        assertEquals(RowSwipe.Delete, swipe.landing(velocity = -900f, flick = flick))
    }

    @Test
    fun `should let a row flicked back come home even from past the lock`() {
        val swipe = RowSwipeState(travel, reveals = true)

        swipe.drag(-80f)

        assertEquals(RowSwipe.Rest, swipe.landing(velocity = 900f, flick = flick))
    }

    @Test
    fun `should leave an untouched row alone`() {
        val swipe = RowSwipeState(travel, reveals = true)

        assertEquals(RowSwipe.Rest, swipe.landing(velocity = 900f, flick = flick))
    }
}
