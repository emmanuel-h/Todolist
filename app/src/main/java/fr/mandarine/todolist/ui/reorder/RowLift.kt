package fr.mandarine.todolist.ui.reorder

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.paperSheetFading
import fr.mandarine.todolist.ui.paper.rememberPaperHaptics
import kotlinx.coroutines.launch

private const val LIFT_HOLD_MILLIS = 400L
private val AUTO_SCROLL_EDGE = 72.dp
private val AUTO_SCROLL_MAX_STEP = 12.dp
private val LIFT_SHADOW_RADIUS = 14.dp
private val LIFT_SHADOW_DROP = 6.dp
private const val LIFT_SHADOW_ALPHA = 0.22f
private const val LIFT_SCALE = 0.02f
private const val LIFT_TILT = 0.6f
private const val FLAT = 0f
private const val RAISED = 1f

@Immutable
class EdgeScroll(val edge: Float, val maxStep: Float)

@Composable
fun rememberEdgeScroll(
    edge: Dp = AUTO_SCROLL_EDGE,
    maxStep: Dp = AUTO_SCROLL_MAX_STEP
): EdgeScroll {
    val density = LocalDensity.current
    return remember(density, edge, maxStep) {
        EdgeScroll(
            edge = with(density) { edge.toPx() },
            maxStep = with(density) { maxStep.toPx() }
        )
    }
}

/**
 * Holding a row is what lifts it, and the platform's own half second is long
 * enough that the page reads as ignoring the finger. The wait belongs to the
 * gesture detector rather than to any one row, so the page hands it down to every
 * row it holds.
 */
@Composable
fun LiftHold(content: @Composable () -> Unit) {
    val platform = LocalViewConfiguration.current
    val holding = remember(platform) { LiftHoldConfiguration(platform) }
    CompositionLocalProvider(LocalViewConfiguration provides holding, content = content)
}

private class LiftHoldConfiguration(platform: ViewConfiguration) : ViewConfiguration by platform {
    override val longPressTimeoutMillis: Long = LIFT_HOLD_MILLIS
}

/**
 * A press held on the row lifts it; the row list is read through
 * [rememberUpdatedState] because the staged order changes under the finger while
 * the same gesture is still running.
 */
@Composable
fun Modifier.liftToReorder(
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll,
    id: String,
    ids: List<String>,
    onDrop: (Reorder?) -> Unit
): Modifier {
    val haptics = rememberPaperHaptics()
    val scope = rememberCoroutineScope()
    val latestIds = rememberUpdatedState(ids)
    val latestDrop = rememberUpdatedState(onDrop)
    return pointerInput(id) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                val current = latestIds.value
                session.start(
                    from = current.indexOf(id),
                    rowIds = current,
                    rowHeights = rowHeights(listState, current)
                )
                haptics.pickUp()
            },
            onDrag = { change, amount ->
                change.consume()
                val before = session.index
                session.drag(amount.y)
                if (session.index != before) haptics.pass()
                session.edgeScrolling = edgeScrollDelta(listState, session, edgeScroll) != 0f
            },
            onDragEnd = {
                haptics.drop()
                scope.launch {
                    val reorder = session.settle(PaperMotion.sheetSettle)
                    if (reorder != null) haptics.submit()
                    latestDrop.value(reorder)
                }
            },
            onDragCancel = {
                scope.launch {
                    session.settle(PaperMotion.sheetSettle)
                    latestDrop.value(null)
                }
            }
        )
    }
}

/**
 * The lifted row is a slip of paper in its own right: it carries its grain and a
 * warm shadow with it, and both are read at draw time so a drag never spends a
 * recomposition on them. The shadow spreads on a slacker spring than the grip
 * that darkens it, so the slip is caught in the hand a moment before the page
 * beneath it goes soft — which is what reads as the paper coming free.
 */
@Composable
fun Modifier.liftedSlip(session: DragSession, lifted: Boolean, animated: Boolean): Modifier {
    val palette = LocalPaperPalette.current
    val spread = remember { Animatable(FLAT) }
    val grip = remember { Animatable(FLAT) }
    val carried = rememberUpdatedState(lifted)
    LaunchedEffect(lifted, animated) {
        val target = if (lifted) RAISED else FLAT
        if (!animated) {
            spread.snapTo(target)
            grip.snapTo(target)
        } else {
            launch { spread.animateTo(target, PaperMotion.slipShadow) }
            grip.animateTo(target, PaperMotion.slipGrip)
        }
    }
    return this
        .zIndex(if (lifted) RAISED else FLAT)
        .preferredFrameRate(if (lifted) FrameRateCategory.High else FrameRateCategory.Default)
        .graphicsLayer {
            if (carried.value) translationY = session.offset
            val held = grip.value
            if (held <= FLAT) return@graphicsLayer
            scaleX = RAISED + LIFT_SCALE * held
            scaleY = scaleX
            rotationZ = LIFT_TILT * held * session.direction
        }
        .dropShadow(RectangleShape) {
            radius = LIFT_SHADOW_RADIUS.toPx() * spread.value
            alpha = LIFT_SHADOW_ALPHA * grip.value
            color = palette.shadow
            offset = Offset(FLAT, LIFT_SHADOW_DROP.toPx() * spread.value)
        }
        .paperSheetFading({ grip.value }, tone = palette.paperSheet)
}

/**
 * A finger held still inside the edge band must keep the list moving, so the
 * scroll is driven by frames rather than by drag events. It only spins while the
 * band is occupied — running it for the whole drag would keep the frame clock
 * busy for as long as a finger is down. Whatever it scrolls is fed back into the
 * session so the row stays under the finger and keeps swapping rows.
 */
@Composable
fun AutoScrollWhileDragging(
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll
) {
    LaunchedEffect(session.edgeScrolling) {
        while (session.edgeScrolling && session.dragging) {
            withFrameNanos { }
            val delta = edgeScrollDelta(listState, session, edgeScroll)
            if (delta == 0f) {
                session.edgeScrolling = false
            } else {
                listState.scrollBy(delta)
                session.drag(delta)
            }
        }
    }
}

private fun edgeScrollDelta(
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll
): Float {
    val info = listState.layoutInfo
    val dragged = info.visibleItemsInfo.firstOrNull { it.index == session.index } ?: return 0f
    val top = dragged.offset - info.viewportStartOffset + session.offset
    return autoScrollDelta(
        rowTop = top,
        rowBottom = top + dragged.size,
        viewportHeight = (info.viewportEndOffset - info.viewportStartOffset).toFloat(),
        edge = edgeScroll.edge,
        maxStep = edgeScroll.maxStep,
        canScrollUp = listState.canScrollBackward,
        canScrollDown = listState.canScrollForward
    )
}

private fun rowHeights(listState: LazyListState, ids: List<String>): List<Int> {
    val sizes = listState.layoutInfo.visibleItemsInfo.associate { it.key to it.size }
    val fallback = sizes.values.firstOrNull() ?: 0
    return ids.map { sizes[it] ?: fallback }
}
