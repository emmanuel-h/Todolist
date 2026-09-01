package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

enum class RowSwipe { Delete, Rest, Reveal }

enum class SwipeMark { Check, Pencil }

/**
 * What a row uncovers when it is dragged start to end. A row that can be finished
 * uncovers a check; a row that has nothing to finish uncovers the pencil that
 * edits it, so the same gesture always means "the other thing this row does".
 * The check is answered by the ring drawing its own tick, which carries its own
 * touch; the pencil opens a surface that has none, so the gesture signs off itself.
 */
@Immutable
class SwipeReveal(val mark: SwipeMark, val label: String?, val perform: () -> Unit) {
    internal val answeredInInk: Boolean get() = mark == SwipeMark.Check
}

private val SWIPE_TRAVEL = 96.dp
private val SWIPE_FLICK = 125.dp
private const val SWIPE_THRESHOLD = 0.6f
private const val SWIPE_MEANT_IT = 0.25f
private const val SWIPE_RESISTANCE = 0.35f
private const val AT_REST = 0f
private const val FULLY_DRAWN = 1f

/**
 * A row is paper, not a panel on rails: it follows the finger exactly as far as
 * the mark it uncovers takes to draw, and past that it grows heavy, so the page
 * pushes back instead of letting the row fly off it. What the release does is
 * decided the instant the finger lifts — the row springs straight home from
 * wherever it was let go of, and the mark it was drawing is what happens.
 */
@Stable
internal class RowSwipeState(private val travel: Float, private val reveals: Boolean) {

    private var pulled by mutableFloatStateOf(AT_REST)

    /**
     * The furthest the row was ever taken during this one gesture, sign and all.
     * A finger lifting off a swipe very often flicks back a little, and reading the
     * row's position at that instant meant the flick could land it on the other
     * side of the page and do the opposite thing to what the reader had watched
     * themselves uncover. What was uncovered is what happens.
     */
    private var furthest = AT_REST

    val offset: Float get() = weightedSwipe(pulled, travel)

    val travelling: Boolean get() = pulled != AT_REST

    val locked: Boolean get() = abs(offset) >= travel * SWIPE_THRESHOLD

    fun begin() {
        furthest = AT_REST
    }

    fun drag(delta: Float) {
        val reached = pulled + delta
        pulled = if (reveals) reached else reached.coerceAtMost(AT_REST)
        if (abs(offset) > abs(furthest)) furthest = offset
    }

    /**
     * A swipe is answered in the direction it was taken, or not at all — never in
     * the other one. Which way it was taken is the furthest the row ever got, so a
     * finger flicking back off it as it lifts cannot turn a tear into an edit.
     *
     * Whether it is answered at all is where the row is when the finger goes: it
     * has to still be held far enough over to have its mark drawn whole, or to have
     * been thrown that way from far enough to mean it. Easing the row back towards
     * home is how the reader changes their mind, and it works from anywhere — they
     * should not have to drag it all the way past the middle to be let off.
     */
    fun landing(velocity: Float, flick: Float): RowSwipe {
        if (furthest == AT_REST) return RowSwipe.Rest
        val direction = sign(furthest)
        if (sign(offset) != direction) return RowSwipe.Rest
        val thrown = abs(velocity) >= flick &&
            sign(velocity) == direction &&
            abs(furthest) >= travel * SWIPE_MEANT_IT
        if (!locked && !thrown) return RowSwipe.Rest
        return if (direction < AT_REST) RowSwipe.Delete else RowSwipe.Reveal
    }

    suspend fun springHome(spec: AnimationSpec<Float>) {
        Animatable(pulled).animateTo(AT_REST, spec) { pulled = value }
    }
}

/**
 * Paper drags freely while the mark is still being drawn and grows heavy once it
 * is complete, so the row can be pushed further but never thrown away.
 */
internal fun weightedSwipe(travelled: Float, travel: Float): Float {
    val excess = abs(travelled) - travel
    if (excess <= AT_REST) return travelled
    return sign(travelled) * (travel + excess * SWIPE_RESISTANCE)
}

/**
 * The row itself is the moving paper: it slides over the page and the mark it is
 * being dragged towards is drawn on the bare sheet it uncovers, so nothing but
 * the row and the ink ever moves.
 */
@Composable
fun SwipeRow(
    key: Any,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    reveal: SwipeReveal? = null,
    tearLabel: String,
    enabled: Boolean = true,
    animated: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = LocalPaperPalette.current
    val haptics = rememberPaperHaptics()
    val density = LocalDensity.current
    val pitch = LocalPagePitch.current
    val travel = with(density) { SWIPE_TRAVEL.toPx() }
    val flick = with(density) { SWIPE_FLICK.toPx() }
    val revealMark = reveal?.mark
    val trash = painterResource(R.drawable.ic_delete)
    val pencil = painterResource(R.drawable.ic_edit)
    val tick = painterResource(R.drawable.ic_check)

    val swipe = remember(key, revealMark, travel) { RowSwipeState(travel, revealMark != null) }
    val settle = if (animated) PaperMotion.sheetSettle else snap()
    val latestDelete = rememberUpdatedState(onDelete)
    val latestReveal = rememberUpdatedState(reveal)

    val pulled: () -> Float = { swipe.offset }

    val locked by remember(swipe) { derivedStateOf { swipe.locked } }
    val travelling by remember(swipe) { derivedStateOf { swipe.travelling } }

    LaunchedEffect(locked) { if (locked) haptics.pickUp() }

    val pull = rememberDraggableState { delta -> swipe.drag(delta) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(pulled().roundToInt(), 0) }
                .preferredFrameRate(
                    if (travelling) FrameRateCategory.High else FrameRateCategory.Default
                )
                .draggable(
                    state = pull,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    onDragStarted = { swipe.begin() },
                    onDragStopped = { velocity ->
                        when (swipe.landing(velocity, flick)) {
                            RowSwipe.Rest -> Unit
                            RowSwipe.Delete -> latestDelete.value()
                            RowSwipe.Reveal -> latestReveal.value?.let { taken ->
                                taken.perform()
                                if (!taken.answeredInInk) haptics.drop()
                            }
                        }
                        swipe.springHome(settle)
                    }
                )
        ) {
            content()
            reveal?.let { uncovered ->
                CornerMark(
                    painter = if (uncovered.mark == SwipeMark.Check) tick else pencil,
                    contentDescription = uncovered.label,
                    atStart = true,
                    opened = { (pulled() / travel).coerceAtLeast(AT_REST) },
                    onPress = {
                        uncovered.perform()
                        if (!uncovered.answeredInInk) haptics.drop()
                    },
                    animated = animated,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
            CornerMark(
                painter = trash,
                contentDescription = tearLabel,
                atStart = false,
                opened = { (-pulled() / travel).coerceAtLeast(AT_REST) },
                onPress = { latestDelete.value() },
                animated = animated,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
