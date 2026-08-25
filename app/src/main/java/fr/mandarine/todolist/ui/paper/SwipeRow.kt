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
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
class SwipeReveal(val mark: SwipeMark, val perform: () -> Unit) {
    internal val answeredInInk: Boolean get() = mark == SwipeMark.Check
}

private val SWIPE_TRAVEL = 96.dp
private val SWIPE_FLICK = 125.dp
private const val SWIPE_THRESHOLD = 0.5f
private const val SWIPE_RESISTANCE = 0.35f
private const val AT_REST = 0f
private const val FULLY_DRAWN = 1f
private const val MARK_MIN_SCALE = 0.6f
private const val MARK_GROWTH = 0.4f
private val MARK_GLYPH = 24.dp
private val MARK_STROKE = 2.dp
private val CHECK_START = Offset(0.10f, 0.52f)
private val CHECK_KNEE = Offset(0.38f, 0.84f)
private val CHECK_END = Offset(0.92f, 0.12f)
private const val HALF = 0.5f

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

    val offset: Float get() = weightedSwipe(pulled, travel)

    val travelling: Boolean get() = pulled != AT_REST

    val locked: Boolean get() = abs(offset) >= travel * SWIPE_THRESHOLD

    fun drag(delta: Float) {
        val reached = pulled + delta
        pulled = if (reveals) reached else reached.coerceAtMost(AT_REST)
    }

    fun landing(velocity: Float, flick: Float): RowSwipe {
        val committing = if (abs(velocity) >= flick) {
            sign(velocity) == sign(offset)
        } else {
            locked
        }
        return when {
            !committing || offset == AT_REST -> RowSwipe.Rest
            offset < AT_REST -> RowSwipe.Delete
            else -> RowSwipe.Reveal
        }
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

    val swipe = remember(key, revealMark, travel) { RowSwipeState(travel, revealMark != null) }
    val settle = if (animated) PaperMotion.sheetSettle else snap()
    val latestDelete = rememberUpdatedState(onDelete)
    val latestReveal = rememberUpdatedState(reveal)

    val locked by remember(swipe) { derivedStateOf { swipe.locked } }
    val travelling by remember(swipe) { derivedStateOf { swipe.travelling } }

    LaunchedEffect(locked) { if (locked) haptics.pickUp() }

    val pull = rememberDraggableState { delta -> swipe.drag(delta) }

    Box(
        modifier = modifier.drawBehind {
            val travelled = swipe.offset
            val rule = pitch.toPx()
            if (travelled < AT_REST) {
                drawStampedGlyph(
                    glyph = trash,
                    progress = -travelled / travel,
                    centre = size.width - travel * HALF,
                    seat = ruleSeat(rule),
                    foot = GlyphFoot.trash,
                    ink = palette.inked(InkTone.Lost)
                )
            } else if (travelled > AT_REST) {
                val progress = travelled / travel
                val seat = ruleSeat(rule)
                if (revealMark == SwipeMark.Pencil) {
                    drawStampedGlyph(
                        glyph = pencil,
                        progress = progress,
                        centre = travel * HALF,
                        seat = seat,
                        foot = GlyphFoot.pencil,
                        ink = palette.inked(InkTone.Margin)
                    )
                } else {
                    drawCheckMark(progress, travel * HALF, seat, palette.inked(InkTone.Margin))
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipe.offset.roundToInt(), 0) }
                .preferredFrameRate(
                    if (travelling) FrameRateCategory.High else FrameRateCategory.Default
                )
                .draggable(
                    state = pull,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
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
        }
    }
}

private fun DrawScope.ruleSeat(pitch: Float): Float = size.height - pitch * BASELINE_LIFT

private fun DrawScope.drawStampedGlyph(
    glyph: Painter,
    progress: Float,
    centre: Float,
    seat: Float,
    foot: Float,
    ink: Color
) {
    val drawn = progress.coerceIn(AT_REST, FULLY_DRAWN)
    val scaled = MARK_GLYPH.toPx() * (MARK_MIN_SCALE + MARK_GROWTH * drawn)
    translate(centre - scaled * HALF, seat - scaled * foot) {
        with(glyph) {
            draw(Size(scaled, scaled), alpha = drawn, ColorFilter.tint(ink))
        }
    }
}

private fun DrawScope.drawCheckMark(progress: Float, centre: Float, seat: Float, ink: Color) {
    val glyph = MARK_GLYPH.toPx()
    val check = checkPath(glyph)
    val measure = PathMeasure().apply { setPath(check, false) }
    val drawn = Path()
    measure.getSegment(AT_REST, measure.length * progress.coerceIn(AT_REST, FULLY_DRAWN), drawn, true)
    translate(centre - glyph * HALF, seat - glyph * CHECK_KNEE.y) {
        inked(drawn, ink, MARK_STROKE.toPx())
    }
}

private fun checkPath(glyph: Float): Path = Path().apply {
    moveTo(glyph * CHECK_START.x, glyph * CHECK_START.y)
    lineTo(glyph * CHECK_KNEE.x, glyph * CHECK_KNEE.y)
    lineTo(glyph * CHECK_END.x, glyph * CHECK_END.y)
}
