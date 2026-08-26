package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import kotlinx.coroutines.launch

private val MARK_WIDTH = 52.dp
private val CURL_AT_REST = 16.dp
private val CURL_UNROLLED = 40.dp
private val CLEAR_OF_THE_EDGE = 11.dp
private val GLYPH_INSET = 5.dp
private val WAY_GLYPH = 13.dp
private val WAY_GAP = 1.dp
private const val WAY_INK = 0.75f
private val SHADOW_DROP = 3.dp
private const val SHADOW_ALPHA = 0.20f
private const val PAST_THE_CORNER = 0.2f
private const val INTO_THE_PAGE = 0.62f
private const val HALF = 0.5f
private const val TWO = 2f
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
 * The corner curls the way the corner of a note pinned to a wall curls: the tip
 * lifts off the line the row is written on and rolls back into the page, so what
 * is left is a crescent of the sheet's own back — lit along its free edge where it
 * has come away, falling into shade at the crease where it is still attached, and
 * throwing a shadow on the writing behind it.
 *
 * At rest the curl is small, the way a corner that has merely been handled is. It
 * unrolls the further the row is pulled.
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
    val flip = remember { Animatable(SHUT) }
    val scope = rememberCoroutineScope()
    val curl = Path()
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
                /**
                 * The curl lifts a little past where the corner was, and throws a
                 * shadow past that again. Both have to stay on the page, so the tip
                 * is seated inside the row's edge by as much as the widest curl
                 * will ever need.
                 */
                val clear = CLEAR_OF_THE_EDGE.toPx()
                val reach = minOf(
                    CURL_AT_REST.toPx() + (CURL_UNROLLED.toPx() - CURL_AT_REST.toPx()) * open,
                    minOf(size.width, size.height) - clear * TWO
                )
                val ink = REST_INK + (FULLY_OPEN - REST_INK) * open
                val tipX = if (atStart) clear else size.width - clear
                val tipY = size.height - clear
                val along = if (atStart) reach else -reach

                /**
                 * The tip lifts past where the corner was and the crease arcs back
                 * into the page, so what lies between the two is a crescent rather
                 * than a triangle — which is the difference between paper that has
                 * curled and paper that has been cut.
                 */
                val fromEdge = Offset(tipX + along, tipY)
                val toEdge = Offset(tipX, tipY - reach)
                val outward = Offset(-along * PAST_THE_CORNER, reach * PAST_THE_CORNER)
                val inward = Offset(along * INTO_THE_PAGE, -reach * INTO_THE_PAGE)
                val middle = Offset(
                    (fromEdge.x + toEdge.x) * HALF,
                    (fromEdge.y + toEdge.y) * HALF
                )

                curl.reset()
                curl.moveTo(fromEdge.x, fromEdge.y)
                curl.quadraticTo(
                    tipX + outward.x,
                    tipY + outward.y,
                    toEdge.x,
                    toEdge.y
                )
                curl.quadraticTo(
                    middle.x + inward.x,
                    middle.y + inward.y,
                    fromEdge.x,
                    fromEdge.y
                )
                curl.close()

                translate(
                    left = if (atStart) -SHADOW_DROP.toPx() else SHADOW_DROP.toPx(),
                    top = SHADOW_DROP.toPx()
                ) {
                    drawPath(curl, shadowInk, alpha = SHADOW_ALPHA * ink)
                }
                drawPath(
                    curl,
                    Brush.linearGradient(
                        colors = listOf(shade, lit),
                        start = Offset(middle.x + inward.x, middle.y + inward.y),
                        end = Offset(tipX + outward.x, tipY + outward.y)
                    ),
                    alpha = ink
                )
            },
        contentAlignment = if (atStart) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier.padding(GLYPH_INSET),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /**
             * A curled corner says the paper is loose. It does not say which way to
             * pull it, and after several attempts at a corner that would, the
             * plainest thing in the drawer turns out to be an arrow: the mark, and
             * beside it the way to go to reach it.
             *
             * It points inward from its own corner, because that is the way the
             * finger travels — the row is dragged towards the middle of the page
             * and the corner it uncovers is the one left behind.
             */
            if (!atStart) Wayfinder(atStart, turned)
            /**
             * The mark is quiet until the corner is being turned. A row carries two
             * of these and a page carries a dozen rows, so at rest they have to
             * read as the paper's own edge rather than as a dozen buttons.
             */
            InkIcon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    alpha = REST_INK + (FULLY_OPEN - REST_INK) * turned()
                },
                tint = palette.inked(InkTone.Margin),
                size = PaperDimens.jotGlyph
            )
            if (atStart) Wayfinder(atStart, turned)
        }
    }
}

/**
 * The arrow beside a corner's mark, pointing the way the row is pulled to reach
 * it. It fades out as the corner opens: once the reader is pulling, they no longer
 * need telling which way.
 */
@Composable
private fun Wayfinder(atStart: Boolean, turned: () -> Float) {
    val palette = LocalPaperPalette.current
    InkIcon(
        painter = painterResource(
            if (atStart) R.drawable.ic_chevron_right else R.drawable.ic_chevron_left
        ),
        contentDescription = null,
        modifier = Modifier
            .padding(if (atStart) PaddingValues(start = WAY_GAP) else PaddingValues(end = WAY_GAP))
            .graphicsLayer { alpha = WAY_INK * (FULLY_OPEN - turned()) },
        tint = palette.inked(InkTone.Margin),
        size = WAY_GLYPH
    )
}
