package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.drop

enum class RowSwipe { Delete, Rest, Reveal }

enum class SwipeMark { Check, Pencil }

/**
 * What a row uncovers when it is dragged start to end. A row that can be finished
 * uncovers a check; a row that has nothing to finish uncovers the pencil that
 * edits it, so the same gesture always means "the other thing this row does".
 */
@Immutable
class SwipeReveal(val mark: SwipeMark, val perform: () -> Unit)

private val SWIPE_TRAVEL = 96.dp
private const val SWIPE_THRESHOLD = 0.5f
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
    val view = LocalView.current
    val density = LocalDensity.current
    val pitch = LocalPagePitch.current
    val travel = with(density) { SWIPE_TRAVEL.toPx() }
    val revealMark = reveal?.mark
    val trash = painterResource(R.drawable.ic_delete)
    val pencil = painterResource(R.drawable.ic_edit)

    val swipe = remember(key, revealMark, travel) {
        AnchoredDraggableState(
            initialValue = RowSwipe.Rest,
            anchors = DraggableAnchors {
                RowSwipe.Delete at -travel
                RowSwipe.Rest at AT_REST
                if (revealMark != null) RowSwipe.Reveal at travel
            }
        )
    }
    val settle = if (animated) PaperMotion.swipeSettle else PaperMotion.instant
    val fling = AnchoredDraggableDefaults.flingBehavior(
        state = swipe,
        positionalThreshold = { distance -> distance * SWIPE_THRESHOLD },
        animationSpec = settle
    )

    val latestDelete = rememberUpdatedState(onDelete)
    val latestReveal = rememberUpdatedState(reveal)

    LaunchedEffect(swipe) {
        snapshotFlow { swipe.targetValue }
            .drop(1)
            .collect { target -> if (target != RowSwipe.Rest) view.performPickUpFeedback() }
    }

    LaunchedEffect(swipe) {
        snapshotFlow { swipe.settledValue }.collect { settled ->
            when (settled) {
                RowSwipe.Rest -> return@collect
                RowSwipe.Delete -> latestDelete.value()
                RowSwipe.Reveal -> latestReveal.value?.perform?.invoke()
            }
            swipe.animateTo(RowSwipe.Rest, settle)
        }
    }

    val travelling by remember(swipe) {
        derivedStateOf { abs(swipe.currentOffset()) > AT_REST }
    }

    Box(
        modifier = modifier.drawBehind {
            val travelled = swipe.currentOffset()
            val rule = pitch.toPx()
            if (travelled < AT_REST) {
                drawStampedGlyph(
                    glyph = trash,
                    progress = -travelled / travel,
                    centre = size.width - travel * HALF,
                    seat = ruleSeat(rule),
                    foot = GlyphFoot.trash,
                    ink = palette.inkDanger
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
                        ink = palette.pencil
                    )
                } else {
                    drawCheckMark(progress, travel * HALF, seat, palette.inkMargin)
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipe.currentOffset().roundToInt(), 0) }
                .preferredFrameRate(
                    if (travelling) FrameRateCategory.High else FrameRateCategory.Default
                )
                .anchoredDraggable(
                    state = swipe,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    flingBehavior = fling
                )
        ) {
            content()
        }
    }
}

private fun AnchoredDraggableState<RowSwipe>.currentOffset(): Float =
    offset.let { if (it.isNaN()) AT_REST else it }

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
        drawPath(drawn, ink, style = Stroke(width = MARK_STROKE.toPx(), cap = StrokeCap.Round))
    }
}

private fun checkPath(glyph: Float): Path = Path().apply {
    moveTo(glyph * CHECK_START.x, glyph * CHECK_START.y)
    lineTo(glyph * CHECK_KNEE.x, glyph * CHECK_KNEE.y)
    lineTo(glyph * CHECK_END.x, glyph * CHECK_END.y)
}
