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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val MARK_WIDTH = 52.dp
private val FOLD_AT_REST = 20.dp
private val FOLD_OPEN = 46.dp
private val CREASE_NIB = 1.dp
private val GLYPH_INSET = 5.dp
private val SHADOW_DROP = 2.dp
private const val CURL = 0.22f
private const val SHADOW_ALPHA = 0.18f
private const val LIT_EDGE = 0.35f
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
 * The flap is not a flat triangle. Paper lifted at a corner bows away from the
 * page: its free edge curves, its back catches the light along that edge and falls
 * into shade at the crease, and it throws a shadow on the writing underneath. A
 * triangle with none of that reads as a cut corner rather than a turned one.
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
    val shade = palette.paperShadeDeep
    val shadowInk = palette.shadow
    val creaseInk = palette.rule
    val flip = remember { Animatable(SHUT) }
    val scope = rememberCoroutineScope()
    val flap = Path()
    val cast = Path()
    val crease = Path()
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
                val side = FOLD_AT_REST.toPx() +
                    (FOLD_OPEN.toPx() - FOLD_AT_REST.toPx()) * open
                val ink = REST_INK + (FULLY_OPEN - REST_INK) * open
                val corner = if (atStart) SHUT else size.width
                val inward = if (atStart) side else -side
                val bow = side * CURL

                cast.reset()
                cast.moveTo(corner, SHADOW_DROP.toPx())
                cast.lineTo(corner + inward, SHADOW_DROP.toPx())
                cast.quadraticTo(
                    corner + inward * LIT_EDGE + bow * if (atStart) 1f else -1f,
                    side * LIT_EDGE + bow,
                    corner,
                    side + SHADOW_DROP.toPx()
                )
                cast.close()
                drawPath(cast, shadowInk, alpha = SHADOW_ALPHA * ink)

                flap.reset()
                flap.moveTo(corner, SHUT)
                flap.lineTo(corner + inward, SHUT)
                flap.quadraticTo(
                    corner + inward * LIT_EDGE + bow * if (atStart) 1f else -1f,
                    side * LIT_EDGE + bow,
                    corner,
                    side
                )
                flap.close()
                drawPath(
                    flap,
                    Brush.linearGradient(
                        colors = listOf(lit, shade),
                        start = Offset(corner + inward, SHUT),
                        end = Offset(corner, side)
                    ),
                    alpha = ink
                )

                crease.reset()
                crease.moveTo(corner + inward, SHUT)
                crease.lineTo(corner, side)
                inked(crease, creaseInk.copy(alpha = ink), InkNib(CREASE_NIB.toPx()))
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
