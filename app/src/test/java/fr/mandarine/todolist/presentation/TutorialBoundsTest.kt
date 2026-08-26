package fr.mandarine.todolist.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A beat the hand travels across rather than lands on takes its points from the
 * row it is crossing. The tear-off drag is the only such beat, and it has to
 * start near the row's end and finish near its start.
 */
class TutorialBoundsTest {

    private val row = TutorialBounds(left = 40, top = 200, width = 300, height = 56)

    @Test
    fun `should walk the point along the row by the fraction given`() {
        assertEquals(TutorialBounds(190, 200, 0, 56), row.alongRow(0.5f))
    }

    @Test
    fun `should place a point near the end of the row further along than one near its start`() {
        assertEquals(298, row.alongRow(0.86f).left)
        assertEquals(94, row.alongRow(0.18f).left)
    }

    @Test
    fun `should rest at the start of the row for a fraction of nothing`() {
        assertEquals(40, row.alongRow(0f).left)
    }

    @Test
    fun `should reach the end of the row for the whole of it`() {
        assertEquals(340, row.alongRow(1f).left)
    }

    @Test
    fun `should keep the row it came from except for its width`() {
        val point = row.alongRow(0.25f)

        assertEquals(200, point.top)
        assertEquals(56, point.height)
        assertEquals(0, point.width)
    }
}
