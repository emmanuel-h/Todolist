package fr.mandarine.todolist.ui.paper

import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * The page rides the keyboard rather than being covered by it, which needs the
 * window to resize as the keyboard animates. Setting it here rather than in the
 * manifest keeps both screens on the same footing whatever their manifest entry
 * says. The bars take no scrim in either light: the paper runs under them, and
 * the icons drawn on them follow whichever sheet the room calls for.
 */
@Suppress("DEPRECATION")
fun ComponentActivity.drawEdgeToEdge() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
    )
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
