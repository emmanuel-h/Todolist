package fr.mandarine.todolist.ui.reorder

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cards on a board are not one height and lie in two directions, so a drag is
 * followed by where the finger is, not counted in rows.
 */
class BoardReorderTest {

    private val order = listOf("a", "b", "c", "d")

    @Test
    fun `should put the card in hand in the slot of the card under the finger`() {
        val step = nextBoardOrder(order, dragged = "a", hovered = "c", guard = SwapGuard())

        assertEquals(listOf("b", "c", "a", "d"), step.order)
        assertEquals("c", step.guard.lastSwapped)
        assertFalse(step.guard.leftIt)
    }

    @Test
    fun `should move a card back towards the start the same way`() {
        val step = nextBoardOrder(order, dragged = "d", hovered = "b", guard = SwapGuard())

        assertEquals(listOf("a", "d", "b", "c"), step.order)
    }

    @Test
    fun `should not swap straight back with the card just passed while the finger is still on it`() {
        val passed = SwapGuard(lastSwapped = "c", leftIt = false)

        val step = nextBoardOrder(listOf("b", "c", "a", "d"), dragged = "a", hovered = "c", guard = passed)

        assertEquals(listOf("b", "c", "a", "d"), step.order)
        assertEquals(passed, step.guard)
    }

    @Test
    fun `should swap back with the card just passed once the finger has left it`() {
        val left = nextBoardOrder(listOf("b", "c", "a", "d"), dragged = "a", hovered = null, guard = SwapGuard("c", false))

        val step = nextBoardOrder(left.order, dragged = "a", hovered = "c", guard = left.guard)

        assertTrue(left.guard.leftIt)
        assertEquals(listOf("b", "a", "c", "d"), step.order)
    }

    @Test
    fun `should leave the order alone over a card it may not be dropped among`() {
        val step = nextBoardOrder(order, dragged = "a", hovered = "finished", guard = SwapGuard())

        assertEquals(order, step.order)
    }

    @Test
    fun `should find the card whose bounds hold a point and nothing elsewhere`() {
        val cards = mapOf("a" to Rect(0f, 0f, 100f, 100f), "b" to Rect(110f, 0f, 210f, 60f))

        assertEquals("b", cardUnder(Offset(150f, 30f), cards))
        assertNull(cardUnder(Offset(150f, 80f), cards))
    }

    @Test
    fun `should carry a card over its neighbour and say the order changed`() {
        val changes = mutableListOf<List<String>>()
        val drag = BoardDrag { changes += it }
        drag.place("a", Rect(0f, 0f, 100f, 100f))
        drag.place("b", Rect(110f, 0f, 210f, 100f))

        assertTrue(drag.start("a", listOf("a", "b"), touch = Offset(50f, 50f)))
        assertTrue(drag.drag(Offset(110f, 0f)))

        assertEquals(listOf(listOf("b", "a")), changes)
        assertEquals("a", drag.draggedId)
    }

    @Test
    fun `should draw the card in hand at the finger however the board re-flows under it`() {
        val drag = BoardDrag { }
        drag.place("a", Rect(0f, 0f, 100f, 100f))
        drag.start("a", listOf("a"), touch = Offset(50f, 50f))

        drag.drag(Offset(30f, 20f))
        drag.place("a", Rect(0f, 200f, 100f, 300f))

        assertEquals(Offset(30f, -180f), drag.offset)
    }

    @Test
    fun `should refuse a second card while one is in hand`() {
        val drag = BoardDrag { }
        drag.place("a", Rect(0f, 0f, 100f, 100f))
        drag.place("b", Rect(110f, 0f, 210f, 100f))
        drag.start("a", listOf("a", "b"), Offset.Zero)

        assertFalse(drag.start("b", listOf("a", "b"), Offset.Zero))
    }

    @Test
    fun `should refuse a card the board has not placed or does not name`() {
        val drag = BoardDrag { }
        drag.place("done", Rect(0f, 0f, 100f, 100f))

        assertFalse(drag.start("unplaced", listOf("unplaced"), Offset.Zero))
        assertFalse(drag.start("done", listOf("a"), Offset.Zero))
    }

    @Test
    fun `should put the card down and draw it where it lies once cancelled`() {
        val drag = BoardDrag { }
        drag.place("a", Rect(0f, 0f, 100f, 100f))
        drag.start("a", listOf("a"), Offset.Zero)
        drag.drag(Offset(40f, 40f))

        drag.cancel()

        assertFalse(drag.dragging)
        assertEquals(Offset.Zero, drag.offset)
    }
}
