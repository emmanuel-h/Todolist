package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind

private const val VEIL_ALPHA = 0.35f
private const val CLEAR = 0f
private const val VEIL_LABEL = "pageVeil"

/**
 * The veil belongs to the page, not to the sheets laid on it. However deep the
 * stack goes the page is dimmed exactly once, and a sheet laid on another sheet
 * never dims the one underneath — each sheet only says that it is there, and the
 * page decides what that costs.
 */
class PaperVeil {

    private var sheets by mutableIntStateOf(0)

    val depth: Float get() = if (sheets > 0) VEIL_ALPHA else CLEAR

    val laid: Boolean get() = sheets > 0

    fun raise() {
        sheets += 1
    }

    fun lower() {
        sheets -= 1
    }
}

val LocalPaperVeil = compositionLocalOf { PaperVeil() }

@Composable
internal fun VeilWhileLaid(veil: PaperVeil, laid: Boolean) {
    DisposableEffect(veil, laid) {
        if (laid) veil.raise()
        onDispose { if (laid) veil.lower() }
    }
}

@Composable
internal fun BoxScope.PageVeil() {
    val veil = LocalPaperVeil.current
    val ink = LocalPaperPalette.current.shadow
    val depth = animateFloatAsState(
        targetValue = veil.depth,
        animationSpec = if (veil.depth > CLEAR) PaperMotion.rowEnter else PaperMotion.rowExit,
        label = VEIL_LABEL
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawBehind {
                val laid = depth.value
                if (laid > CLEAR) drawRect(ink, alpha = laid)
            }
    )
}
