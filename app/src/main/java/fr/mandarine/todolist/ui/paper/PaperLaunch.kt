package fr.mandarine.todolist.ui.paper

import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

private const val LIFT_OFF_MILLIS = 150L
private const val LIFT_OFF_SCALE = 0.85f
private const val GONE = 0f

/**
 * The launch window is the same sheet the page is written on, so handing over is a
 * note being lifted off the paper rather than one screen replacing another. The
 * window is held back while the page is still blank, then the note shrinks away
 * and the launch sheet fades where it stands: the ground under it never changes
 * tone, so all that arrives is the ink.
 */
fun ComponentActivity.openOnPaper(pageIsWritten: () -> Boolean) {
    val launch = installSplashScreen()
    launch.setKeepOnScreenCondition { !pageIsWritten() }
    launch.setOnExitAnimationListener { sheet ->
        sheet.iconView.animate()
            .alpha(GONE)
            .scaleX(LIFT_OFF_SCALE)
            .scaleY(LIFT_OFF_SCALE)
            .setDuration(LIFT_OFF_MILLIS)
            .start()
        sheet.view.animate()
            .alpha(GONE)
            .setDuration(LIFT_OFF_MILLIS)
            .withEndAction { sheet.remove() }
            .start()
    }
}
