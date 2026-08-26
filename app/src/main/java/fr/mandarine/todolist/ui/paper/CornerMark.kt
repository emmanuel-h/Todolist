package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val MARK_WIDTH = 52.dp
private val REACH_AT_REST = 7.dp
private val REACH_ROLLED = 46.dp
private val ROLL_AT_REST = 10.dp
private val ROLL_ROLLED = 16.dp
private val CLEAR_OF_THE_RULE = 2.dp
private val GLYPH_INSET = 5.dp
private val SHADOW_DROP = 2.dp
private const val SHADOW_ALPHA = 0.22f
private const val TWO = 2f
private const val EPSILON = 0.0001f
private const val REST_INK = 0.6f
private const val FULLY_OPEN = 1f
private const val SHUT = 0f

/**
 * A corner of the row turned back, with the mark that says what turning it does.
 *
 * A row is a page and it comes away in the hand, but nothing at rest used to say
 * so: the mark under it was drawn only once the finger was already moving, so the
 * gesture had to be guessed before it could be seen. Both corners are turned from
 * the start now — pulling the row opens the one being pulled the rest of the way,
 * and pressing the corner turns it right back on its own before doing the same
 * thing, so the gesture is an invitation rather than the only way in.
 *
 * At rest the corner is only rounded off — a bead of paper where the point would
 * be, saying the corner is loose without claiming it has been turned. Pulling the
 * row rolls it: the bead lengthens into a tube of paper lying across the corner,
 * lit along its crown and in shade where it meets the page on either side, with a
 * shadow under it and both ends round, because a rolled corner has no points left
 * on it.
 */
@Composable
fun CornerMark(
    painter: Painter,
    contentDescription: String?,
    atStart: Boolean,
    opened: () -> Float,
    onPress: () -> Unit,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalPaperPalette.current
    val lit = palette.paperSheet
    val shade = palette.paperShade
    val shadowInk = palette.shadow
    val flip = remember { Animatable(SHUT) }
    val scope = rememberCoroutineScope()
    val turned: () -> Float = { maxOf(opened(), flip.value).coerceIn(SHUT, FULLY_OPEN) }
    Box(
        modifier = modifier
            .width(MARK_WIDTH)
            .height(LocalPagePitch.current)
            .then(
                /**
                 * A corner is only named where nothing else on the row says what
                 * turning it does. The tick on an item's corner is the ring's job,
                 * already sitting on the row and already named, so the corner there
                 * is the paper's own edge and nothing more.
                 */
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier
                        .clickable(
                            onClickLabel = contentDescription,
                            role = Role.Button,
                            onClick = {
                                if (!animated) {
                                    onPress()
                                } else {
                                    scope.launch {
                                        flip.snapTo(SHUT)
                                        flip.animateTo(FULLY_OPEN, PaperMotion.pickUp)
                                        onPress()
                                        flip.animateTo(SHUT, PaperMotion.sheetSettle)
                                    }
                                }
                            }
                        )
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = contentDescription
                        }
                }
            )
            .drawBehind {
                val open = turned()
                val wanted = REACH_AT_REST.toPx() +
                    (REACH_ROLLED.toPx() - REACH_AT_REST.toPx()) * open
                val thickness = ROLL_AT_REST.toPx() +
                    (ROLL_ROLLED.toPx() - ROLL_AT_REST.toPx()) * open
                val ink = REST_INK + (FULLY_OPEN - REST_INK) * open
                val corner = if (atStart) SHUT else size.width
                /**
                 * The roll is round at both ends, so one laid on the row's top edge
                 * would stand half its own thickness above it and cross the rule
                 * the row above is written on. It sits clear of that line, and is
                 * only as long as the rule below leaves room for — whatever the
                 * reader's font scale has done to the space between the two.
                 */
                val top = thickness / TWO + CLEAR_OF_THE_RULE.toPx()
                val reach = minOf(wanted, size.height - top * TWO)
                val inward = if (atStart) reach else -reach

                val from = Offset(corner + inward, top)
                val to = Offset(corner, top + reach)
                val across = Offset(to.y - from.y, from.x - to.x).let { edge ->
                    val length = maxOf(EPSILON, kotlin.math.hypot(edge.x, edge.y))
                    Offset(edge.x / length, edge.y / length)
                }
                val middle = Offset((from.x + to.x) / TWO, (from.y + to.y) / TWO)
                drawLine(
                    color = shadowInk,
                    start = from + across * SHADOW_DROP.toPx(),
                    end = to + across * SHADOW_DROP.toPx(),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round,
                    alpha = SHADOW_ALPHA * ink
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(shade, lit, shade),
                        start = middle - across * thickness / TWO,
                        end = middle + across * thickness / TWO
                    ),
                    start = from,
                    end = to,
                    strokeWidth = thickness,
                    cap = StrokeCap.Round,
                    alpha = ink
                )
            },
        contentAlignment = if (atStart) Alignment.BottomStart else Alignment.BottomEnd
    ) {
        /**
         * The mark is quiet until the corner is being turned. A row carries two of
         * these and a page carries a dozen rows, so at rest they have to read as
         * the paper's own edge rather than as a dozen buttons.
         */
        InkIcon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .padding(GLYPH_INSET)
                .graphicsLayer { alpha = REST_INK + (FULLY_OPEN - REST_INK) * turned() },
            tint = palette.inked(InkTone.Margin),
            size = PaperDimens.jotGlyph
        )
    }
}
