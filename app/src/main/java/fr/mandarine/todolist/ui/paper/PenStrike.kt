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

@Stable
class PenStrikeState internal constructor(internal val seed: Int, struck: Boolean) {

    internal var layout by mutableStateOf<TextLayoutResult?>(null)

    internal val progress = Animatable(if (struck) STRIKE_DONE else STRIKE_CLEAR)

    fun onTextLayout(result: TextLayoutResult) {
        layout = result
    }
}

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
