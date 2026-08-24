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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val TOUCH_FLOOR = 48.dp
private val HEAD_RULE_GAP = 3.dp
private const val THINNEST_RULE = 1f
private const val SMALLEST_PITCH = 1f
private const val PIXEL_TOLERANCE = 0.5f
private const val INK_GONE = 0f
private const val INK_WHOLE = 1f

val LocalPagePitch = staticCompositionLocalOf { 56.dp }

@Composable
fun pagePitch(style: TextStyle = PaperType.itemLine): Dp =
    with(LocalDensity.current) { maxOf(style.lineHeight.toDp(), TOUCH_FLOOR) }

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
        thickness = maxOf(THINNEST_RULE, PaperDimens.rule.toPx().roundToInt().toFloat()),
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

fun Modifier.pitchHeight(pitch: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val step = pitch.roundToPx().coerceAtLeast(1)
    val lines = ceil(placeable.height.toFloat() / step).toInt().coerceAtLeast(1)
    val height = (lines * step).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(placeable.width, height) { placeable.place(0, 0) }
}

@Composable
fun OnRuleSlot(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier.height(LocalPagePitch.current),
        contentAlignment = Alignment.BottomCenter,
        content = content
    )
}
