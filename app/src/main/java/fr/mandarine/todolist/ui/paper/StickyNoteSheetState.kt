package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Immutable

const val STICKY_PEEL_REST = 0f
const val STICKY_PEEL_LIFTED = 1f
const val STICKY_PEEL_GONE = 2f
const val STICKY_SETTLE_START = 0f
const val STICKY_SETTLE_DONE = 1f
const val STICKY_FLAT = 0f
const val STICKY_FOLDED = 1f

@Immutable
class StickyNoteSheetState(
    val rotationDegrees: Float,
    val scale: Float,
    val alpha: Float,
    val travelFraction: Float,
    val foldFraction: Float
)

private const val REST_ROTATION = 0f
private const val LIFT_ROTATION = -6f
private const val PEEL_ROTATION = -14f
private const val REST_SCALE = 1f
private const val LIFT_SCALE = 1.08f
private const val SETTLE_SCALE = 0.9f

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

/**
 * A sheet is glued along its top edge, so it comes off the pad by folding up from
 * that edge before it travels. The fold opens over the first half of the peel and
 * lies flat again over the second, which is the half that carries the sheet away.
 */
fun stickyNotePeelAt(progress: Float): StickyNoteSheetState {
    val clamped = progress.coerceIn(STICKY_PEEL_REST, STICKY_PEEL_GONE)
    if (clamped <= STICKY_PEEL_LIFTED) {
        return StickyNoteSheetState(
            rotationDegrees = lerp(REST_ROTATION, LIFT_ROTATION, clamped),
            scale = lerp(REST_SCALE, LIFT_SCALE, clamped),
            alpha = 1f,
            travelFraction = 0f,
            foldFraction = clamped
        )
    }
    val peeling = clamped - STICKY_PEEL_LIFTED
    return StickyNoteSheetState(
        rotationDegrees = lerp(LIFT_ROTATION, PEEL_ROTATION, peeling),
        scale = lerp(LIFT_SCALE, REST_SCALE, peeling),
        alpha = 1f - peeling,
        travelFraction = peeling,
        foldFraction = STICKY_FOLDED - peeling
    )
}

fun stickyNoteSettleAt(progress: Float): StickyNoteSheetState {
    val clamped = progress.coerceIn(STICKY_SETTLE_START, STICKY_SETTLE_DONE)
    return StickyNoteSheetState(
        rotationDegrees = lerp(LIFT_ROTATION, REST_ROTATION, clamped),
        scale = lerp(SETTLE_SCALE, REST_SCALE, clamped),
        alpha = clamped,
        travelFraction = 0f,
        foldFraction = STICKY_FLAT
    )
}
