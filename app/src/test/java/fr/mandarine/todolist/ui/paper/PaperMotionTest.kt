package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Spring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperMotionTest {

    private val tolerance = 0.0001f

    @Test
    fun `should make the lift overshoot so the sheet reads as peeled off the pad`() {
        assertTrue(PaperMotion.sheetLift.dampingRatio < Spring.DampingRatioNoBouncy)
        assertTrue(PaperMotion.sheetLift.stiffness > PaperMotion.sheetSettle.stiffness)
    }

    @Test
    fun `should make the settle slower and calmer than the lift`() {
        assertTrue(PaperMotion.sheetSettle.dampingRatio > PaperMotion.sheetLift.dampingRatio)
        assertTrue(PaperMotion.sheetSettle.stiffness < PaperMotion.sheetLift.stiffness)
    }

    @Test
    fun `should let a row enter with a trace of bounce and leave with none`() {
        assertTrue(PaperMotion.rowEnter.dampingRatio < Spring.DampingRatioNoBouncy)
        assertEquals(
            Spring.DampingRatioNoBouncy,
            PaperMotion.rowExit.dampingRatio,
            tolerance
        )
    }

    @Test
    fun `should move a row out faster than it lets one in`() {
        assertTrue(PaperMotion.rowExit.stiffness > PaperMotion.rowEnter.stiffness)
    }

    @Test
    fun `should place a reordered row on the same spring a new row enters on`() {
        assertEquals(
            PaperMotion.rowEnter.dampingRatio,
            PaperMotion.rowPlacement.dampingRatio,
            tolerance
        )
        assertEquals(
            PaperMotion.rowEnter.stiffness,
            PaperMotion.rowPlacement.stiffness,
            tolerance
        )
    }

    @Test
    fun `should offer an instant spec that neither bounces nor lingers`() {
        assertEquals(
            Spring.DampingRatioNoBouncy,
            PaperMotion.instant.dampingRatio,
            tolerance
        )
        assertEquals(Spring.StiffnessHigh, PaperMotion.instant.stiffness, tolerance)
    }
}
