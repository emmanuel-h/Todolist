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
 * How far off the page a thing is lifted is a question the light answers, and the
 * light changes. On a daylit page depth is what an object casts below itself; on a
 * night page a cast shadow lands on ink and reads as nothing at all, so the same
 * object is lifted by the hairline of light it catches along its top edge instead.
 * The shadow a sheet would cast in daylight is handed in rather than described
 * here, because every sheet falls from its own height.
 */
fun Modifier.raised(
    shape: Shape,
    palette: PaperPalette,
    strength: () -> Float = { FULLY_RAISED },
    castShadow: Modifier.() -> Modifier
): Modifier = if (palette.byLamplight) {
    this.drawWithCache {
        val edge = shape.createOutline(size, layoutDirection, this)
        val caught = Brush.verticalGradient(
            AT_THE_EDGE to palette.lift,
            OFF_THE_EDGE to Color.Transparent,
            endY = LIT_EDGE.toPx()
        )
        onDrawWithContent {
            drawContent()
            drawOutline(edge, caught, alpha = LIT_EDGE_ALPHA * strength())
        }
    }
} else {
    this.castShadow()
}
