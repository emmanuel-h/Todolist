package fr.mandarine.todolist.ui.paper

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

private const val GESTURE_FEEDBACK_SDK = Build.VERSION_CODES.R
private const val TOGGLE_FEEDBACK_SDK = Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/**
 * One touch per physical moment on the page. Compose hands a feedback type
 * straight to the platform without checking that the platform knows it, so a
 * constant the page asks for is simply no feedback at all on a device older than
 * the release that added it. What each touch falls back to is therefore written
 * down once, here, rather than at every call site.
 */
@Immutable
class PaperHaptics(private val compose: HapticFeedback, private val view: View) {

    fun tick() = play(
        HapticFeedbackType.ToggleOn,
        TOGGLE_FEEDBACK_SDK,
        HapticFeedbackConstants.CLOCK_TICK
    )

    fun untick() = play(
        HapticFeedbackType.ToggleOff,
        TOGGLE_FEEDBACK_SDK,
        HapticFeedbackConstants.CLOCK_TICK
    )

    fun pickUp() = play(
        HapticFeedbackType.GestureThresholdActivate,
        TOGGLE_FEEDBACK_SDK,
        HapticFeedbackConstants.LONG_PRESS
    )

    fun pass() = play(
        HapticFeedbackType.SegmentTick,
        TOGGLE_FEEDBACK_SDK,
        HapticFeedbackConstants.CLOCK_TICK
    )

    fun drop() = play(
        HapticFeedbackType.GestureEnd,
        GESTURE_FEEDBACK_SDK,
        HapticFeedbackConstants.CLOCK_TICK
    )

    fun submit() = play(
        HapticFeedbackType.Confirm,
        GESTURE_FEEDBACK_SDK,
        HapticFeedbackConstants.CLOCK_TICK
    )

    fun tearOff() = play(
        HapticFeedbackType.Reject,
        GESTURE_FEEDBACK_SDK,
        HapticFeedbackConstants.CONTEXT_CLICK
    )

    fun land() = compose.performHapticFeedback(HapticFeedbackType.ContextClick)

    private fun play(type: HapticFeedbackType, since: Int, fallback: Int) {
        if (Build.VERSION.SDK_INT >= since) {
            compose.performHapticFeedback(type)
        } else {
            view.performHapticFeedback(fallback)
        }
    }
}

@Composable
fun rememberPaperHaptics(): PaperHaptics {
    val compose = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(compose, view) { PaperHaptics(compose, view) }
}
