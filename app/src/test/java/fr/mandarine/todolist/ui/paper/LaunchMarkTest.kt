package fr.mandarine.todolist.ui.paper

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.R as SplashR
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.R
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val GRID = 108f
private const val SIDE = 432
private const val UNIT = SIDE / GRID
private const val SAFE_RADIUS = 33f
private const val OPAQUE = 0xFF000000.toInt()
private const val LEAST_INK = 0.02f
private const val MOST_INK = 0.20f
private const val NO_REFERENCE = 0

/**
 * The launcher tile and the window the app opens on are the same sheet of paper as
 * the page, and this is where that is held to: the icon is drawn with the pad's own
 * tones, the themed form keeps the one mark that is legible after the tint, and the
 * launch window is handed the very colour resource the page is painted with.
 *
 * The one place the two part company is the lamp. The window turns down with the
 * room because it is repainted every time it is shown; the launcher tile cannot,
 * because a launcher rasterises it once and keeps what it got, so a tile that reads
 * the room ends up showing whichever room it was first drawn in. That is what these
 * tests used to require of it, and it is what #44 was.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchMarkTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val palette = PaperPalette.light

    @Test
    fun `should stand the launcher icon on the same paper the page is written on`() {
        val ground = icon().background.pixelAt(54f, 54f)

        assertEquals(context.getColor(R.color.launcher_paper), ground)
        assertEquals(palette.paper.toArgb(), ground)
    }

    @Test
    @Config(qualifiers = "night")
    fun `should leave the launcher paper alone when the room turns down`() {
        val ground = icon().background.pixelAt(54f, 54f)

        assertEquals(palette.paper.toArgb(), ground)
        assertTrue("$ground", ground != PaperPalette.night.paper.toArgb())
    }

    @Test
    @Config(qualifiers = "night")
    fun `should give the launcher paper the same colour by night as by day`() {
        assertEquals(palette.paper.toArgb(), context.getColor(R.color.launcher_paper))
    }

    /**
     * The window is the half of the pair that *does* read the room, and the reason
     * the launcher's sheet needed a colour of its own rather than a night override
     * being deleted.
     */
    @Test
    @Config(qualifiers = "night")
    fun `should turn the window's paper down with the room`() {
        assertEquals(PaperPalette.night.paper.toArgb(), context.getColor(R.color.paper))
    }

    @Test
    fun `should write the icon with the pad's own tones`() {
        val face = icon().foreground

        assertEquals(palette.stickyNote.toArgb(), face.pixelAt(36f, 45f))
        assertEquals(palette.stickyNoteEdge.toArgb(), face.pixelAt(77f, 50f))
        assertEquals(palette.inkBlue.toArgb(), face.pixelAt(57.7f, 57.5f))
    }

    @Test
    fun `should shade the glued edge between the top sheet and the one under it`() {
        val glue = Color(icon().foreground.pixelAt(50f, 33f))

        assertTrue("$glue", glue.grey() < palette.stickyNote.grey())
        assertTrue("$glue", glue.grey() > palette.stickyNoteMid.grey())
    }

    @Test
    fun `should hand the launcher one mark that survives the monochrome tint`() {
        val mark = icon().monochrome

        assertNotNull(mark)
        val inked = checkNotNull(mark).render().count { it.ushr(24) > 0 }
        assertTrue("$inked inked", inked > LEAST_INK * SIDE * SIDE)
        assertTrue("$inked inked", inked < MOST_INK * SIDE * SIDE)
    }

    @Test
    fun `should keep the monochrome mark inside the mask the launcher cuts`() {
        val pixels = checkNotNull(icon().monochrome).render()

        var furthest = 0f
        pixels.forEachIndexed { at, pixel ->
            if (pixel.ushr(24) == 0) return@forEachIndexed
            val x = (at % SIDE) / UNIT - GRID / 2
            val y = (at / SIDE) / UNIT - GRID / 2
            furthest = maxOf(furthest, hypot(x, y))
        }

        assertTrue("reached $furthest", furthest < SAFE_RADIUS)
    }

    @Test
    fun `should open the launch window on the page's own ground`() {
        val launch = themeOf(R.style.Theme_ToDoList_Splash)
        val page = themeOf(R.style.Theme_ToDoList)

        assertEquals(R.color.paper, launch.referenceOf(SplashR.attr.windowSplashScreenBackground))
        assertEquals(R.color.paper, page.referenceOf(android.R.attr.windowBackground))
    }

    @Test
    fun `should hand the page over to the theme it is written in`() {
        val launch = themeOf(R.style.Theme_ToDoList_Splash)

        assertEquals(R.style.Theme_ToDoList, launch.referenceOf(SplashR.attr.postSplashScreenTheme))
    }

    @Test
    fun `should settle the note for as long as the animation is written to run`() {
        val launch = themeOf(R.style.Theme_ToDoList_Splash)
        val settle = launch.obtainStyledAttributes(
            intArrayOf(SplashR.attr.windowSplashScreenAnimationDuration)
        )
        val declared = settle.getInt(0, NO_REFERENCE)
        settle.recycle()

        assertEquals(
            R.drawable.avd_sticky_settle,
            launch.referenceOf(SplashR.attr.windowSplashScreenAnimatedIcon)
        )
        assertEquals(context.resources.getInteger(R.integer.splash_settle_millis), declared)
    }

    @Test
    fun `should settle the very note the launcher tile is stamped with`() {
        val settle = checkNotNull(context.getDrawable(R.drawable.avd_sticky_settle))

        assertTrue("$settle", settle is AnimatedVectorDrawable)
        assertEquals(palette.stickyNote.toArgb(), settle.pixelAt(36f, 45f))
        assertEquals(palette.stickyNoteEdge.toArgb(), settle.pixelAt(77f, 50f))
        assertEquals(palette.inkBlue.toArgb(), settle.pixelAt(57.7f, 57.5f))
    }

    private fun icon(): AdaptiveIconDrawable =
        checkNotNull(context.getDrawable(R.mipmap.ic_launcher)) as AdaptiveIconDrawable

    private fun themeOf(style: Int): Resources.Theme =
        context.resources.newTheme().apply { applyStyle(style, true) }

    private fun Resources.Theme.referenceOf(attr: Int): Int {
        val values = obtainStyledAttributes(intArrayOf(attr))
        val reference = values.getResourceId(0, NO_REFERENCE)
        values.recycle()
        return reference
    }

    private fun Drawable.render(): IntArray {
        val bitmap = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, SIDE, SIDE)
        draw(Canvas(bitmap))
        val pixels = IntArray(SIDE * SIDE)
        bitmap.getPixels(pixels, 0, SIDE, 0, 0, SIDE, SIDE)
        return pixels
    }

    private fun Drawable.pixelAt(x: Float, y: Float): Int {
        val pixels = render()
        return pixels[(y * UNIT).toInt() * SIDE + (x * UNIT).toInt()] or OPAQUE
    }

    private fun Color.grey(): Float = (red + green + blue) / 3f
}
