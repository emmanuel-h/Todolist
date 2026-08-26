package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.max

private val MARK_WIDTH = 44.dp
private val FOLD_AT_REST = 9.dp
private val FOLD_OPEN = 34.dp
private val FOLD_CREASE = 1.dp
private val GLYPH_INSET = 4.dp
private const val FOLD_SHADE_ALPHA = 0.10f
private const val REST_INK = 0.55f
private const val OPEN_INK = 1f
private const val NOTHING = 0f

/**
 * A corner of the row turned back, with the mark that says what turning it does.
 *
 * A row is a page and it comes away in the hand, but nothing at rest used to say
 * so: the mark under it was drawn only once the finger was already moving, so the
 * gesture had to be guessed before it could be seen. Both corners are turned a
 * little from the start now — pulling the row opens the one being pulled the rest
 * of the way, and pressing the corner does the same thing as pulling it home, so
 * the gesture is an invitation rather than the only way in.
 */
@Composable
fun CornerMark(
    painter: Painter,
    contentDescription: String?,
    atStart: Boolean,
    opened: () -> Float,
    onPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPaperPalette.current
    val back = palette.paperShadeDeep
    val shade = palette.shadow
    val creaseInk = palette.rule
    val fold = Path()
    val crease = Path()
    Box(
        modifier = modifier
            .width(MARK_WIDTH)
            .height(LocalPagePitch.current)
            .then(
                /**
                 * A corner is only named where nothing else on the row says what
                 * turning it does. The check on an item's corner is the ring's job,
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
                            onClick = onPress
                        )
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = contentDescription
                        }
                }
            )
            .drawBehind {
                val open = opened().coerceIn(NOTHING, OPEN_INK)
                val ink = REST_INK + (OPEN_INK - REST_INK) * open
                val side = max(
                    FOLD_AT_REST.toPx(),
                    FOLD_AT_REST.toPx() + (FOLD_OPEN.toPx() - FOLD_AT_REST.toPx()) * open
                )
                val corner = if (atStart) NOTHING else size.width
                val inward = if (atStart) side else -side
                fold.reset()
                fold.moveTo(corner, NOTHING)
                fold.lineTo(corner + inward, NOTHING)
                fold.lineTo(corner, side)
                fold.close()
                drawPath(fold, back, alpha = ink)
                drawPath(fold, shade, alpha = FOLD_SHADE_ALPHA * ink)
                crease.reset()
                crease.moveTo(corner + inward, NOTHING)
                crease.lineTo(corner, side)
                inked(crease, creaseInk.copy(alpha = ink), InkNib(FOLD_CREASE.toPx()))
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
                .graphicsLayer { alpha = REST_INK + (OPEN_INK - REST_INK) * opened() },
            tint = palette.inked(InkTone.Margin),
            size = PaperDimens.jotGlyph
        )
    }
}
