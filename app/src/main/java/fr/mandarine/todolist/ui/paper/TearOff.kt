package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

private const val ON_THE_PAGE = 0f
private const val TORN_OFF = 1f
private const val TEAR_TRAVEL = 0.6f
private const val TEAR_TILT = -2.5f

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
