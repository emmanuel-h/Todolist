package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.graphics.Color
import fr.mandarine.todolist.domain.ListColour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Both palettes must answer every [ListColour] constant: a hue with no lamplit
 * twin is the bug this design exists to prevent, so the test iterates the entire
 * enum rather than spot-checking individual values.
 */
class PaperHighlightWashTest {

    @Test
    fun `should return transparent for None on the light palette`() {
        assertEquals(Color.Transparent, PaperPalette.light.highlightWash(ListColour.None))
    }

    @Test
    fun `should return transparent for None on the night palette`() {
        assertEquals(Color.Transparent, PaperPalette.night.highlightWash(ListColour.None))
    }

    @Test
    fun `should return a non-transparent colour for every hue on the light palette`() {
        ListColour.entries
            .filter { it != ListColour.None }
            .forEach { colour ->
                val wash = PaperPalette.light.highlightWash(colour)
                assertNotEquals(
                    "Light palette returned transparent for $colour",
                    Color.Transparent,
                    wash
                )
                assertEquals(
                    "Light palette wash for $colour must be fully opaque",
                    1f,
                    wash.alpha,
                    0.001f
                )
            }
    }

    @Test
    fun `should return a non-transparent colour for every hue on the night palette`() {
        ListColour.entries
            .filter { it != ListColour.None }
            .forEach { colour ->
                val wash = PaperPalette.night.highlightWash(colour)
                assertNotEquals(
                    "Night palette returned transparent for $colour",
                    Color.Transparent,
                    wash
                )
                assertEquals(
                    "Night palette wash for $colour must be fully opaque",
                    1f,
                    wash.alpha,
                    0.001f
                )
            }
    }

    @Test
    fun `should give each hue a different wash on the light palette`() {
        val hues = ListColour.entries.filter { it != ListColour.None }
        val washes = hues.map { PaperPalette.light.highlightWash(it) }
        assertEquals(
            "Some light washes are identical — each hue needs its own colour",
            hues.size,
            washes.distinct().size
        )
    }

    @Test
    fun `should give each hue a different wash on the night palette`() {
        val hues = ListColour.entries.filter { it != ListColour.None }
        val washes = hues.map { PaperPalette.night.highlightWash(it) }
        assertEquals(
            "Some night washes are identical — each hue needs its own lamplit twin",
            hues.size,
            washes.distinct().size
        )
    }

    @Test
    fun `should give each hue a different wash in night mode than in day mode`() {
        ListColour.entries
            .filter { it != ListColour.None }
            .forEach { colour ->
                assertNotEquals(
                    "Night and light washes for $colour are identical — the night twin must be tuned for the dark sheet",
                    PaperPalette.light.highlightWash(colour),
                    PaperPalette.night.highlightWash(colour)
                )
            }
    }

    @Test
    fun `should give light washes a higher luminance than night washes`() {
        ListColour.entries
            .filter { it != ListColour.None }
            .forEach { colour ->
                val light = PaperPalette.light.highlightWash(colour)
                val night = PaperPalette.night.highlightWash(colour)
                val lightLum = light.red * 0.2126f + light.green * 0.7152f + light.blue * 0.0722f
                val nightLum = night.red * 0.2126f + night.green * 0.7152f + night.blue * 0.0722f
                assert(lightLum > nightLum) {
                    "Light wash for $colour ($lightLum) is not brighter than its night twin ($nightLum)"
                }
            }
    }
}
