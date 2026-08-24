package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.mandarine.todolist.R
import kotlin.math.roundToInt

private const val SEAT_FRACTION = 0.2f
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

    val typography = Typography(
        bodyLarge = itemLine,
        bodyMedium = itemLine,
        bodySmall = caption,
        titleLarge = listLine,
        titleMedium = listLine,
        labelMedium = caption,
        labelSmall = caption
    )
}

private fun TextStyle.offRule(leading: TextUnit): TextStyle = copy(
    lineHeight = leading,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

fun Modifier.seatOnRule(style: TextStyle): Modifier = offset {
    IntOffset(0, (style.fontSize.toPx() * SEAT_FRACTION).roundToInt())
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
