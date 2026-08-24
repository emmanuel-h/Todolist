package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import fr.mandarine.todolist.R
import kotlinx.coroutines.delay

private const val ON_THE_PAGE = 0f
private const val TORN_OFF = 1f
private const val TEAR_TRAVEL = 0.6f
private const val TEAR_TILT = -2.5f
private val SLIP_WIDTH = 160.dp
private val SLIP_SPENT_WIDTH = 60.dp
private const val SLIP_TICK_MILLIS = 200L
private const val SLIP_FRESH_INK = 1f
private const val SLIP_SPENT_INK = 0.62f
private const val SLIP_WHOLE = 1f
private const val ONE_TICK = 1
private val SLIP_MARGIN = 12.dp
private val SLIP_SHADOW = 6.dp
private val SLIP_CONTACT = 1.dp
private val SLIP_DROP = 2.dp
private const val SLIP_SHADOW_ALPHA = 0.18f
private const val SLIP_CONTACT_ALPHA = 0.26f
private val SLIP_EDGE = 1.dp
private const val SLIP_TILT = -1.2f

/**
 * A deleted row is torn off the page rather than blinked out: it pivots on its own
 * bottom-left corner, slides clear and fades, and only then does the page close
 * the gap it left.
 */
@Composable
fun Modifier.tearOff(torn: Boolean, animated: Boolean, onTorn: () -> Unit): Modifier {
    val progress = remember { Animatable(ON_THE_PAGE) }
    val latestTorn = rememberUpdatedState(onTorn)
    val haptics = rememberPaperHaptics()
    LaunchedEffect(torn) {
        if (!torn) {
            progress.snapTo(ON_THE_PAGE)
            return@LaunchedEffect
        }
        haptics.tearOff()
        if (animated) {
            progress.animateTo(TORN_OFF, PaperMotion.tearOff)
        } else {
            progress.snapTo(TORN_OFF)
        }
        latestTorn.value()
    }
    return this.graphicsLayer {
        val tear = progress.value
        if (tear <= ON_THE_PAGE) return@graphicsLayer
        transformOrigin = TransformOrigin(ON_THE_PAGE, TORN_OFF)
        translationX = size.width * TEAR_TRAVEL * tear
        rotationZ = TEAR_TILT * tear
        alpha = TORN_OFF - tear
    }
}

/**
 * The only thing that survives a tear-off: a scrap of paper carrying one glyph.
 * It puts the row back while it is on screen and takes the deletion with it when
 * it settles away. The scrap is spent as its window runs down — it loses paper and
 * ink every tick — so how much of it is left is how much time is left.
 */
@Composable
fun UndoSlip(
    pending: String?,
    window: Long,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val palette = LocalPaperPalette.current
    val spoken = stringResource(R.string.undo_delete)
    val left = rememberSlipLeft(pending, window)
    AnimatedVisibility(
        visible = pending != null,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.navigationBars.union(WindowInsets.ime)
        ),
        enter = if (animated) slideInVertically { it } + fadeIn() else fadeIn(PaperMotion.instant),
        exit = if (animated) slideOutVertically { it } + fadeOut() else fadeOut(PaperMotion.instant)
    ) {
        Box(
            modifier = Modifier
                .padding(SLIP_MARGIN)
                .width(lerp(SLIP_SPENT_WIDTH, SLIP_WIDTH, left))
                .height(LocalPagePitch.current)
                .graphicsLayer {
                    rotationZ = SLIP_TILT
                    alpha = lerp(SLIP_SPENT_INK, SLIP_FRESH_INK, left)
                }
                .dropShadow(RectangleShape) {
                    radius = SLIP_SHADOW.toPx()
                    alpha = SLIP_SHADOW_ALPHA
                    color = palette.ink
                    offset = Offset(ON_THE_PAGE, SLIP_DROP.toPx())
                }
                .dropShadow(RectangleShape) {
                    radius = SLIP_CONTACT.toPx()
                    alpha = SLIP_CONTACT_ALPHA
                    color = palette.ink
                }
                .paperSheet(tone = palette.paperShade, lit = palette.paperSheet)
                .tornEdge(palette.paperShadeDeep)
                .clickable(onClick = onUndo)
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = spoken
                },
            contentAlignment = Alignment.Center
        ) {
            InkIcon(
                painter = painterResource(R.drawable.ic_undo),
                contentDescription = null,
                tint = palette.inkLive
            )
        }
    }
}

@Composable
private fun rememberSlipLeft(pending: String?, window: Long): Float {
    val ticks = (window / SLIP_TICK_MILLIS).toInt().coerceAtLeast(ONE_TICK)
    var spent by remember { mutableIntStateOf(0) }
    LaunchedEffect(pending, ticks) {
        if (pending == null) return@LaunchedEffect
        spent = 0
        repeat(ticks) { tick ->
            delay(SLIP_TICK_MILLIS)
            spent = tick + ONE_TICK
        }
    }
    return SLIP_WHOLE - spent.toFloat() / ticks
}

private fun Modifier.tornEdge(color: Color): Modifier =
    drawWithCache {
        val thickness = SLIP_EDGE.toPx()
        onDrawWithContent {
            drawContent()
            drawRect(color, Offset(ON_THE_PAGE, ON_THE_PAGE), Size(size.width, thickness))
        }
    }
