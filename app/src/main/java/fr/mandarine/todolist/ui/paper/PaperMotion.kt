package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Five springs, and every movement in the app is one of them. Three move things
 * through space — a pick-up is quick and overshoots the way paper does when it
 * leaves the pad, a row or a sheet settles on the middle spring, and anything the
 * size of the whole page travels on the slow one. Two carry ink instead of paper:
 * ink arrives at one speed and leaves faster, and neither of them bounces, because
 * a pen stroke that overshoots is a pen stroke drawn twice.
 *
 * The numbers are the Material 3 Expressive spatial and effects tokens. They live
 * here rather than being read from `MaterialTheme.motionScheme` because the
 * tutorial's scenes animate outside any composition and the tests read them
 * directly.
 */
@Immutable
object PaperMotion {

    private const val SPATIAL_FAST_DAMPING = 0.6f
    private const val SPATIAL_FAST_STIFFNESS = 800f
    private const val SPATIAL_DAMPING = 0.8f
    private const val SPATIAL_STIFFNESS = 380f
    private const val SPATIAL_SLOW_STIFFNESS = 200f
    private const val INK_ARRIVING_STIFFNESS = 1600f
    private const val INK_LEAVING_STIFFNESS = 3800f
    private const val BREATH_MILLIS = 1900

    val pickUp: SpringSpec<Float> = spring(
        dampingRatio = SPATIAL_FAST_DAMPING,
        stiffness = SPATIAL_FAST_STIFFNESS
    )

    val sheetSettle: SpringSpec<Float> = spring(
        dampingRatio = SPATIAL_DAMPING,
        stiffness = SPATIAL_STIFFNESS
    )

    val sheetTravel: SpringSpec<Rect> = spring(
        dampingRatio = SPATIAL_DAMPING,
        stiffness = SPATIAL_STIFFNESS,
        visibilityThreshold = Rect.VisibilityThreshold
    )

    val rowPlacement: SpringSpec<IntOffset> = spring(
        dampingRatio = SPATIAL_DAMPING,
        stiffness = SPATIAL_STIFFNESS,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    val rowUnfold: SpringSpec<IntSize> = spring(
        dampingRatio = SPATIAL_DAMPING,
        stiffness = SPATIAL_STIFFNESS,
        visibilityThreshold = IntSize.VisibilityThreshold
    )

    val pageMove: SpringSpec<Float> = spring(
        dampingRatio = SPATIAL_DAMPING,
        stiffness = SPATIAL_SLOW_STIFFNESS
    )

    val handGlide: SpringSpec<Offset> = spring(
        dampingRatio = SPATIAL_DAMPING,
        stiffness = SPATIAL_SLOW_STIFFNESS,
        visibilityThreshold = Offset.VisibilityThreshold
    )

    val rowEnter: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = INK_ARRIVING_STIFFNESS
    )

    val rowExit: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = INK_LEAVING_STIFFNESS
    )

    val rowFold: SpringSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = INK_LEAVING_STIFFNESS,
        visibilityThreshold = IntSize.VisibilityThreshold
    )

    /**
     * The one movement that repeats instead of settling, and so the one that has to
     * be given a duration: a spring comes to rest and a breath does not.
     */
    val breath: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(durationMillis = BREATH_MILLIS, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )
}
