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
 *
 * With one exception, and it is the reason [LEGACY_NAVIGATION_SCRIM] exists.
 * Drawing dark icons on the navigation bar arrived in API 26; below that the
 * platform draws them white whatever is asked of it, and `enableEdgeToEdge` knows
 * this — it applies the *dark* scrim to that bar on every older device, in either
 * light. A transparent dark scrim therefore meant white Back, Home and Recents on
 * a pale sheet on API 24 and 25, which is to say no navigation bar at all. From 26
 * up the icons follow the sheet and the bar stays clear.
 */
private val LEGACY_NAVIGATION_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

@Suppress("DEPRECATION")
fun ComponentActivity.drawEdgeToEdge() {
    val navigationScrim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Color.TRANSPARENT
    } else {
        LEGACY_NAVIGATION_SCRIM
    }
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, navigationScrim)
    )
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
