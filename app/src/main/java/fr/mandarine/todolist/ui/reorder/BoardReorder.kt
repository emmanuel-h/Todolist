package fr.mandarine.todolist.ui.reorder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.rememberPaperHaptics
import kotlinx.coroutines.launch

private const val LIFT_SCALE = 0.05f
private const val FLAT = 0f
private const val RAISED = 1f

/**
 * Which card was last swapped with the one in hand, and whether the finger has been
 * off it since. Cards on a board are not one height, so swapping a tall card with a
 * short one can leave the finger over the short one again in its new place — and
 * swapping straight back would make the pair flicker for as long as the finger
 * stayed there. A card just passed is passed for good until the finger leaves it.
 */
data class SwapGuard(val lastSwapped: String? = null, val leftIt: Boolean = true)

data class BoardStep(val order: List<String>, val guard: SwapGuard)

/**
 * Where the card in hand belongs now that the finger is over [hovered]: in that
 * card's slot, with everything between shuffled one place towards where it came
 * from — unless the finger is over nothing, over itself, over a card it may not be
 * dropped among, or still over the card it just passed.
 */
fun nextBoardOrder(order: List<String>, dragged: String, hovered: String?, guard: SwapGuard): BoardStep {
    if (hovered == null || hovered == dragged || hovered !in order || dragged !in order) {
        return BoardStep(order, guard.copy(leftIt = true))
    }
    if (hovered == guard.lastSwapped && !guard.leftIt) return BoardStep(order, guard)
    return BoardStep(order.moved(order.indexOf(dragged), order.indexOf(hovered)), SwapGuard(hovered, leftIt = false))
}

/** The card whose bounds hold [point], if any. */
fun cardUnder(point: Offset, cards: Map<String, Rect>): String? =
    cards.entries.firstOrNull { it.value.contains(point) }?.key

/**
 * One card carried across the board. Unlike a row, a card moves in two directions
 * and its neighbours are not one height, so the drag is not counted in rows: the
 * finger is followed in window coordinates, and whichever card it is over is where
 * the one in hand goes. Every card reports where it lies as it is placed, and the
 * card in hand is drawn at the finger, however the board re-flows beneath it.
 */
class BoardDrag(private val onOrderChanged: (List<String>) -> Unit) {

    var draggedId by mutableStateOf<String?>(null)
        private set

    private var pointer by mutableStateOf(Offset.Zero)
    private var restingAt by mutableStateOf(Offset.Zero)
    private var grab = Offset.Zero
    private var settling = false
    private var ids: List<String> = emptyList()
    private var startOrder: List<String> = emptyList()
    private var guard = SwapGuard()
    private val cards = HashMap<String, Rect>()

    var viewport: Rect = Rect.Zero

    val dragging: Boolean get() = draggedId != null

    /** How far the card in hand is drawn from the slot the board has given it. */
    val offset: Offset get() = if (dragging) pointer - grab - restingAt else Offset.Zero

    val carried: Rect?
        get() {
            val id = draggedId ?: return null
            val size = cards[id]?.size ?: return null
            return Rect(pointer - grab, size)
        }

    fun place(id: String, bounds: Rect) {
        cards[id] = bounds
        if (id == draggedId) restingAt = bounds.topLeft
    }

    fun forget(id: String) {
        if (id != draggedId) cards.remove(id)
    }

    /**
     * Takes the card, or refuses it because another is already in hand. Every card
     * has its own gesture detector, so a second finger on another card would
     * otherwise rewrite the order under the first.
     */
    fun start(id: String, order: List<String>, touch: Offset): Boolean {
        if (dragging) return false
        val bounds = cards[id] ?: return false
        if (id !in order) return false
        ids = order
        startOrder = order
        guard = SwapGuard()
        grab = touch
        restingAt = bounds.topLeft
        pointer = bounds.topLeft + touch
        draggedId = id
        return true
    }

    fun drag(delta: Offset): Boolean {
        if (!dragging || settling) return false
        pointer += delta
        return follow()
    }

    /** Re-reads the board under the finger; true when the card changed places. */
    fun follow(): Boolean {
        val dragged = draggedId ?: return false
        if (settling) return false
        val others = cards.filterKeys { it != dragged && it in ids }
        val step = nextBoardOrder(ids, dragged, cardUnder(pointer, others), guard)
        guard = step.guard
        if (step.order == ids) return false
        ids = step.order
        onOrderChanged(ids)
        return true
    }

    /**
     * The card is already in its new slot when the finger lifts; the drop only
     * glides it down into it. The card stays in hand for the glide, so a fresh
     * long-press during it is refused.
     */
    suspend fun settle(spec: AnimationSpec<Offset>): List<String>? {
        if (!dragging) return null
        val order = ids
        val moved = order != startOrder
        settling = true
        try {
            Animatable(pointer, Offset.VectorConverter).animateTo(restingAt + grab, spec) { pointer = value }
        } finally {
            cancel()
        }
        return order.takeIf { moved }
    }

    fun cancel() {
        settling = false
        draggedId = null
        ids = emptyList()
        guard = SwapGuard()
        pointer = Offset.Zero
        restingAt = Offset.Zero
        grab = Offset.Zero
    }

}

/**
 * A card on the board: it reports where it lies, a held press picks it up, and while
 * it is in hand it is drawn at the finger, a little larger and above its neighbours.
 */
@Composable
fun Modifier.boardCard(
    drag: BoardDrag,
    id: String,
    ids: List<String>,
    animated: Boolean,
    onDrop: (List<String>?) -> Unit
): Modifier {
    val haptics = rememberPaperHaptics()
    val scope = rememberCoroutineScope()
    val latestIds = rememberUpdatedState(ids)
    val latestDrop = rememberUpdatedState(onDrop)
    val lifted = drag.draggedId == id
    val grip = remember { Animatable(FLAT) }
    LaunchedEffect(lifted, animated) {
        val target = if (lifted) RAISED else FLAT
        if (animated) grip.animateTo(target, PaperMotion.pickUp) else grip.snapTo(target)
    }
    DisposableEffect(drag, id) { onDispose { drag.forget(id) } }
    return this
        .onPlaced { drag.place(id, Rect(it.positionInRoot(), it.size.toSize())) }
        .pointerInput(id) {
            var holds = false
            detectDragGesturesAfterLongPress(
                onDragStart = { touch ->
                    val order = latestIds.value
                    holds = drag.start(id, order, touch)
                    if (holds) haptics.pickUp()
                },
                onDrag = { change, amount ->
                    if (holds) {
                        change.consume()
                        if (drag.drag(amount)) haptics.pass()
                    }
                },
                onDragEnd = {
                    if (holds) {
                        holds = false
                        haptics.drop()
                        scope.launch {
                            val order = drag.settle(PaperMotion.cardSettle)
                            if (order != null) haptics.submit()
                            latestDrop.value(order)
                        }
                    }
                },
                onDragCancel = {
                    if (holds) {
                        holds = false
                        scope.launch {
                            drag.settle(PaperMotion.cardSettle)
                            latestDrop.value(null)
                        }
                    }
                }
            )
        }
        .zIndex(if (lifted) RAISED else FLAT)
        .preferredFrameRate(if (lifted) FrameRateCategory.High else FrameRateCategory.Default)
        .graphicsLayer {
            if (lifted) {
                val carried = drag.offset
                translationX = carried.x
                translationY = carried.y
            }
            val held = grip.value
            scaleX = RAISED + LIFT_SCALE * held
            scaleY = scaleX
        }
}

/**
 * Near the top or the foot of the board the board scrolls under a held card, so a
 * card can be carried further than one screen. The board is re-read every frame of
 * the drag, because scrolling moves the cards under a finger that has not moved.
 */
@Composable
fun ScrollBoardWhileDragging(
    gridState: LazyStaggeredGridState,
    drag: BoardDrag,
    edgeScroll: EdgeScroll
) {
    LaunchedEffect(drag.dragging) {
        while (drag.dragging) {
            withFrameNanos { }
            val card = drag.carried ?: continue
            val viewport = drag.viewport
            val delta = autoScrollDelta(
                rowTop = card.top - viewport.top,
                rowBottom = card.bottom - viewport.top,
                viewportHeight = viewport.height,
                edge = edgeScroll.edge,
                maxStep = edgeScroll.maxStep,
                canScrollUp = gridState.canScrollBackward,
                canScrollDown = gridState.canScrollForward
            )
            if (delta != 0f) gridState.scrollBy(delta)
            drag.follow()
        }
    }
}
