package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * What a mark on the page is for, rather than which pigment it happens to be
 * written in. Every tint in the app is chosen as a tone and only then turned into
 * a colour, so what the page is allowed to spend can be read in one place.
 */
enum class InkTone { Words, Crossed, Margin, Today, Acted, Lost }

/**
 * The colour budget. A row at rest carries only the ink of its own words and the
 * pencil of its margin — the ring, the tally, the date jot. Blue ink is spent on
 * the one thing under the reader's finger and nowhere else; red only on a row
 * being torn off the page or on a day already missed.
 */
@Immutable
object InkBudget {

    val atRest: Set<InkTone> = setOf(InkTone.Words, InkTone.Crossed, InkTone.Margin, InkTone.Today)

    fun restsOn(tone: InkTone): Boolean = tone in atRest

    fun words(finished: Boolean): InkTone = if (finished) InkTone.Crossed else InkTone.Words

    fun ring(wet: Boolean): InkTone = if (wet) InkTone.Acted else InkTone.Margin

    fun toneOf(colour: Color, palette: PaperPalette): InkTone? =
        InkTone.entries.firstOrNull { palette.inked(it) == colour }
}

fun PaperPalette.inked(tone: InkTone): Color = when (tone) {
    InkTone.Words -> ink
    InkTone.Crossed -> inkDone
    InkTone.Margin -> pencil
    InkTone.Today -> inkAmber
    InkTone.Acted -> inkBlue
    InkTone.Lost -> inkRed
}
