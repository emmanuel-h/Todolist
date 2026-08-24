package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@Immutable
object PaperMotion {

    val sheetLift: SpringSpec<Float> = spring(
        dampingRatio = 0.42f,
        stiffness = 1400f
    )

    val sheetSettle: SpringSpec<Float> = spring(
        dampingRatio = 0.62f,
        stiffness = 700f
    )

    val rowEnter: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 500f
    )

    val rowExit: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 800f
    )

    val rowPlacement: SpringSpec<IntOffset> = spring(
        dampingRatio = 0.85f,
        stiffness = 500f,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    val penStroke: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 1600f
    )

    val swipeSettle: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    val tearOff: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 800f
    )

    val slipShadow: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 800f
    )

    val slipGrip: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 1600f
    )

    val pressWash: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 1600f
    )

    val pressRelease: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 3800f
    )

    val nibSquash: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 800f
    )

    val lineUnfold: SpringSpec<IntSize> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f,
        visibilityThreshold = IntSize.VisibilityThreshold
    )

    val lineFold: SpringSpec<IntSize> = spring(
        dampingRatio = 1f,
        stiffness = 3800f,
        visibilityThreshold = IntSize.VisibilityThreshold
    )

    val lineInk: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 1600f
    )

    val seamShade: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 400f
    )

    val instant: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
}
