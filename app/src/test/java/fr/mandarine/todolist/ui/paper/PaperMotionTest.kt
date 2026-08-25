package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperMotionTest {

    private val tolerance = 0.0001f

    @Test
    fun `should make a pick-up overshoot so paper reads as lifted off the page`() {
        assertTrue(PaperMotion.pickUp.dampingRatio < Spring.DampingRatioNoBouncy)
        assertTrue(PaperMotion.pickUp.stiffness > PaperMotion.sheetSettle.stiffness)
    }

    @Test
    fun `should settle a sheet slower and calmer than it picks one up`() {
        assertTrue(PaperMotion.sheetSettle.dampingRatio > PaperMotion.pickUp.dampingRatio)
        assertTrue(PaperMotion.sheetSettle.stiffness < PaperMotion.pickUp.stiffness)
    }

    @Test
    fun `should move the whole page slower than it moves one row`() {
        assertTrue(PaperMotion.pageMove.stiffness < PaperMotion.sheetSettle.stiffness)
        assertEquals(
            PaperMotion.sheetSettle.dampingRatio,
            PaperMotion.pageMove.dampingRatio,
            tolerance
        )
    }

    @Test
    fun `should never let ink overshoot the mark it is drawing`() {
        assertEquals(Spring.DampingRatioNoBouncy, PaperMotion.rowEnter.dampingRatio, tolerance)
        assertEquals(Spring.DampingRatioNoBouncy, PaperMotion.rowExit.dampingRatio, tolerance)
        assertEquals(Spring.DampingRatioNoBouncy, PaperMotion.rowFold.dampingRatio, tolerance)
    }

    @Test
    fun `should take ink off the page faster than it puts ink on it`() {
        assertTrue(PaperMotion.rowExit.stiffness > PaperMotion.rowEnter.stiffness)
        assertEquals(PaperMotion.rowExit.stiffness, PaperMotion.rowFold.stiffness, tolerance)
    }

    @Test
    fun `should carry ink faster than it carries paper`() {
        assertTrue(PaperMotion.rowEnter.stiffness > PaperMotion.pickUp.stiffness)
    }

    @Test
    fun `should place a reordered row and unfold a line on the one spring for space`() {
        assertEquals(
            PaperMotion.sheetSettle.dampingRatio,
            PaperMotion.rowPlacement.dampingRatio,
            tolerance
        )
        assertEquals(
            PaperMotion.sheetSettle.stiffness,
            PaperMotion.rowPlacement.stiffness,
            tolerance
        )
        assertEquals(
            PaperMotion.sheetSettle.stiffness,
            PaperMotion.rowUnfold.stiffness,
            tolerance
        )
    }

    @Test
    fun `should walk the tutorial hand at the pace of a whole-page move`() {
        assertEquals(PaperMotion.pageMove.stiffness, PaperMotion.handGlide.stiffness, tolerance)
        assertEquals(
            PaperMotion.pageMove.dampingRatio,
            PaperMotion.handGlide.dampingRatio,
            tolerance
        )
    }

    @Test
    fun `should reverse the breath instead of restarting it`() {
        assertEquals(RepeatMode.Reverse, PaperMotion.breath.repeatMode)
    }
}
