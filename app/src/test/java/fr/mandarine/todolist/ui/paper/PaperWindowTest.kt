package fr.mandarine.todolist.ui.paper

import android.graphics.Color
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drawing dark icons on the navigation bar arrived in API 26. Below it the platform
 * draws them white whatever is asked, and `enableEdgeToEdge` knows that — it applies
 * the *dark* scrim to that bar on older devices in either light. A transparent dark
 * scrim therefore left white Back, Home and Recents on the pale sheet, which is to
 * say no navigation bar at all.
 */
@RunWith(RobolectricTestRunner::class)
class PaperWindowTest {

    @Test
    @Config(sdk = [25])
    fun `should scrim the navigation bar where its icons cannot be darkened`() {
        assertNotEquals(Color.TRANSPARENT, navigationBarColour())
    }

    @Test
    @Config(sdk = [24])
    fun `should scrim the navigation bar on the oldest supported device`() {
        assertNotEquals(Color.TRANSPARENT, navigationBarColour())
    }

    /**
     * From 26 up the icons follow the sheet, so the paper runs under a clear bar.
     */
    @Test
    @Config(sdk = [26])
    fun `should leave the navigation bar clear once its icons can be darkened`() {
        assertEquals(Color.TRANSPARENT, navigationBarColour())
    }

    @Test
    @Config(sdk = [34])
    fun `should leave the navigation bar clear on a current device`() {
        assertEquals(Color.TRANSPARENT, navigationBarColour())
    }

    @Test
    @Config(sdk = [34])
    fun `should leave the status bar clear`() {
        assertEquals(Color.TRANSPARENT, statusBarColour())
    }

    private fun navigationBarColour(): Int = withWindow { it.window.navigationBarColor }

    private fun statusBarColour(): Int = withWindow { it.window.statusBarColor }

    private fun <T> withWindow(read: (ComponentActivity) -> T): T =
        Robolectric.buildActivity(ComponentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.drawEdgeToEdge()
            read(activity)
        }
}
