package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val TICK_MILLIS = 380
private const val TICK_HOLD_MILLIS = 420
private const val TICK_FADE_MILLIS = 400
private const val SCRAP_MILLIS = 1400
private const val SETTLE_MILLIS = 380

private val TICK_NIB = 11.dp
private const val TICK_INK = 0.75f

/**
 * The mark, on the page's own proportions rather than on a fixed size: a tick
 * drawn over a phone and a tick drawn over a tablet should both look like one
 * stroke of the same pen held over the whole sheet.
 */
private const val TICK_FROM_X = 0.28f
private const val TICK_FROM_Y = 0.46f
private const val TICK_VERTEX_X = 0.44f
private const val TICK_VERTEX_Y = 0.64f
private const val TICK_TO_X = 0.75f
private const val TICK_TO_Y = 0.28f

private const val SCRAP_COUNT = 46
private const val SHARD_IN = 4
private val SCRAP_SIZE_MIN = 7.dp
private val SCRAP_SIZE_MAX = 15.dp

/**
 * Up and outward, in a wide cone about straight up, then gravity has it back.
 *
 * Thrown hard the shower is over before it is seen: a tick is often written near
 * the head of the page, and scraps that leave at the speed of a party popper are
 * off the top edge in three frames. These are thrown about as hard as a handful of
 * paper is thrown, so most of them arc inside the sheet and come back down it.
 */
private const val THROW_MIN = 520f
private const val THROW_MAX = 1250f
private const val THROW_SPREAD = 1.25f
private const val GRAVITY = 2200f
private const val DRAG = 0.4f
private const val SPIN_MIN = 4f
private const val SPIN_MAX = 13f
private const val SCRAP_FADE_FROM = 0.72f

private const val SETTLE_DIP = 0.012f
private const val PAGE_REST = 1f
private const val NONE = 0f
private const val WHOLE = 1f

/**
 * What the page does when the last thing on it is ticked off: a tick the size of
 * the sheet, a shower of punched-out paper and torn scraps thrown up from where
 * the reader's finger was, and the page lifting and settling under it the way a
 * sheet does when it is closed and set down.
 *
 * It is a flourish and nothing more — the list is already finished by the time it
 * runs, the name is already struck through, and a reader who has asked for
 * stillness gets the strike and the buzz and none of this.
 */
class FinishFlourish {

    var burst by mutableStateOf<Offset?>(null)
        private set

    private var scraps: List<Scrap> = emptyList()

    val mark = Animatable(NONE)
    val markInk = Animatable(NONE)
    val shower = Animatable(NONE)
    val settle = Animatable(PAGE_REST)

    val running: Boolean get() = burst != null

    /**
     * Deterministic from the item the tick was written on: the same finished list
     * throws the same confetti twice, which is what makes this testable and what
     * keeps a recomposition from reshuffling the sky mid-flight.
     */
    /**
     * One event, not three in a queue. The mark, the shower and the sheet settling
     * under them all start together and the whole flourish is over inside a second
     * and a half — played in sequence it ran for three, and a tick that sits on the
     * page waiting for its confetti stops reading as a flourish and starts reading
     * as something the reader is supposed to dismiss.
     */
    suspend fun play(at: Offset, seed: String) = coroutineScope {
        scraps = scrapsFrom(Random(seed.hashCode()))
        burst = at
        mark.snapTo(NONE)
        markInk.snapTo(WHOLE)
        shower.snapTo(NONE)
        settle.snapTo(PAGE_REST)

        val shown = launch { shower.animateTo(WHOLE, tween(SCRAP_MILLIS, easing = LinearEasing)) }
        launch {
            settle.animateTo(PAGE_REST - SETTLE_DIP, tween(SETTLE_MILLIS))
            settle.animateTo(PAGE_REST, PaperMotion.sheetSettle)
        }
        launch {
            mark.animateTo(WHOLE, tween(TICK_MILLIS, easing = LinearEasing))
            mark.animateTo(WHOLE, tween(TICK_HOLD_MILLIS))
            markInk.animateTo(NONE, tween(TICK_FADE_MILLIS))
        }
        shown.join()
        burst = null
    }

    internal fun scrapsInFlight(): List<Scrap> = scraps
}

internal class Scrap(
    val shard: Boolean,
    val ink: Int,
    val size: Float,
    val throwX: Float,
    val throwY: Float,
    val spin: Float,
    val phase: Float
)

private fun scrapsFrom(random: Random): List<Scrap> {
    val scraps = ArrayList<Scrap>(SCRAP_COUNT)
    for (index in 0 until SCRAP_COUNT) {
        val speed = THROW_MIN + random.nextFloat() * (THROW_MAX - THROW_MIN)
        val lean = (random.nextFloat() * 2f - WHOLE) * THROW_SPREAD
        scraps.add(
            Scrap(
                shard = index % SHARD_IN == 0,
                ink = random.nextInt(SCRAP_INKS),
                size = random.nextFloat(),
                throwX = speed * lean,
                throwY = -speed,
                spin = SPIN_MIN + random.nextFloat() * (SPIN_MAX - SPIN_MIN),
                phase = random.nextFloat() * WHOLE
            )
        )
    }
    return scraps
}

private const val SCRAP_INKS = 5

@Composable
fun rememberFinishFlourish(): FinishFlourish = remember { FinishFlourish() }

/**
 * Drawn over the page rather than inside it, so the scraps travel across rows and
 * over the completed section instead of being clipped into whichever row threw
 * them.
 */
@Composable
fun PaperFinish(flourish: FinishFlourish, modifier: Modifier = Modifier) {
    val palette = LocalPaperPalette.current
    val inks = remember(palette) {
        listOf(
            palette.stickyNote,
            palette.stickyNoteMid,
            palette.inkBlue,
            palette.inkAmber,
            palette.inkRedSoft
        )
    }
    val markInk = palette.inked(InkTone.Acted)
    Canvas(modifier.fillMaxSize().clearAndSetSemantics {}) {
        val from = flourish.burst ?: return@Canvas
        drawMark(flourish.mark.value, flourish.markInk.value, markInk)
        drawShower(from, flourish.shower.value, flourish.scrapsInFlight(), inks)
    }
}

/**
 * Each leg is stroked on its own with a round cap, which puts a disc of ink at the
 * corner from both sides and joins them without a path. A path would have been the
 * obvious thing and cannot be measured off a device: `android.graphics.Path` is not
 * there in a plain JVM test, and the geometry is the half of this worth testing.
 */
private fun DrawScope.drawMark(drawn: Float, ink: Float, color: Color) {
    if (drawn <= NONE || ink <= NONE) return
    val nib = tickUpTo(size, drawn)
    if (nib.size < TWO_ENDS) return
    val wet = color.copy(alpha = color.alpha * TICK_INK * ink)
    for (leg in 1 until nib.size) {
        drawLine(
            color = wet,
            start = nib[leg - 1],
            end = nib[leg],
            strokeWidth = TICK_NIB.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private const val TWO_ENDS = 2

/**
 * The nib is somewhere along a two-segment polyline, and the ink behind it is
 * everything the nib has already passed over. Measuring it by length rather than
 * by segment is what keeps the pen at one speed through the corner: the down leg
 * is much shorter than the up leg, and a pen given half the stroke for each would
 * visibly slow down at the turn.
 */
internal fun tickUpTo(size: Size, drawn: Float): List<Offset> {
    val start = Offset(size.width * TICK_FROM_X, size.height * TICK_FROM_Y)
    val vertex = Offset(size.width * TICK_VERTEX_X, size.height * TICK_VERTEX_Y)
    val end = Offset(size.width * TICK_TO_X, size.height * TICK_TO_Y)

    val down = hypot(vertex.x - start.x, vertex.y - start.y)
    val up = hypot(end.x - vertex.x, end.y - vertex.y)
    val inked = (down + up) * drawn.coerceIn(NONE, WHOLE)

    if (inked <= down) {
        val along = if (down == NONE) NONE else inked / down
        return listOf(start, start + (vertex - start) * along)
    }
    val along = if (up == NONE) NONE else (inked - down) / up
    return listOf(start, vertex, vertex + (end - vertex) * along)
}

private fun DrawScope.drawShower(
    from: Offset,
    flown: Float,
    scraps: List<Scrap>,
    inks: List<Color>
) {
    if (flown <= NONE || flown >= WHOLE) return
    val seconds = flown * SCRAP_MILLIS / 1000f
    val fade = scrapFade(flown)
    val small = SCRAP_SIZE_MIN.toPx()
    val large = SCRAP_SIZE_MAX.toPx()
    for (scrap in scraps) {
        val at = scrapAt(from, scrap, seconds)
        val side = small + (large - small) * scrap.size
        val ink = inks[scrap.ink % inks.size].copy(alpha = fade)
        if (scrap.shard) {
            rotate(degrees = scrap.spin * seconds * DEGREES, pivot = at) {
                drawRect(
                    color = ink,
                    topLeft = Offset(at.x - side / 2f, at.y - side / 2f),
                    size = Size(side, side * SHARD_SQUAT)
                )
            }
        } else {
            val turned = abs(cos(scrap.spin * seconds + scrap.phase))
            drawOval(
                color = ink,
                topLeft = Offset(at.x - side * turned / 2f, at.y - side / 2f),
                size = Size(side * turned, side)
            )
        }
    }
}

private const val DEGREES = 57.29578f
private const val SHARD_SQUAT = 0.7f

/**
 * Thrown, then dragged, then pulled down. The drag term is why a scrap does not
 * come back to the hand as fast as it left it, which is the difference between
 * paper and a ball bearing.
 */
internal fun scrapAt(from: Offset, scrap: Scrap, seconds: Float): Offset {
    val slowed = seconds * (WHOLE - DRAG * seconds / 2f).coerceAtLeast(NONE)
    return Offset(
        x = from.x + scrap.throwX * slowed,
        y = from.y + scrap.throwY * slowed + GRAVITY * seconds * seconds / 2f
    )
}

internal fun scrapFade(flown: Float): Float =
    if (flown <= SCRAP_FADE_FROM) {
        WHOLE
    } else {
        ((WHOLE - flown) / (WHOLE - SCRAP_FADE_FROM)).coerceIn(NONE, WHOLE)
    }

/**
 * Where the hand goes down, watched rather than caught. The page is covered in
 * gestures that all want the same pointer — a tap that puts the pen down, a row's
 * own tap, a swipe, a long press — so this takes the position on the initial pass
 * and consumes nothing, leaving every one of them to work exactly as before.
 */
fun Modifier.noteWhereTheHandWent(onDown: (Offset) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.firstOrNull { it.pressed && it.previousPressed.not() }
            if (down != null) onDown(down.position)
        }
    }
}
