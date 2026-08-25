package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp

private const val FULLY_RAISED = 1f
private const val AT_THE_EDGE = 0f
private const val OFF_THE_EDGE = 1f
private const val LIT_EDGE_ALPHA = 0.3f

private val LIT_EDGE = 1.dp

/**
 * Which edge of an object the lamp finds. A sheet resting on the page catches the
 * light along its top; a sheet sliding off one catches it along the edge it leads
 * with.
 */
enum class LiftEdge { Top, Leading }

/**
 * How far off the page a thing is lifted is a question the light answers, and the
 * light changes. On a daylit page depth is what an object casts below itself; on a
 * night page a cast shadow lands on ink and reads as nothing at all, so the same
 * object is lifted by the hairline of light it catches along its edge instead.
 * The shadow a sheet would cast in daylight is handed in rather than described
 * here, because every sheet falls from its own height.
 */
fun Modifier.raised(
    shape: Shape,
    palette: PaperPalette,
    strength: () -> Float = { FULLY_RAISED },
    edge: LiftEdge = LiftEdge.Top,
    castShadow: Modifier.() -> Modifier
): Modifier = if (palette.byLamplight) {
    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val caught = when (edge) {
            LiftEdge.Top -> Brush.verticalGradient(
                AT_THE_EDGE to palette.lift,
                OFF_THE_EDGE to Color.Transparent,
                endY = LIT_EDGE.toPx()
            )
            LiftEdge.Leading -> Brush.horizontalGradient(
                AT_THE_EDGE to palette.lift,
                OFF_THE_EDGE to Color.Transparent,
                endX = LIT_EDGE.toPx()
            )
        }
        onDrawWithContent {
            drawContent()
            drawOutline(outline, caught, alpha = LIT_EDGE_ALPHA * strength())
        }
    }
} else {
    this.castShadow()
}
