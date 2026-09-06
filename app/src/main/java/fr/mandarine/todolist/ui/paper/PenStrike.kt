package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.text.TextLayoutResult
import kotlin.random.Random
import kotlinx.coroutines.delay

private const val NO_DELAY = 0L
private const val NO_NIB = 0f
private const val STRIKE_CLEAR = 0f
private const val STRIKE_DONE = 1f
private const val LEAD_IN = 0.3f
private const val LEAD_OUT = 0.4f
private const val START_RISE = 0.23f
private const val END_RISE = 0.29f
private const val JITTER = 0.08f
private const val NIB_WIDTH = 0.07f
private const val THIRDS = 3f
private const val TWO_THIRDS = 2f
private const val JITTER_SPAN = 2f
private const val JITTER_CENTRE = 1f

/**
 * The strike-through on a completed item, drawn as a pen stroke rather than as a
 * line.
 *
 * A `TextDecoration.LineThrough` would be a straight rule at a fixed height and
 * would appear instantly. This draws a slightly curved path along the measured
 * baseline, jittered by a seed derived from the item's id — so every item's strike
 * is a little different but the *same* item's strike never changes between frames —
 * and reveals it progressively, so the stroke is seen being written.
 *
 * ### How the reveal works
 * [PenStrikeState.progress] is an `Animatable` running 0..1. On each frame the draw
 * modifier takes `progress × totalLength` of path and walks the strokes handing out
 * that budget, using `PathMeasure.getSegment` to cut a partial path. Multi-line text
 * therefore strikes line by line rather than all at once.
 *
 * Everything is scaled by `em` — the font size in pixels — so the curve, the
 * overshoot at each end and the nib width all hold their proportions at any text
 * size, including the reader's system font scaling.
 */
@Stable
class PenStrikeState internal constructor(internal val seed: Int, struck: Boolean) {

    internal var layout by mutableStateOf<TextLayoutResult?>(null)

    internal val progress = Animatable(if (struck) STRIKE_DONE else STRIKE_CLEAR)

    fun onTextLayout(result: TextLayoutResult) {
        layout = result
    }
}

/**
 * Holds one row's strike across recompositions.
 *
 * `remember(id)` keys the state on the item, so a `LazyColumn` recycling this slot
 * for a different item gets a fresh, correctly-seeded state instead of inheriting
 * the previous item's half-drawn stroke.
 *
 * The `LaunchedEffect` restarts whenever any of its keys change and is cancelled
 * when this leaves the composition. The early return matters: without it, a
 * recomposition for an unrelated reason would re-run an animation that has already
 * finished. When animations are off the value is snapped rather than animated, and
 * [delayMillis] lets the strike start after the ring has finished being inked, so
 * the two marks read as one gesture rather than as two at once.
 */
@Composable
fun rememberPenStrike(
    id: String,
    struck: Boolean,
    animated: Boolean = true,
    delayMillis: Long = NO_DELAY
): PenStrikeState {
    val state = remember(id) { PenStrikeState(id.hashCode(), struck) }
    LaunchedEffect(state, struck, animated, delayMillis) {
        val target = if (struck) STRIKE_DONE else STRIKE_CLEAR
        if (state.progress.value == target) return@LaunchedEffect
        if (animated) {
            delay(delayMillis)
            state.progress.animateTo(target, PaperMotion.rowEnter)
        } else {
            state.progress.snapTo(target)
        }
    }
    return state
}

/**
 * Draws the strike over whatever the node it is applied to draws.
 *
 * `drawWithCache` has two halves, and the split is the point: the outer block runs
 * only when the size or the captured values change, and is where the paths are
 * measured; `onDrawWithContent` runs every frame. Building the paths in the outer
 * block means an animating strike is not re-measuring text sixty times a second.
 *
 * `drawContent()` draws the text itself; anything after it lands on top.
 */
fun Modifier.penStrike(state: PenStrikeState, color: Color): Modifier = drawWithCache {
    val layout = state.layout
    val strokes = if (layout == null) emptyList() else strikeStrokes(layout, state.seed)
    val inked = strokes.sumOf { it.length.toDouble() }.toFloat()
    val nib = InkNib(if (layout == null) NO_NIB else NIB_WIDTH * layout.emPixels(this))
    onDrawWithContent {
        drawContent()
        val revealed = inked * state.progress.value
        if (revealed <= STRIKE_CLEAR) return@onDrawWithContent
        var remaining = revealed
        for (stroke in strokes) {
            if (remaining <= STRIKE_CLEAR) break
            val taken = minOf(remaining, stroke.length)
            stroke.drawn.reset()
            stroke.measure.getSegment(STRIKE_CLEAR, taken, stroke.drawn, true)
            inked(stroke.drawn, color, nib)
            remaining -= taken
        }
    }
}

private class PenStroke(val measure: PathMeasure, val length: Float, val drawn: Path)

private fun TextLayoutResult.emPixels(scope: CacheDrawScope): Float =
    with(scope) { layoutInput.style.fontSize.toPx() }

/**
 * One curved path per line of text, from just before the first glyph to just past
 * the last.
 *
 * `Random(seed)` is seeded from the item id, so the wobble is stable for an item
 * across every redraw and every process — a strike that re-rolled its jitter each
 * frame would shimmer. `cubicTo` with two control points at roughly the thirds of
 * the run gives a stroke that sags and lifts the way a hand does.
 */
private fun CacheDrawScope.strikeStrokes(
    layout: TextLayoutResult,
    seed: Int
): List<PenStroke> {
    val em = layout.emPixels(this)
    val random = Random(seed)
    return (0 until layout.lineCount).map { line ->
        val baseline = layout.getLineBaseline(line)
        val startX = layout.getLineLeft(line) - LEAD_IN * em
        val endX = layout.getLineRight(line) + LEAD_OUT * em
        val startY = baseline - START_RISE * em + random.nudge(em)
        val endY = baseline - END_RISE * em + random.nudge(em)
        val run = endX - startX
        val path = Path().apply {
            moveTo(startX, startY)
            cubicTo(
                startX + run / THIRDS,
                startY + random.nudge(em),
                startX + TWO_THIRDS * run / THIRDS,
                endY + random.nudge(em),
                endX,
                endY
            )
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        PenStroke(measure, measure.length, Path())
    }
}

private fun Random.nudge(em: Float): Float =
    (nextFloat() * JITTER_SPAN - JITTER_CENTRE) * JITTER * em
