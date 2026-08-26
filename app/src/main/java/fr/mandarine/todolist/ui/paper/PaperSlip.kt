package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

private const val OFF_THE_PAGE = 0f
private val SLIP_SHADOW = 6.dp
private val SLIP_DROP = 3.dp
private const val SLIP_SHADOW_ALPHA = 0.16f
private val CAPTION_PADDING = 14.dp
private val CAPTION_GLYPH_GAP = 8.dp

/**
 * A slip of the same paper the sheets are cut from, laid on the page: square
 * corners, the page's grain, one shallow warm shadow to lift it off the writing
 * and no outline at all.
 */
@Composable
fun Modifier.paperSlip(): Modifier {
    val palette = LocalPaperPalette.current
    val shadowInk = palette.shadow
    return this
        .raised(RectangleShape, palette) {
            dropShadow(RectangleShape) {
                radius = SLIP_SHADOW.toPx()
                offset = Offset(OFF_THE_PAGE, SLIP_DROP.toPx())
                color = shadowInk
                alpha = SLIP_SHADOW_ALPHA
            }
        }
        .paperSheet(tone = palette.paperShade)
}

/**
 * A slip with a glyph and a few words on it — the app explaining rather than
 * showing. It reads as a note left on the page rather than as part of the page,
 * which is what keeps the words from becoming furniture: a note is something
 * somebody put there and will take away again.
 */
@Composable
fun PaperSlipCaption(painter: Painter, text: String, modifier: Modifier = Modifier) {
    val palette = LocalPaperPalette.current
    Row(
        modifier = modifier.paperSlip().padding(CAPTION_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InkIcon(
            painter = painter,
            contentDescription = null,
            tint = palette.inked(InkTone.Margin),
            size = PaperDimens.iconGlyph
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = CAPTION_GLYPH_GAP),
            color = palette.inked(InkTone.Words),
            style = PaperType.prose
        )
    }
}
