package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

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

private const val PEEL_HOLDS_INK_UNTIL = 0.9f

/**
 * A sheet is glued along its top edge, so it comes off the pad by folding up from
 * that edge before it travels. The fold opens over the first half of the peel and
 * lies flat again over the second, which is the half that carries the sheet away.
 *
 * The sheet keeps its ink until it has all but arrived, and gives it up in the last
 * tenth. Fading it evenly over the travel was fine while the travel was a hair;
 * over the length of a page it left the sheet transparent well before the line it
 * was headed for, so what the reader watched was a sheet dissolving in mid-air
 * rather than one being put down.
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
        alpha = peelInkAt(peeling),
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

internal fun peelInkAt(travelled: Float): Float {
    if (travelled <= PEEL_HOLDS_INK_UNTIL) return 1f
    val left = STICKY_FOLDED - PEEL_HOLDS_INK_UNTIL
    return ((STICKY_FOLDED - travelled) / left).coerceIn(0f, 1f)
}

/**
 * How far the taken sheet has to be carried, from where the pad is standing to
 * where the sheet is being put down. The pad box is wider than the sheet in it, so
 * the sheet's own seat in that box comes off the distance — otherwise the sheet
 * lands a corner's width past the line it was meant to sit on. A pad with nowhere
 * named to put a sheet down drifts it the pad's own length instead.
 */
fun stickyNoteCarryTo(
    landing: Offset?,
    seat: Offset,
    sheetInset: Float,
    drift: Offset
): Offset {
    if (landing == null) return drift
    return Offset(landing.x - seat.x - sheetInset, landing.y - seat.y - sheetInset)
}
