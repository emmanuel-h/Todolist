package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

private const val MOST_DOTS = 9
private const val HOLLOW = 0f
private const val INKED = 1f
private const val DOT_LABEL = "pageDot"
private val DOT = 7.dp
private val DOT_GAP = 6.dp
private val DOT_RIM = 1.2.dp

/**
 * Where the open list sits among the others, as a row of pencil circles with the
 * open one inked in — the mark a notebook's page tabs make at the edge of a closed
 * book. A long run keeps to [MOST_DOTS] circles centred on the open page, so the
 * row never runs out of its corner.
 */
@Composable
fun PageDots(
    current: Int,
    count: Int,
    spoken: String,
    modifier: Modifier = Modifier
) {
    if (count <= 1) return
    val palette = LocalPaperPalette.current
    val shown = minOf(count, MOST_DOTS)
    val first = (current - shown / 2).coerceIn(0, count - shown)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(DOT_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (page in first until first + shown) {
            val fill by animateFloatAsState(
                targetValue = if (page == current) INKED else HOLLOW,
                animationSpec = PaperMotion.pageMove,
                label = DOT_LABEL
            )
            val ink = palette.inked(InkTone.Words)
            val pencil = palette.inked(InkTone.Margin)
            Canvas(Modifier.size(DOT)) {
                val rim = DOT_RIM.toPx()
                val radius = size.minDimension / 2 - rim / 2
                drawCircle(pencil, radius, style = Stroke(rim))
                if (fill > HOLLOW) drawCircle(ink, (radius + rim / 2) * fill)
            }
        }
    }
}
