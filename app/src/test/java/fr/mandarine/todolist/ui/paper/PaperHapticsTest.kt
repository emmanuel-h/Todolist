package fr.mandarine.todolist.ui.paper

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaperHapticsTest {

    private val view = View(RuntimeEnvironment.getApplication())
    private val asked = mutableListOf<HapticFeedbackType>()
    private val haptics = PaperHaptics(RecordingHaptics(asked), view)

    @Test
    fun `should toggle on when an item is ticked`() {
        haptics.tick()

        assertEquals(listOf(HapticFeedbackType.ToggleOn), asked)
    }

    @Test
    fun `should toggle off when an item is restored`() {
        haptics.untick()

        assertEquals(listOf(HapticFeedbackType.ToggleOff), asked)
    }

    @Test
    fun `should buzz the threshold when a row is picked up`() {
        haptics.pickUp()

        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), asked)
    }

    @Test
    fun `should tick a segment for every rule a lifted row crosses`() {
        haptics.pass()

        assertEquals(listOf(HapticFeedbackType.SegmentTick), asked)
    }

    @Test
    fun `should end the gesture when a lifted row is dropped`() {
        haptics.drop()

        assertEquals(listOf(HapticFeedbackType.GestureEnd), asked)
    }

    @Test
    fun `should confirm what has been written to the page`() {
        haptics.submit()

        assertEquals(listOf(HapticFeedbackType.Confirm), asked)
    }

    @Test
    fun `should reject when a row is torn off the page`() {
        haptics.tearOff()

        assertEquals(listOf(HapticFeedbackType.Reject), asked)
    }

    @Test
    fun `should click when a sheet lands on the page`() {
        haptics.land()

        assertEquals(listOf(HapticFeedbackType.ContextClick), asked)
    }

    @Test
    @Config(sdk = [33])
    fun `should fall back to clock ticks on a platform without toggle feedback`() {
        haptics.tick()
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, lastOnTheView())

        haptics.untick()
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, lastOnTheView())

        haptics.pass()
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, lastOnTheView())

        assertEquals(emptyList<HapticFeedbackType>(), asked)
    }

    @Test
    @Config(sdk = [33])
    fun `should fall back to a long press for a pick up the platform cannot name`() {
        haptics.pickUp()

        assertEquals(HapticFeedbackConstants.LONG_PRESS, lastOnTheView())
        assertEquals(emptyList<HapticFeedbackType>(), asked)
    }

    @Test
    @Config(sdk = [33])
    fun `should keep asking the platform for the gestures it does know`() {
        haptics.drop()
        haptics.submit()
        haptics.tearOff()

        assertEquals(
            listOf(
                HapticFeedbackType.GestureEnd,
                HapticFeedbackType.Confirm,
                HapticFeedbackType.Reject
            ),
            asked
        )
    }

    @Test
    @Config(sdk = [29])
    fun `should fall back to a clock tick where a gesture cannot be ended or confirmed`() {
        haptics.drop()
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, lastOnTheView())

        haptics.submit()
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, lastOnTheView())

        assertEquals(emptyList<HapticFeedbackType>(), asked)
    }

    @Test
    @Config(sdk = [29])
    fun `should fall back to a context click where a tear cannot be rejected`() {
        haptics.tearOff()

        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, lastOnTheView())
        assertEquals(emptyList<HapticFeedbackType>(), asked)
    }

    private fun lastOnTheView(): Int = shadowOf(view).lastHapticFeedbackPerformed()

    private class RecordingHaptics(private val asked: MutableList<HapticFeedbackType>) :
        HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            asked += hapticFeedbackType
        }
    }
}
