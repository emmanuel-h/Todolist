package fr.mandarine.todolist.ui.reorder

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val AUTO_SCROLL_EDGE = 72.dp
private val AUTO_SCROLL_MAX_STEP = 12.dp

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
 * Drives one row by its handle. The row list is read through
 * [rememberUpdatedState] because the staged order changes under the finger while
 * the same gesture is still running.
 */
@Composable
fun Modifier.reorderHandle(
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll,
    id: String,
    ids: List<String>,
    onDrop: (Reorder?) -> Unit
): Modifier {
    val view = LocalView.current
    val latestIds = rememberUpdatedState(ids)
    return pointerInput(id) {
        detectDragGestures(
            onDragStart = {
                val current = latestIds.value
                session.start(
                    from = current.indexOf(id),
                    rowIds = current,
                    rowHeights = rowHeights(listState, current)
                )
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            },
            onDrag = { change, amount ->
                change.consume()
                session.drag(amount.y)
                session.edgeScrolling = edgeScrollDelta(listState, session, edgeScroll) != 0f
            },
            onDragEnd = {
                val reorder = session.end()
                if (reorder != null) view.performDropFeedback()
                onDrop(reorder)
            },
            onDragCancel = {
                session.cancel()
                onDrop(null)
            }
        )
    }
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

private fun View.performDropFeedback() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}
