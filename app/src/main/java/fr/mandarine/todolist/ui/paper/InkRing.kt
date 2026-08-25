package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

private const val RING_STEPS = 9
private const val RING_OVERSHOOT = 0.22f
private const val TICK_DRAWN = 1f
private const val TICK_CLEAR = 0f
private const val WASH_FULL = 1f
private const val WASH_GONE = 0f
private const val WET = 0f
private const val DRY = 1f
private const val TWO_PI = 6.2831855f
private const val JITTER_SPAN = 2f
private const val JITTER_CENTRE = 1f
private const val HALF = 0.5f
private const val RING_WOBBLE = 0.045f
private val RING_STROKE = 1.75.dp
private val RING_JITTER = 0.8.dp
private val TICK_STROKE = 2.dp
private val TICK_START = Offset(0.22f, 0.54f)
private val TICK_KNEE = Offset(0.42f, 0.78f)
private val TICK_END = Offset(0.82f, 0.20f)

@Composable
fun InkRing(
    checked: Boolean,
    onToggle: () -> Unit,
    seed: Int,
    contentDescription: String,
    stateDescription: String,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val palette = LocalPaperPalette.current
    val haptics = rememberPaperHaptics()
    val ringSize = with(LocalDensity.current) { style.fontSize.toPx().toDp() }
    val tick = remember { Animatable(if (checked) TICK_DRAWN else TICK_CLEAR) }
    val wash = remember { Animatable(WASH_GONE) }
    val dryness = remember { Animatable(DRY) }

    LaunchedEffect(checked, animated) {
        val target = if (checked) TICK_DRAWN else TICK_CLEAR
        if (tick.value == target) return@LaunchedEffect
        if (!animated) {
            tick.snapTo(target)
            wash.snapTo(WASH_GONE)
            dryness.snapTo(DRY)
        } else if (checked) {
            dryness.snapTo(WET)
            launch { wash.animateTo(WASH_FULL, PaperMotion.rowEnter) }
            tick.animateTo(TICK_DRAWN, PaperMotion.rowEnter)
            launch { wash.animateTo(WASH_GONE, PaperMotion.rowEnter) }
            launch { dryness.animateTo(DRY, PaperMotion.rowEnter) }
        } else {
            dryness.snapTo(DRY)
            launch { wash.animateTo(WASH_FULL, PaperMotion.rowEnter) }
            tick.animateTo(TICK_CLEAR, PaperMotion.rowEnter)
            launch { wash.animateTo(WASH_GONE, PaperMotion.rowEnter) }
        }
        if (checked) haptics.tick() else haptics.untick()
    }

    Box(
        modifier = modifier
            .width(PaperDimens.iconButton)
            .height(LocalPagePitch.current)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = stateDescription
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Spacer(
            modifier = Modifier
                .size(ringSize)
                .drawWithCache {
                    val ring = ringPath(size, seed, RING_JITTER.toPx())
                    val check = tickPath(size)
                    val measure = PathMeasure().apply { setPath(check, false) }
                    val drawnTick = measure.length
                    val drawn = Path()
                    val nib = InkNib(RING_STROKE.toPx())
                    val tip = InkNib(TICK_STROKE.toPx())
                    val ringInk = palette.inked(InkBudget.ring(wet = false))
                    val washInk = palette.inkBluePale
                    val wetInk = palette.inked(InkBudget.ring(wet = true))
                    val dryInk = palette.inked(InkTone.Crossed)
                    onDrawBehind {
                        drawWash(washInk, wash.value)
                        inked(ring, ringInk, nib)
                        val shown = tick.value
                        if (shown <= TICK_CLEAR) return@onDrawBehind
                        drawn.reset()
                        measure.getSegment(TICK_CLEAR, drawnTick * shown, drawn, true)
                        inked(drawn, lerp(wetInk, dryInk, dryness.value), tip)
                    }
                }
        )
    }
}

/**
 * The mark that says "this one": the same hand-drawn ring the completion toggle
 * carries, thrown around whatever it wraps and inked in stroke by stroke as the
 * choice is made.
 */
@Composable
fun Modifier.circledInInk(
    circled: Boolean,
    seed: Int,
    color: Color,
    animated: Boolean = true
): Modifier {
    val drawn = remember { Animatable(if (circled) TICK_DRAWN else TICK_CLEAR) }
    LaunchedEffect(circled, animated) {
        val target = if (circled) TICK_DRAWN else TICK_CLEAR
        if (drawn.value == target) return@LaunchedEffect
        if (animated) drawn.animateTo(target, PaperMotion.rowEnter) else drawn.snapTo(target)
    }
    return drawWithCache {
        val ring = ringPath(size, seed, size.minDimension * RING_WOBBLE)
        val measure = PathMeasure().apply { setPath(ring, false) }
        val ringLength = measure.length
        val nib = InkNib(RING_STROKE.toPx())
        val segment = Path()
        onDrawWithContent {
            drawContent()
            val shown = drawn.value
            if (shown <= TICK_CLEAR) return@onDrawWithContent
            segment.reset()
            measure.getSegment(TICK_CLEAR, ringLength * shown, segment, true)
            inked(segment, color, nib)
        }
    }
}

private fun DrawScope.drawWash(color: Color, wash: Float) {
    if (wash <= WASH_GONE) return
    drawCircle(color = color, radius = size.minDimension * HALF * wash)
}

private fun ringPath(size: Size, seed: Int, jitter: Float): Path {
    val random = Random(seed)
    val radius = size.minDimension * HALF - jitter
    val centre = Offset(size.width * HALF, size.height * HALF)
    val points = List(RING_STEPS) { step ->
        val angle = TWO_PI * step / RING_STEPS
        val reach = radius + (random.nextFloat() * JITTER_SPAN - JITTER_CENTRE) * jitter
        Offset(centre.x + cos(angle) * reach, centre.y + sin(angle) * reach)
    }
    val path = Path()
    val start = points.last().midpointTo(points.first())
    path.moveTo(start.x, start.y)
    points.forEachIndexed { index, point ->
        val next = point.midpointTo(points[(index + 1) % RING_STEPS])
        path.quadraticTo(point.x, point.y, next.x, next.y)
    }
    val past = start.midpointTo(points.first().midpointTo(points[1]), RING_OVERSHOOT)
    path.quadraticTo(points.first().x, points.first().y, past.x, past.y)
    return path
}

private fun tickPath(size: Size): Path = Path().apply {
    moveTo(size.width * TICK_START.x, size.height * TICK_START.y)
    lineTo(size.width * TICK_KNEE.x, size.height * TICK_KNEE.y)
    lineTo(size.width * TICK_END.x, size.height * TICK_END.y)
}

private fun Offset.midpointTo(other: Offset, fraction: Float = HALF): Offset =
    Offset(x + (other.x - x) * fraction, y + (other.y - y) * fraction)
