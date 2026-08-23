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
    fun `should buzz a long press when a row is picked up`() {
        view.performPickUpFeedback()

        assertEquals(
            HapticFeedbackConstants.LONG_PRESS,
            shadowOf(view).lastHapticFeedbackPerformed()
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
