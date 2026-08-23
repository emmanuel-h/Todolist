package fr.mandarine.todolist.ui.paper

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

fun View.performPickUpFeedback() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

fun View.performConfirmFeedback() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}
