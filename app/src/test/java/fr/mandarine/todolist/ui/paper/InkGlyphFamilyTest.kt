package fr.mandarine.todolist.ui.paper

import android.content.Context
import android.content.res.XmlResourceParser
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * One pen drew every glyph in the app, and this is where that is checked: each is
 * stroked on the same 24 unit grid at the same weight with the same round nib, and
 * none of them is filled. A glyph imported from somewhere else fails here before
 * it can sit on a rule next to the others.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InkGlyphFamilyTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val family = mapOf(
        "check" to R.drawable.ic_check,
        "x" to R.drawable.ic_close,
        "plus" to R.drawable.ic_add,
        "arrow-left" to R.drawable.ic_arrow_back,
        "chevron-left" to R.drawable.ic_chevron_left,
        "chevron-right" to R.drawable.ic_chevron_right,
        "trash" to R.drawable.ic_delete,
        "undo" to R.drawable.ic_undo,
        "calendar" to R.drawable.ic_event,
        "alarm-clock" to R.drawable.ic_alarm,
        "circle-help" to R.drawable.ic_help,
        "pencil" to R.drawable.ic_edit,
        "list-checks" to R.drawable.ic_checklist
    )

    @Test
    fun `should draw every glyph on the same grid`() {
        val sides = mutableSetOf<Int>()
        family.forEach { (name, glyph) ->
            val vector = attributesOf(glyph, VECTOR)

            assertEquals(name, GRID, vector.getAttributeFloatValue(ANDROID, "viewportWidth", NONE))
            assertEquals(name, GRID, vector.getAttributeFloatValue(ANDROID, "viewportHeight", NONE))

            val drawn = checkNotNull(context.getDrawable(glyph))
            assertEquals(name, drawn.intrinsicWidth, drawn.intrinsicHeight)
            sides += drawn.intrinsicWidth
        }

        assertEquals(sides.toString(), ONE_SIDE, sides.size)
    }

    @Test
    fun `should stroke every glyph at one weight with one nib`() {
        family.forEach { (name, glyph) ->
            val path = attributesOf(glyph, PATH)

            assertEquals(name, NIB, path.getAttributeFloatValue(ANDROID, "strokeWidth", NONE))
            assertEquals(name, ROUND, path.getAttributeIntValue(ANDROID, "strokeLineCap", NO_CAP))
            assertEquals(name, ROUND, path.getAttributeIntValue(ANDROID, "strokeLineJoin", NO_CAP))
        }
    }

    @Test
    fun `should fill no glyph in the family`() {
        family.forEach { (name, glyph) ->
            val path = attributesOf(glyph, PATH)

            assertEquals(name, TRANSPARENT, path.getAttributeIntValue(ANDROID, "fillColor", OPAQUE))
        }
    }

    @Test
    fun `should write each glyph as one path so it takes one tint`() {
        family.forEach { (name, glyph) ->
            assertEquals(name, ONE_PATH, countOf(glyph, PATH))
        }
    }

    @Test
    fun `should mirror only the glyphs that point somewhere`() {
        val pointing = setOf(
            R.drawable.ic_arrow_back,
            R.drawable.ic_chevron_left,
            R.drawable.ic_chevron_right
        )

        family.forEach { (name, glyph) ->
            val mirrored = attributesOf(glyph, VECTOR)
                .getAttributeBooleanValue(ANDROID, "autoMirrored", false)

            assertEquals(name, glyph in pointing, mirrored)
        }
    }

    @Test
    fun `should leave no glyph on the page that the family does not own`() {
        val owned = family.values.map { context.resources.getResourceEntryName(it) }.toSet()
        val drawn = R.drawable::class.java.fields
            .map { it.name }
            .filterNot { it.startsWith(LAUNCHER) || it.startsWith(LAUNCH_ANIMATION) }
            .toSet()

        assertEquals(owned, drawn)
    }

    private fun attributesOf(glyph: Int, tag: String): XmlResourceParser {
        val parser = context.resources.getXml(glyph)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == tag) return parser
        }
        throw AssertionError("no <$tag> in ${context.resources.getResourceEntryName(glyph)}")
    }

    private fun countOf(glyph: Int, tag: String): Int {
        val parser = context.resources.getXml(glyph)
        var seen = 0
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == tag) seen += 1
        }
        return seen
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
        const val LAUNCHER = "ic_launcher"
        const val LAUNCH_ANIMATION = "avd_"
        const val VECTOR = "vector"
        const val PATH = "path"
        const val GRID = 24f
        const val NIB = 2f
        const val ONE_SIDE = 1
        const val ROUND = 1
        const val NO_CAP = -1
        const val NONE = 0f
        const val TRANSPARENT = 0
        const val OPAQUE = -1
        const val ONE_PATH = 1
    }
}
