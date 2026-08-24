package fr.mandarine.todolist.ui.reorder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.sign

data class DragPosition(val index: Int, val offset: Float)

data class Reorder(val from: Int, val to: Int)

const val NO_DRAG_INDEX = -1

/**
 * Owns a single handle drag. Rows are addressed by their index within the
 * reorderable section only, so the section below it is unreachable by
 * construction rather than by a bounds check on every move.
 */
class DragSession(private val onOrderChanged: (List<String>) -> Unit) {

    var index by mutableIntStateOf(NO_DRAG_INDEX)
        private set

    var offset by mutableFloatStateOf(0f)
        private set

    var direction by mutableFloatStateOf(0f)
        private set

    var edgeScrolling by mutableStateOf(false)

    private var startIndex = NO_DRAG_INDEX
    private var ids: List<String> = emptyList()
    private var heights: List<Int> = emptyList()

    val dragging: Boolean get() = index != NO_DRAG_INDEX

    fun start(from: Int, rowIds: List<String>, rowHeights: List<Int>) {
        require(rowIds.size == rowHeights.size) {
            "${rowIds.size} ids against ${rowHeights.size} heights"
        }
        require(from in rowIds.indices) { "drag start $from outside ${rowIds.size} rows" }
        startIndex = from
        index = from
        offset = 0f
        direction = 0f
        edgeScrolling = false
        ids = rowIds
        heights = rowHeights
    }

    fun drag(delta: Float) {
        if (!dragging) return
        if (delta != 0f) direction = sign(delta)
        val settled = settleDrag(index, offset + delta, heights)
        if (settled.index != index) {
            ids = ids.moved(index, settled.index)
            heights = heights.moved(index, settled.index)
            index = settled.index
            onOrderChanged(ids)
        }
        offset = settled.offset
    }

    fun heightOf(position: Int): Int = heights.getOrElse(position) { 0 }

    /**
     * The row is already sitting in its new slot by the time the finger lifts, so
     * the drop only has to glide the residual offset away. The session stays open
     * for the whole glide — releasing it first would drop the row into place in a
     * single frame, which is the jump the handle used to make.
     */
    suspend fun settle(spec: AnimationSpec<Float>): Reorder? {
        if (!dragging) return null
        edgeScrolling = false
        var landed = false
        try {
            Animatable(offset).animateTo(0f, spec) { offset = value }
            landed = true
        } finally {
            if (!landed) cancel()
        }
        return end()
    }

    fun end(): Reorder? {
        val from = startIndex
        val to = index
        cancel()
        return if (from != to && from != NO_DRAG_INDEX) Reorder(from, to) else null
    }

    fun cancel() {
        startIndex = NO_DRAG_INDEX
        index = NO_DRAG_INDEX
        offset = 0f
        direction = 0f
        edgeScrolling = false
        ids = emptyList()
        heights = emptyList()
    }
}

/**
 * Once a step is taken the search keeps going the same way. A drag that lands on
 * exactly half a row otherwise meets the swap threshold in both directions and
 * ping-pongs between the two rows forever.
 */
fun settleDrag(index: Int, offset: Float, rowHeights: List<Int>): DragPosition {
    require(index in rowHeights.indices) { "drag index $index outside ${rowHeights.size} rows" }
    val heights = rowHeights.toMutableList()
    var current = index
    var remaining = offset
    var direction = 0
    while (true) {
        val next = current + 1
        val previous = current - 1
        val stepsDown = direction >= 0 && remaining > 0f &&
            next <= heights.lastIndex && remaining >= heights[next] / 2f
        val stepsUp = direction <= 0 && remaining < 0f &&
            previous >= 0 && -remaining >= heights[previous] / 2f
        if (stepsDown) {
            remaining -= heights[next]
            heights.add(current, heights.removeAt(next))
            current = next
            direction = 1
        } else if (stepsUp) {
            remaining += heights[previous]
            heights.add(current, heights.removeAt(previous))
            current = previous
            direction = -1
        } else {
            return DragPosition(current, remaining)
        }
    }
}

fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    require(from in indices) { "from index $from outside $size items" }
    require(to in indices) { "to index $to outside $size items" }
    val result = toMutableList()
    result.add(to, result.removeAt(from))
    return result
}

/**
 * A staged order outlives the repository read that follows a drag, so it is
 * discarded rather than trusted whenever it no longer names exactly the rows on
 * screen — otherwise a row deleted mid-drag would vanish from the section.
 */
fun <T> orderedBy(items: List<T>, order: List<String>?, idOf: (T) -> String): List<T> {
    if (order == null) return items
    val byId = items.associateBy(idOf)
    val ordered = order.mapNotNull { byId[it] }
    return if (ordered.size == items.size) ordered else items
}

/**
 * A row resting at the top of the list is inside the top edge band by definition,
 * so the band alone cannot decide this — without the scroll guards, picking up the
 * first row scrolls the list at it forever and carries the row off the screen.
 */
fun autoScrollDelta(
    rowTop: Float,
    rowBottom: Float,
    viewportHeight: Float,
    edge: Float,
    maxStep: Float,
    canScrollUp: Boolean,
    canScrollDown: Boolean
): Float {
    if (edge <= 0f) return 0f
    if (canScrollUp && rowTop < edge) {
        return -maxStep * ((edge - rowTop) / edge).coerceAtMost(1f)
    }
    val bottomEdge = viewportHeight - edge
    if (canScrollDown && rowBottom > bottomEdge) {
        return maxStep * ((rowBottom - bottomEdge) / edge).coerceAtMost(1f)
    }
    return 0f
}
