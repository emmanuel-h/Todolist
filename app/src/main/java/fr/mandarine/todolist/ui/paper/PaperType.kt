package fr.mandarine.todolist.ui.paper

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.mandarine.todolist.R
import kotlin.math.roundToInt

internal const val BASELINE_LIFT = 0.04f
private const val EMOJI_SCALE = 0.8f
private const val EMOJI_PICTOGRAPHS_START = 0x1F000
private const val EMOJI_PICTOGRAPHS_END = 0x1FAFF
private const val EMOJI_SYMBOLS_START = 0x2600
private const val EMOJI_SYMBOLS_END = 0x27BF
private const val EMOJI_ARROWS_START = 0x2B00
private const val EMOJI_ARROWS_END = 0x2BFF
private const val EMOJI_VARIATION = 0xFE0F
private const val EMOJI_KEYCAP = 0x20E3
private const val EMOJI_JOINER = 0x200D

@Immutable
object PaperType {

    val hand = FontFamily(Font(R.font.patrick_hand))

    val base = TextStyle(
        fontFamily = hand,
        letterSpacing = 0.sp,
        lineHeight = 56.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Bottom,
            trim = LineHeightStyle.Trim.None
        ),
        lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Normal,
            wordBreak = LineBreak.WordBreak.Default
        )
    )

    val itemLine = base.copy(fontSize = 18.sp)
    val listLine = base.copy(fontSize = 20.sp)
    val margin = base.copy(fontSize = 14.sp)

    val caption = margin.offRule(20.sp)
    val prose = itemLine.offRule(26.sp)
    val field = listLine.offRule(28.sp)
}

/**
 * A line height written in `sp` does not survive the platform's non-linear font
 * scale: above scale 1.0 the same `56.sp` resolves to a different box for every
 * font size, and the smallest hand ends up the tallest. Each hand therefore takes
 * its leading in `em` of its own resolved size, so every one of them writes a line
 * exactly one pitch tall whatever the reader's font setting.
 */
@Immutable
class RuledHand(
    val itemLine: TextStyle = PaperType.itemLine,
    val listLine: TextStyle = PaperType.listLine,
    val margin: TextStyle = PaperType.margin
) {
    val typography = Typography(
        bodyLarge = itemLine,
        bodyMedium = itemLine,
        bodySmall = PaperType.caption,
        titleLarge = listLine,
        titleMedium = listLine,
        labelMedium = PaperType.caption,
        labelSmall = PaperType.caption
    )
}

val LocalRuledHand = staticCompositionLocalOf { RuledHand() }

@Composable
fun rememberRuledHand(pitch: Dp): RuledHand {
    val density = LocalDensity.current
    return remember(density, pitch) {
        val lineBox = with(density) { pitch.roundToPx() }.toFloat()
        RuledHand(
            itemLine = density.lineBoxOf(PaperType.itemLine, lineBox),
            listLine = density.lineBoxOf(PaperType.listLine, lineBox),
            margin = density.lineBoxOf(PaperType.margin, lineBox)
        )
    }
}

private fun Density.lineBoxOf(style: TextStyle, lineBox: Float): TextStyle =
    style.copy(lineHeight = (lineBox / style.fontSize.toPx()).em)

private fun TextStyle.offRule(leading: TextUnit): TextStyle = copy(
    lineHeight = leading,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

fun TextStyle.trimmedToGlyphs(): TextStyle = copy(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Bottom,
        trim = LineHeightStyle.Trim.Both
    )
)

/**
 * Every hand on the page sits on one baseline, a hair above the rule, whatever
 * size it is written at — so a date jot and a tally numeral rest on the same line
 * as the name they annotate. A name that wraps keeps its first line on that
 * baseline and drops each further line by exactly one pitch.
 */
@Composable
fun Modifier.seatOnRule(): Modifier {
    val seat = with(LocalDensity.current) {
        (LocalPagePitch.current.toPx() * (1f - BASELINE_LIFT)).roundToInt()
    }
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val baseline = placeable[FirstBaseline]
        val line = if (baseline == AlignmentLine.Unspecified) placeable.height else baseline
        layout(placeable.width, placeable.height) { placeable.place(0, seat - line) }
    }
}

/**
 * A drawn glyph carries its own blank border inside its box, so seating the box on
 * the rule leaves the mark itself hovering above the line. The foot of the ink —
 * not the foot of the box — is what lands on the writing line.
 */
@Composable
fun Modifier.seatGlyphOnRule(foot: Float): Modifier {
    val seat = with(LocalDensity.current) {
        (LocalPagePitch.current.toPx() * (1f - BASELINE_LIFT)).roundToInt()
    }
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val ink = (placeable.height * foot).roundToInt()
        layout(placeable.width, placeable.height) { placeable.place(0, seat - ink) }
    }
}

fun handwritten(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        if (!isPictograph(codePoint)) {
            index += Character.charCount(codePoint)
            continue
        }
        var end = index + Character.charCount(codePoint)
        while (end < text.length) {
            val next = text.codePointAt(end)
            if (!isPictograph(next)) break
            end += Character.charCount(next)
        }
        addStyle(SpanStyle(fontSize = EMOJI_SCALE.em), index, end)
        index = end
    }
}

private fun isPictograph(codePoint: Int): Boolean =
    codePoint in EMOJI_PICTOGRAPHS_START..EMOJI_PICTOGRAPHS_END ||
        codePoint in EMOJI_SYMBOLS_START..EMOJI_SYMBOLS_END ||
        codePoint in EMOJI_ARROWS_START..EMOJI_ARROWS_END ||
        codePoint == EMOJI_VARIATION ||
        codePoint == EMOJI_KEYCAP ||
        codePoint == EMOJI_JOINER
