package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LIMIT = 100f
private const val FLAT = 0f
private const val ACROSS = 0f
private const val TOLERANCE = 0.001f
private const val FRAME_NANOS = 16_000_000L

class PaperOverscrollTest {

    private class FramesOnDemand : MonotonicFrameClock {

        private var elapsed = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            yield()
            elapsed += FRAME_NANOS
            return onFrame(elapsed)
        }
    }

    @Test
    fun `should bend the page at half the speed of the finger`() {
        val page = pageAtItsEnd()

        page.pull(40f)

        assertEquals(20f, page.bend, TOLERANCE)
    }

    @Test
    fun `should never bend the page further than a page bends`() {
        val page = pageAtItsEnd()

        page.pull(1000f)

        assertEquals(LIMIT, page.bend, TOLERANCE)
    }

    @Test
    fun `should bend the other way when the page is pulled past its last line`() {
        val page = pageAtItsEnd()

        page.pull(-40f)

        assertEquals(-20f, page.bend, TOLERANCE)
    }

    @Test
    fun `should give back everything it took when it bends the page`() {
        val page = pageAtItsEnd()

        val given = page.pull(40f)

        assertEquals(40f, given.y, TOLERANCE)
    }

    @Test
    fun `should give back only what the list scrolled when the page does not bend`() {
        val scrolled = 12f
        val page = PaperOverscrollEffect(LIMIT) { true }

        val given = page.applyToScroll(Offset(ACROSS, 40f), NestedScrollSource.UserInput) {
            Offset(ACROSS, scrolled)
        }

        assertEquals(scrolled, given.y, TOLERANCE)
    }

    @Test
    fun `should leave a downward pull alone while the keyboard is up`() {
        val page = PaperOverscrollEffect(LIMIT) { true }

        page.pull(40f)

        assertEquals(FLAT, page.bend, TOLERANCE)
    }

    @Test
    fun `should still bend upward while the keyboard is up`() {
        val page = PaperOverscrollEffect(LIMIT) { true }

        page.pull(-40f)

        assertEquals(-20f, page.bend, TOLERANCE)
    }

    @Test
    fun `should bend for the finger and for nothing else`() {
        val page = pageAtItsEnd()

        page.applyToScroll(Offset(ACROSS, 40f), NestedScrollSource.SideEffect) { Offset.Zero }

        assertEquals(FLAT, page.bend, TOLERANCE)
    }

    @Test
    fun `should unbend the page before the list scrolls again`() {
        val page = pageAtItsEnd()
        page.pull(40f)
        var offered = Float.NaN

        page.applyToScroll(Offset(ACROSS, -60f), NestedScrollSource.UserInput) { delta ->
            offered = delta.y
            delta
        }

        assertEquals(-20f, offered, TOLERANCE)
        assertEquals(FLAT, page.bend, TOLERANCE)
    }

    @Test
    fun `should take twice the travel to unbend that it took to bend`() {
        val page = pageAtItsEnd()
        page.pull(40f)

        page.applyToScroll(Offset(ACROSS, -20f), NestedScrollSource.UserInput) { it }

        assertEquals(10f, page.bend, TOLERANCE)
    }

    @Test
    fun `should call itself in progress only while the page is bent`() {
        val page = pageAtItsEnd()

        assertFalse(page.isInProgress)
        page.pull(40f)
        assertTrue(page.isInProgress)
    }

    @Test
    fun `should lay the page flat again when the finger lifts`() = runTest {
        val page = pageAtItsEnd()
        page.pull(40f)

        withContext(FramesOnDemand()) {
            page.applyToFling(Velocity.Zero) { Velocity.Zero }
        }

        assertEquals(FLAT, page.bend, TOLERANCE)
    }

    @Test
    fun `should let the list fling before it lays the page flat`() = runTest {
        val page = pageAtItsEnd()
        val thrown = Velocity(ACROSS, 900f)
        var flung: Velocity? = null

        withContext(FramesOnDemand()) {
            page.applyToFling(thrown) { velocity ->
                flung = velocity
                Velocity.Zero
            }
        }

        assertEquals(thrown, flung)
    }

    @Test
    fun `should spend nothing on unbending a page that is already flat`() {
        assertEquals(FLAT, unbendingDelta(delta = 30f, bend = FLAT), TOLERANCE)
    }

    @Test
    fun `should spend nothing on unbending when the finger pulls the way the page is bent`() {
        assertEquals(FLAT, unbendingDelta(delta = 30f, bend = 20f), TOLERANCE)
    }

    private fun pageAtItsEnd() = PaperOverscrollEffect(LIMIT) { false }

    private fun PaperOverscrollEffect.pull(delta: Float): Offset =
        applyToScroll(Offset(ACROSS, delta), NestedScrollSource.UserInput) { Offset.Zero }
}
