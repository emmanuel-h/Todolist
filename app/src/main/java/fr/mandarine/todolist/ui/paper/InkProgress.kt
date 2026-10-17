package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private const val NOTHING_DONE = 0f
private const val ALL_DONE = 1f
private const val PROGRESS_LABEL = "inkProgress"
private val STROKE = 3.dp

/**
 * How far through a list the reader is, drawn as a pen line laid over a pencil one:
 * the pencil is the whole list, the ink is what has been ticked off. It is spent in
 * the reader's own ink rather than blue, because at rest the page carries no blue —
 * blue is for the one thing under the finger.
 */
@Composable
fun InkProgress(
    done: Int,
    total: Int,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val palette = LocalPaperPalette.current
    val target = if (total > 0) done.toFloat() / total else NOTHING_DONE
    val fraction by animateFloatAsState(
        targetValue = target.coerceIn(NOTHING_DONE, ALL_DONE),
        animationSpec = if (animated) PaperMotion.pageMove else snap(),
        label = PROGRESS_LABEL
    )
    val track = palette.rule
    val ink = palette.inked(InkTone.Words)
    Canvas(
        modifier = modifier
            .height(STROKE)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(target, NOTHING_DONE..ALL_DONE)
            }
    ) {
        val stroke = size.height
        val y = size.height / 2
        val start = stroke / 2
        val end = size.width - stroke / 2
        val rtl = layoutDirection == LayoutDirection.Rtl
        drawLine(track, Offset(start, y), Offset(end, y), stroke, StrokeCap.Round)
        if (fraction > NOTHING_DONE) {
            val reach = start + (end - start) * fraction
            val from = if (rtl) Offset(end, y) else Offset(start, y)
            val to = if (rtl) Offset(end - (reach - start), y) else Offset(reach, y)
            drawLine(ink, from, to, stroke, StrokeCap.Round)
        }
    }
}
