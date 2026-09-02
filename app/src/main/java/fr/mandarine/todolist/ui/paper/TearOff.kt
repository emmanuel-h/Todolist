package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
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
private const val SLIP_SPENT = 0f
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
            progress.animateTo(TORN_OFF, PaperMotion.pickUp)
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
        alpha = (TORN_OFF - tear).coerceIn(ON_THE_PAGE, TORN_OFF)
    }
}

/**
 * The only thing that survives a tear-off: a scrap of paper carrying one glyph.
 * It puts the row back while it is on screen and takes the deletion with it when
 * it settles away. The scrap is spent as its window runs down — it loses paper and
 * ink every tick — so how much of it is left is how much time is left.
 *
 * What is spent is the paper, never the reach. The scrap is drawn inside a target
 * that keeps the width it had at full size for the whole window, because a reader
 * aims at where they saw it land: a target that narrows by a finger's width every
 * second turns a delete they meant to take back into one they cannot, and there is
 * no second way to undo.
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
    val press = remember { MutableInteractionSource() }
    AnimatedVisibility(
        visible = pending != null,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.navigationBars.union(WindowInsets.ime)
        ),
        enter = if (animated) {
            slideInVertically(PaperMotion.rowPlacement) { it } + fadeIn(PaperMotion.rowEnter)
        } else {
            EnterTransition.None
        },
        exit = if (animated) {
            slideOutVertically(PaperMotion.rowPlacement) { it } + fadeOut(PaperMotion.rowExit)
        } else {
            ExitTransition.None
        }
    ) {
        Box(
            modifier = Modifier
                .padding(SLIP_MARGIN)
                .width(SLIP_WIDTH)
                .height(maxOf(LocalPagePitch.current, PaperDimens.touchTarget))
                .clickable(interactionSource = press, indication = PaperFocusMark, onClick = onUndo)
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = spoken
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(lerp(SLIP_SPENT_WIDTH, SLIP_WIDTH, left))
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationZ = SLIP_TILT
                        alpha = lerp(SLIP_SPENT_INK, SLIP_FRESH_INK, left)
                    }
                    .raised(RectangleShape, palette) {
                        dropShadow(RectangleShape) {
                            radius = SLIP_SHADOW.toPx()
                            alpha = SLIP_SHADOW_ALPHA
                            color = palette.ink
                            offset = Offset(ON_THE_PAGE, SLIP_DROP.toPx())
                        }.dropShadow(RectangleShape) {
                            radius = SLIP_CONTACT.toPx()
                            alpha = SLIP_CONTACT_ALPHA
                            color = palette.ink
                        }
                    }
                    .paperSheet(tone = palette.paperShade, lit = palette.paperSheet)
                    .tornEdge(palette.paperShadeDeep)
                    .indication(press, PaperIndication),
                contentAlignment = Alignment.Center
            ) {
                InkIcon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = null,
                    tint = palette.inked(InkTone.Acted)
                )
            }
        }
    }
}

/**
 * How much of the window is left, read off the clock the deletion is actually
 * timed by rather than counted up out of the ticks that have been served. Ticks
 * arrive late — on a page that is drawing they all do — and counting them makes a
 * scrap that claims time the delete no longer has.
 */
@Composable
private fun rememberSlipLeft(pending: String?, window: Long): Float {
    var left by remember { mutableFloatStateOf(SLIP_WHOLE) }
    LaunchedEffect(pending, window) {
        if (pending == null) return@LaunchedEffect
        left = SLIP_WHOLE
        val opened = withFrameMillis { it }
        while (left > SLIP_SPENT) {
            delay(SLIP_TICK_MILLIS)
            left = slipLeft(withFrameMillis { it } - opened, window)
        }
    }
    return left
}

internal fun slipLeft(elapsed: Long, window: Long): Float =
    (SLIP_WHOLE - elapsed.toFloat() / window).coerceIn(SLIP_SPENT, SLIP_WHOLE)

private fun Modifier.tornEdge(color: Color): Modifier =
    drawWithCache {
        val thickness = SLIP_EDGE.toPx()
        onDrawWithContent {
            drawContent()
            drawRect(color, Offset(ON_THE_PAGE, ON_THE_PAGE), Size(size.width, thickness))
        }
    }
