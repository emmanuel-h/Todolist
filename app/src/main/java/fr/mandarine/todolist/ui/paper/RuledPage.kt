package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

private const val PITCH_SAMPLE = "Ag"
private val TOUCH_FLOOR = 48.dp
private val HEAD_RULE_GAP = 3.dp
private const val THINNEST_RULE = 1f
private const val SMALLEST_PITCH = 1f
private const val PIXEL_TOLERANCE = 0.5f
private const val ROUNDING_SLACK = 1f
private const val INK_GONE = 0f
private const val INK_WHOLE = 1f
private const val INK_FAINT_AT = 0.24f
private const val INK_FAINT = 0.08f
private const val INK_RUNNING_AT = 0.48f
private const val INK_RUNNING = 0.72f
private const val INK_SETTLED_AT = 0.68f
private const val AT_REST = 0f

val LocalPagePitch = staticCompositionLocalOf { 56.dp }

/**
 * The pitch is the line the hand actually writes on, so it is measured rather
 * than derived: `lineHeight` resolves through the platform's font-scale curve and
 * through the font's own metrics, and only a real measurement at the current
 * scale knows how tall the line ends up.
 */
@Composable
fun pagePitch(style: TextStyle = PaperType.itemLine): Dp {
    val measurer = rememberTextMeasurer()
    val resolved = remember(measurer, style) {
        measurer.measure(PITCH_SAMPLE, style).size.height
    }
    return with(LocalDensity.current) { maxOf(resolved.toDp(), TOUCH_FLOOR) }
}

@Composable
fun pageVerticalInsets(): PaddingValues = WindowInsets.safeDrawing
    .exclude(WindowInsets.ime)
    .only(WindowInsetsSides.Vertical)
    .asPaddingValues()

@Composable
fun Modifier.pageFrame(): Modifier = this
    .widthIn(max = PaperDimens.pageWidth)
    .fillMaxSize()
    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    .imePadding()

@Immutable
internal class PageRuling(
    val color: Color,
    val start: Float,
    val head: Float,
    val pitch: Float,
    val thickness: Float,
    val gap: Float
)

fun Modifier.ruledPage(
    listState: LazyListState,
    pitch: Dp,
    headMargin: Dp,
    color: Color,
    gutter: Dp = PaperDimens.gutter
): Modifier = drawWithCache {
    val ruling = PageRuling(
        color = color,
        start = gutter.toPx(),
        head = headMargin.toPx(),
        pitch = pitch.toPx().coerceAtLeast(SMALLEST_PITCH),
        thickness = ruleThickness(),
        gap = HEAD_RULE_GAP.toPx()
    )
    onDrawBehind {
        drawPageRules(
            ruling = ruling,
            scrolled = listState.firstVisibleItemScrollOffset,
            headVisible = listState.firstVisibleItemIndex == 0
        )
    }
}

internal fun Density.ruleThickness(): Float =
    maxOf(THINNEST_RULE, PaperDimens.rule.toPx().roundToInt().toFloat())

internal fun DrawScope.drawPageRules(ruling: PageRuling, scrolled: Int, headVisible: Boolean) {
    val width = size.width - ruling.start
    val headRule = headRuleOffset(ruling.head, scrolled, ruling.thickness)
    val ceiling = if (headVisible) headRule else 0f
    var line = firstRuleOffset(ruling.head, scrolled, ruling.thickness, ruling.pitch)
    while (line < size.height) {
        if (line + PIXEL_TOLERANCE >= ceiling) {
            drawRect(
                ruling.color,
                Offset(ruling.start, floor(line)),
                Size(width, ruling.thickness)
            )
        }
        line += ruling.pitch
    }
    if (headVisible) {
        drawRect(
            ruling.color,
            Offset(ruling.start, floor(headRule + ruling.gap)),
            Size(width, ruling.thickness)
        )
    }
}

internal fun firstRuleOffset(
    headPx: Float,
    scrolledPx: Int,
    thickness: Float,
    pitchPx: Float
): Float {
    val base = headPx - scrolledPx - thickness
    return base - floor(base / pitchPx) * pitchPx
}

internal fun headRuleOffset(
    headPx: Float,
    scrolledPx: Int,
    thickness: Float
): Float = headPx - thickness - scrolledPx

/**
 * Writing that scrolls into the head margin runs out rather than greys out: it
 * holds its ink until it is well inside the margin, then gives way over the last
 * stretch. A row caught halfway is a row whose top has left the page, not a row
 * that has been switched off.
 */
fun Modifier.headMarginFade(listState: LazyListState, headMargin: Dp): Modifier = this
    .graphicsLayer {
        compositingStrategy = if (listState.canScrollBackward) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
    .drawWithCache {
        val band = headMargin.toPx()
        val dissolve = Brush.verticalGradient(
            INK_GONE to Color.Transparent,
            INK_FAINT_AT to Color.Black.copy(alpha = INK_FAINT),
            INK_RUNNING_AT to Color.Black.copy(alpha = INK_RUNNING),
            INK_SETTLED_AT to Color.Black,
            INK_WHOLE to Color.Black,
            endY = band
        )
        onDrawWithContent {
            drawContent()
            if (listState.canScrollBackward) {
                drawRect(
                    brush = dissolve,
                    size = Size(size.width, band),
                    blendMode = BlendMode.DstIn
                )
            }
        }
    }

/**
 * Ink that is fixed to the window rather than written on the page still belongs on
 * a line. It holds still while the page is moving — a mark that chased the ruling
 * would jitter through every fling — and settles onto the rule nearest its seat as
 * soon as the page comes to rest.
 */
@Composable
fun Modifier.settleOnRule(listState: LazyListState, headMargin: Dp, pitch: Dp): Modifier {
    val density = LocalDensity.current
    val resting = with(density) {
        if (listState.isScrollInProgress) {
            null
        } else {
            nearestRuleShift(
                headPx = headMargin.toPx(),
                scrolledPx = listState.firstVisibleItemScrollOffset,
                thickness = ruleThickness(),
                pitchPx = pitch.toPx().coerceAtLeast(SMALLEST_PITCH)
            )
        }
    }
    val latest = rememberUpdatedState(resting)
    val seat = remember { Animatable(AT_REST) }
    LaunchedEffect(seat) {
        snapshotFlow { latest.value }.collectLatest { rest ->
            if (rest != null) seat.animateTo(rest, PaperMotion.sheetSettle)
        }
    }
    return this.graphicsLayer { translationY = seat.value }
}

internal fun nearestRuleShift(
    headPx: Float,
    scrolledPx: Int,
    thickness: Float,
    pitchPx: Float
): Float {
    val seat = headPx - thickness
    val first = firstRuleOffset(headPx, scrolledPx, thickness, pitchPx)
    val lines = ((seat - first) / pitchPx).roundToInt()
    return first + lines * pitchPx - seat
}

fun Modifier.pitchHeight(pitch: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val step = pitch.roundToPx().coerceAtLeast(1)
    val filled = placeable.height - ROUNDING_SLACK
    val lines = ceil(filled / step).toInt().coerceAtLeast(1)
    val height = (lines * step).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(placeable.width, height) { placeable.place(0, 0) }
}

@Composable
fun OnRuleSlot(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomCenter,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.height(LocalPagePitch.current),
        contentAlignment = alignment,
        content = content
    )
}
