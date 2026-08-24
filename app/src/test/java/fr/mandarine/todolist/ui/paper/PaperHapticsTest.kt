package fr.mandarine.todolist.ui.paper

import android.view.HapticFeedbackConstants
import android.view.View
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

    @Test
    fun `should buzz the threshold when a row is picked up`() {
        view.performPickUpFeedback()

        assertEquals(
            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
            shadowOf(view).lastHapticFeedbackPerformed()
        )
    }

    @Test
    @Config(sdk = [29])
    fun `should fall back to a long press on platforms without a threshold buzz`() {
        val legacyView = View(RuntimeEnvironment.getApplication())

        legacyView.performPickUpFeedback()

        assertEquals(
            HapticFeedbackConstants.LONG_PRESS,
            shadowOf(legacyView).lastHapticFeedbackPerformed()
        )
    }

    @Test
    fun `should buzz a rejection when a row is torn off the page`() {
        view.performTearOffFeedback()

        assertEquals(
            HapticFeedbackConstants.REJECT,
            shadowOf(view).lastHapticFeedbackPerformed()
        )
    }

    @Test
    @Config(sdk = [29])
    fun `should fall back to a context click on platforms without a rejection buzz`() {
        val legacyView = View(RuntimeEnvironment.getApplication())

        legacyView.performTearOffFeedback()

        assertEquals(
            HapticFeedbackConstants.CONTEXT_CLICK,
            shadowOf(legacyView).lastHapticFeedbackPerformed()
        )
    }

    @Test
    fun `should buzz the end of the gesture when a lifted row is dropped`() {
        view.performDropFeedback()

        assertEquals(
            HapticFeedbackConstants.GESTURE_END,
            shadowOf(view).lastHapticFeedbackPerformed()
        )
    }

    @Test
    @Config(sdk = [29])
    fun `should fall back to a context click on platforms without a gesture end`() {
        val legacyView = View(RuntimeEnvironment.getApplication())

        legacyView.performDropFeedback()

        assertEquals(
            HapticFeedbackConstants.CONTEXT_CLICK,
            shadowOf(legacyView).lastHapticFeedbackPerformed()
        )
    }

    @Test
    fun `should tick once for every rule a lifted row crosses`() {
        view.performPassRuleFeedback()

        assertEquals(
            HapticFeedbackConstants.SEGMENT_TICK,
            shadowOf(view).lastHapticFeedbackPerformed()
        )
    }

    @Test
    @Config(sdk = [29])
    fun `should fall back to a clock tick on platforms without segment ticks`() {
        val legacyView = View(RuntimeEnvironment.getApplication())

        legacyView.performPassRuleFeedback()

        assertEquals(
            HapticFeedbackConstants.CLOCK_TICK,
            shadowOf(legacyView).lastHapticFeedbackPerformed()
        )
    }

    @Test
    fun `should buzz a confirmation when the platform knows the constant`() {
        view.performConfirmFeedback()

        assertEquals(
            HapticFeedbackConstants.CONFIRM,
            shadowOf(view).lastHapticFeedbackPerformed()
        )
    }

    @Test
    @Config(sdk = [29])
    fun `should fall back to a clock tick on platforms without confirm`() {
        val legacyView = View(RuntimeEnvironment.getApplication())

        legacyView.performConfirmFeedback()

        assertEquals(
            HapticFeedbackConstants.CLOCK_TICK,
            shadowOf(legacyView).lastHapticFeedbackPerformed()
        )
    }
}
