package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp

private const val BLEED_ALPHA = 0.08f
private const val EDGE_ALPHA = 0.35f
private const val EDGE_SPREAD = 1.5f
private const val BLEED_SPREAD = 3.5f
private const val EDGE_DEPTH = 0.4f
private const val FULLY_INKED = 1f

/**
 * A pen does not lay down a flat line. The ink soaks a hair past the nib and dries
 * along the edge of the channel it cut, so a mark has a wet edge rather than a cut
 * one. Every mark the hand draws on this page — the ring, the tick, the strike
 * through a finished line, the check a swipe uncovers, the ring around a chosen
 * day — is laid down in these same three passes, so they all dry alike. The width
 * given is the width of the mark itself; the edge and the bleed lie outside it.
 */
fun DrawScope.inked(
    path: Path,
    colour: Color,
    width: Float,
    alpha: Float = FULLY_INKED,
    cap: StrokeCap = StrokeCap.Round
) = inked(path, colour, InkNib(width, cap), alpha)

/**
 * The three passes are one nib rather than three widths asked for again on every
 * frame. A hand that keeps writing the same mark — a ring on every row of a page
 * being flung, a strike through every finished line — cuts its nib once, where the
 * brushes and the tile are cut, and spends the scroll drawing with it.
 */
@Immutable
class InkNib(width: Float, cap: StrokeCap = StrokeCap.Round) {

    internal val bleed = Stroke(width = width + BLEED_SPREAD, cap = cap)
    internal val edge = Stroke(width = width + EDGE_SPREAD, cap = cap)
    internal val mark = Stroke(width = width, cap = cap)
}

fun DrawScope.inked(
    path: Path,
    colour: Color,
    nib: InkNib,
    alpha: Float = FULLY_INKED
) {
    drawPath(path = path, color = colour, alpha = BLEED_ALPHA * alpha, style = nib.bleed)
    drawPath(path = path, color = colour.dried(), alpha = EDGE_ALPHA * alpha, style = nib.edge)
    drawPath(path = path, color = colour, alpha = alpha, style = nib.mark)
}

/**
 * Ink dries denser at the edge of the channel the nib cut, which on a daylit page
 * means darker and on a night page — where the ink is chalk and the paper is the
 * dark thing — means paler. Either way the edge settles further from the paper
 * than the mark it rims.
 */
internal fun Color.dried(): Color =
    lerp(this, if (unlit) Color.Black else Color.White, EDGE_DEPTH)
