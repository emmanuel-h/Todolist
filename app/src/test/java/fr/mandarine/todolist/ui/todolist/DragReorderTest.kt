package fr.mandarine.todolist.ui.todolist

import fr.mandarine.todolist.domain.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DragReorderTest {

    private val uniform = listOf(100, 100, 100, 100)

    @Test
    fun `should stay on the same row when the drag is shorter than half a row`() {
        val settled = settleDrag(1, 49f, uniform)

        assertEquals(1, settled.index)
        assertEquals(49f, settled.offset, 0.01f)
    }

    @Test
    fun `should step down one row when the drag reaches half of the row below`() {
        val settled = settleDrag(1, 50f, uniform)

        assertEquals(2, settled.index)
        assertEquals(-50f, settled.offset, 0.01f)
    }

    @Test
    fun `should step up one row when the drag reaches half of the row above`() {
        val settled = settleDrag(2, -50f, uniform)

        assertEquals(1, settled.index)
        assertEquals(50f, settled.offset, 0.01f)
    }

    @Test
    fun `should cross several rows when the drag spans them in one move`() {
        val settled = settleDrag(0, 240f, uniform)

        assertEquals(2, settled.index)
        assertEquals(40f, settled.offset, 0.01f)
    }

    @Test
    fun `should settle instead of oscillating when the drag lands on exactly half a row`() {
        val settled = settleDrag(1, 50f, uniform)

        assertEquals(2, settled.index)
        assertEquals(-50f, settled.offset, 0.01f)
    }

    @Test
    fun `should clamp at the last row when the drag runs past the end`() {
        val settled = settleDrag(3, 900f, uniform)

        assertEquals(3, settled.index)
        assertEquals(900f, settled.offset, 0.01f)
    }

    @Test
    fun `should clamp at the first row when the drag runs past the start`() {
        val settled = settleDrag(0, -900f, uniform)

        assertEquals(0, settled.index)
        assertEquals(-900f, settled.offset, 0.01f)
    }

    @Test
    fun `should use the height of each neighbour when rows differ in height`() {
        val settled = settleDrag(0, 59f, listOf(56, 120, 56))

        assertEquals(0, settled.index)
        assertEquals(59f, settled.offset, 0.01f)
    }

    @Test
    fun `should step past a taller neighbour once half of its own height is covered`() {
        val settled = settleDrag(0, 61f, listOf(56, 120, 56))

        assertEquals(1, settled.index)
        assertEquals(-59f, settled.offset, 0.01f)
    }

    @Test
    fun `should reject a drag index outside the row list`() {
        val failure = runCatching { settleDrag(4, 0f, uniform) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `should move an item down the list`() {
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").moved(0, 2))
    }

    @Test
    fun `should move an item up the list`() {
        assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(2, 0))
    }

    @Test
    fun `should reject a move from outside the list`() {
        val failure = runCatching { listOf("a").moved(3, 0) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `should reject a move to outside the list`() {
        val failure = runCatching { listOf("a").moved(0, 3) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `should not scroll while the dragged row sits away from both edges`() {
        assertEquals(0f, scrollDelta(rowTop = 300f), 0.01f)
    }

    @Test
    fun `should scroll up when the dragged row enters the top edge`() {
        assertEquals(-6f, scrollDelta(rowTop = 36f), 0.01f)
    }

    @Test
    fun `should scroll down when the dragged row enters the bottom edge`() {
        assertEquals(6f, scrollDelta(rowTop = 908f), 0.01f)
    }

    @Test
    fun `should cap the scroll step when the dragged row is past the edge`() {
        assertEquals(-12f, scrollDelta(rowTop = -400f), 0.01f)
    }

    @Test
    fun `should not scroll when no edge band is configured`() {
        assertEquals(0f, scrollDelta(rowTop = 0f, edge = 0f), 0.01f)
    }

    @Test
    fun `should not scroll up when the list is already at its top`() {
        assertEquals(0f, scrollDelta(rowTop = 36f, canScrollUp = false), 0.01f)
    }

    @Test
    fun `should not scroll down when the list is already at its end`() {
        assertEquals(0f, scrollDelta(rowTop = 908f, canScrollDown = false), 0.01f)
    }

    @Test
    fun `should still scroll down when only the top of the list is unreachable`() {
        assertEquals(6f, scrollDelta(rowTop = 908f, canScrollUp = false), 0.01f)
    }

    @Test
    fun `should report no drag before one starts`() {
        val session = DragSession { }

        assertFalse(session.dragging)
        assertEquals(NO_DRAG_INDEX, session.index)
        assertEquals(0f, session.offset, 0.01f)
    }

    @Test
    fun `should publish a new order once the drag crosses a row`() {
        var published: List<String>? = null
        val session = DragSession { published = it }
        session.start(0, listOf("a", "b", "c"), uniform.take(3))

        session.drag(60f)

        assertEquals(listOf("b", "a", "c"), published)
        assertEquals(1, session.index)
    }

    @Test
    fun `should not publish an order while the drag stays within its own row`() {
        var published: List<String>? = null
        val session = DragSession { published = it }
        session.start(0, listOf("a", "b", "c"), uniform.take(3))

        session.drag(20f)

        assertNull(published)
        assertEquals(20f, session.offset, 0.01f)
    }

    @Test
    fun `should report the travelled distance when the drag ends on another row`() {
        val session = DragSession { }
        session.start(0, listOf("a", "b", "c"), uniform.take(3))
        session.drag(60f)

        assertEquals(Reorder(0, 1), session.end())
    }

    @Test
    fun `should report nothing when the drag ends where it started`() {
        val session = DragSession { }
        session.start(1, listOf("a", "b", "c"), uniform.take(3))
        session.drag(20f)

        assertNull(session.end())
    }

    @Test
    fun `should report nothing when the drag ends after returning to its own row`() {
        val session = DragSession { }
        session.start(0, listOf("a", "b", "c"), uniform.take(3))
        session.drag(60f)
        session.drag(-60f)

        assertNull(session.end())
    }

    @Test
    fun `should forget the drag once it ends`() {
        val session = DragSession { }
        session.start(0, listOf("a", "b"), listOf(100, 100))
        session.end()

        assertFalse(session.dragging)
        assertEquals(NO_DRAG_INDEX, session.index)
    }

    @Test
    fun `should forget the drag when it is cancelled`() {
        val session = DragSession { }
        session.start(0, listOf("a", "b"), listOf(100, 100))
        session.drag(60f)
        session.cancel()

        assertFalse(session.dragging)
        assertNull(session.end())
    }

    @Test
    fun `should ignore a drag delta when no drag is running`() {
        var published: List<String>? = null
        val session = DragSession { published = it }

        session.drag(400f)

        assertNull(published)
        assertEquals(0f, session.offset, 0.01f)
    }

    @Test
    fun `should expose the height of the row at a position`() {
        val session = DragSession { }
        session.start(0, listOf("a", "b"), listOf(56, 120))

        assertEquals(120, session.heightOf(1))
    }

    @Test
    fun `should report zero height for a position outside the drag`() {
        val session = DragSession { }

        assertEquals(0, session.heightOf(7))
    }

    @Test
    fun `should reject a drag start with mismatched ids and heights`() {
        val session = DragSession { }

        val failure = runCatching { session.start(0, listOf("a"), listOf(56, 56)) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `should reject a drag start outside the row list`() {
        val session = DragSession { }

        val failure = runCatching { session.start(2, listOf("a"), listOf(56)) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `should keep the repository order when no preview order is set`() {
        val items = listOf(item("a"), item("b"))

        assertEquals(items, orderActive(items, null))
    }

    @Test
    fun `should apply the preview order when one is set`() {
        val items = listOf(item("a"), item("b"), item("c"))

        val ordered = orderActive(items, listOf("c", "a", "b"))

        assertEquals(listOf("c", "a", "b"), ordered.map { it.id })
    }

    @Test
    fun `should fall back to the repository order when the preview is stale`() {
        val items = listOf(item("a"), item("b"))

        assertEquals(items, orderActive(items, listOf("a", "gone")))
    }

    private fun scrollDelta(
        rowTop: Float,
        rowHeight: Float = 56f,
        viewportHeight: Float = 1000f,
        edge: Float = 72f,
        maxStep: Float = 12f,
        canScrollUp: Boolean = true,
        canScrollDown: Boolean = true
    ): Float = autoScrollDelta(
        rowTop = rowTop,
        rowBottom = rowTop + rowHeight,
        viewportHeight = viewportHeight,
        edge = edge,
        maxStep = maxStep,
        canScrollUp = canScrollUp,
        canScrollDown = canScrollDown
    )

    private fun item(id: String) = TodoItem(id, "title-$id", "list-1")
}
