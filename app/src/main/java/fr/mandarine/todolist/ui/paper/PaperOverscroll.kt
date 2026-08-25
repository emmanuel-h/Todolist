package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

private const val PAPER_RESISTANCE = 0.5f
private const val FLAT = 0f
private const val CURL_SHADE = 0.1f

internal val PAGE_BEND_LIMIT = 32.dp
private val CURL_DEPTH = 48.dp

/**
 * A sheet of paper does not stretch. Pulled past its last line it bends: it gives
 * ground at half the speed of the finger, only so far, and the edge that was pulled
 * catches the light differently for as long as it is lifted. Letting go lays it flat
 * again on the slow spring every page-sized movement travels on.
 *
 * What is given back to the scrolling container is only what was really taken —
 * never the whole delta — because whatever is left over is offered to the parents
 * afterwards, and the gesture that pushes the keyboard back down lives there. That
 * gesture is a downward pull with the keyboard up, which is exactly the pull this
 * would otherwise swallow, so it is the one pull the page refuses to bend to.
 */
@Stable
class PaperOverscrollEffect(
    private val limit: Float,
    private val keyboardShown: () -> Boolean
) : OverscrollEffect {

    private var pulled by mutableFloatStateOf(FLAT)

    val bend: Float get() = pulled

    override val isInProgress: Boolean get() = pulled != FLAT

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        val unbending = unbendingDelta(delta.y, pulled)
        val consumed = performScroll(Offset(delta.x, delta.y - unbending))
        val leftover = delta.y - unbending - consumed.y
        val absorbed = if (absorbsPull(leftover, source, keyboardShown())) leftover else FLAT
        val taken = unbending + absorbed
        if (taken != FLAT) pulled = bentTo(pulled, taken, limit)
        return Offset(consumed.x, consumed.y + taken)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        performFling(velocity)
        if (pulled != FLAT) {
            Animatable(pulled).animateTo(FLAT, PaperMotion.pageMove) { pulled = value }
        }
    }
}

/**
 * How much of a finger movement goes into laying the page flat again before any of
 * it reaches the list underneath. A page already bent one way is unbent by the
 * movement that opposes it, and it takes twice the travel to unbend as it took to
 * bend, because both directions meet the same resistance.
 */
internal fun unbendingDelta(delta: Float, bend: Float): Float {
    if (bend == FLAT || sign(delta) == sign(bend)) return FLAT
    return sign(delta) * min(abs(delta), abs(bend) / PAPER_RESISTANCE)
}

internal fun bentTo(bend: Float, delta: Float, limit: Float): Float =
    (bend + delta * PAPER_RESISTANCE).coerceIn(-limit, limit)

internal fun absorbsPull(
    leftover: Float,
    source: NestedScrollSource,
    keyboardShown: Boolean
): Boolean = leftover != FLAT &&
    source == NestedScrollSource.UserInput &&
    !(leftover > FLAT && keyboardShown)

/**
 * The bend belongs to the page rather than to the rows on it, so it is held here
 * and worn by the whole sheet — ruling, ink and all. A reader who has asked for
 * stillness gets no bend at all, and the list falls back to no overscroll rather
 * than to the stretch the paper was hiding.
 */
@Composable
fun rememberPageBend(animated: Boolean): PaperOverscrollEffect? {
    val limit = with(LocalDensity.current) { PAGE_BEND_LIMIT.toPx() }
    val keyboard = rememberUpdatedState(keyboardVisible())
    val bend = remember(limit) { PaperOverscrollEffect(limit) { keyboard.value } }
    return if (animated) bend else null
}

/**
 * A pulled edge is a lifted edge: in daylight it lays a soft shade along itself,
 * and under a lamp it is the one edge turned far enough to catch the light. Both
 * are read at draw time, so a page bending costs no recomposition.
 */
@Composable
fun Modifier.pageBend(bend: PaperOverscrollEffect?): Modifier {
    if (bend == null) return this
    val palette = LocalPaperPalette.current
    val curl = if (palette.byLamplight) palette.lift else palette.shadow
    return this
        .graphicsLayer { translationY = bend.bend }
        .drawWithCache {
            val depth = CURL_DEPTH.toPx()
            val fromTop = Brush.verticalGradient(
                listOf(curl, Color.Transparent),
                startY = FLAT,
                endY = depth
            )
            val fromBottom = Brush.verticalGradient(
                listOf(Color.Transparent, curl),
                startY = size.height - depth,
                endY = size.height
            )
            val limit = PAGE_BEND_LIMIT.toPx()
            onDrawWithContent {
                drawContent()
                val pulled = bend.bend
                val shade = CURL_SHADE * (abs(pulled) / limit).coerceIn(FLAT, 1f)
                if (shade > FLAT) {
                    drawRect(
                        brush = if (pulled > FLAT) fromTop else fromBottom,
                        topLeft = Offset(FLAT, if (pulled > FLAT) FLAT else size.height - depth),
                        size = Size(size.width, depth),
                        alpha = shade
                    )
                }
            }
        }
}
