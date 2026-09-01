package fr.mandarine.todolist.ui.nav

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.ui.paper.LiftEdge
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.raised

private const val NAME_KEY = "list-name-"
private const val ABOVE_THE_PAGE = 1f
private const val PEEL_LABEL = "peelingEdge"
private const val FLAT = 0f
private const val PEELED = 1f
private const val PEEL_SHADOW_ALPHA = 0.22f
private val PEEL_SHADOW = 16.dp
private val PEEL_SHADOW_DRIFT = 4.dp

/**
 * The two scopes a mark needs to travel between pages: the layout both pages live
 * in, and the arrival the mark is riding. A screen composed without them — a
 * preview or a test — reads no travel at all and every travelling modifier stands
 * down.
 */
@Immutable
class PageTravel(
    val pages: SharedTransitionScope,
    val arrival: AnimatedContentScope
)

val LocalPageTravel = compositionLocalOf<PageTravel?> { null }

/**
 * The name is the one mark written on both pages, and so the one mark that travels:
 * the same words leave the row a finger lands on and grow into the head rule of the
 * page they open, while the rows around them stay where they are.
 *
 * They travel as shared *bounds* rather than as a shared element. A shared element
 * draws only the page it is arriving on, so for every frame that page has not yet
 * placed the words — the frames before an opened page is first measured, and the
 * whole of a back gesture while the finger is still down — nothing anywhere draws
 * them and the name falls out of both pages at once. Shared bounds keep both copies
 * alive and hand over between them, which conserves the ink: the two halves of the
 * handover are the same spring run in opposite directions and always sum to one
 * full-strength mark.
 *
 * The handover is on ink speed rather than the speed of the travel, because the two
 * copies are the same words set at the two sizes the two pages write them at, and
 * neither size can be scaled onto the other exactly. Held together they read as one
 * word written twice with the pen slipping. Over in a tenth of a second, and taken
 * where the two copies are still nearly on top of each other, it reads as one word.
 *
 * Nothing larger than the words travels. Wrapping the whole rule in shared bounds
 * as well collapsed these ones — nested inside the travelling row the name measured
 * out at a couple of pixels — and the row is not really the same object on the two
 * pages anyway: it carries a tally the head rule has no room for, and the head rule
 * carries a back glyph no row has.
 */
@Composable
fun Modifier.travellingName(listId: String): Modifier {
    val travel = LocalPageTravel.current ?: return this
    return with(travel.pages) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(NAME_KEY + listId),
            animatedVisibilityScope = travel.arrival,
            enter = fadeIn(PaperMotion.rowEnter),
            exit = fadeOut(PaperMotion.rowEnter),
            boundsTransform = { _, _ -> PaperMotion.sheetTravel },
            resizeMode = ResizeMode.scaleToBounds(ContentScale.FillHeight, Alignment.TopStart),
            zIndexInOverlay = ABOVE_THE_PAGE
        )
    }
}

/**
 * A sheet only shows its own thickness once it starts to move: the shadow it casts
 * along the edge it leads with sits just off the page while the sheet lies flat, so
 * it costs nothing at rest and grows with the finger as the sheet is peeled off.
 */
@Composable
fun Modifier.peelingEdge(): Modifier {
    val travel = LocalPageTravel.current ?: return this
    val palette = LocalPaperPalette.current
    val lifted = travel.arrival.transition.animateFloat(
        transitionSpec = { PaperMotion.sheetSettle },
        label = PEEL_LABEL
    ) { state -> if (state == EnterExitState.Visible) FLAT else PEELED }
    return raised(RectangleShape, palette, { lifted.value }, LiftEdge.Leading) {
        dropShadow(RectangleShape) {
            radius = PEEL_SHADOW.toPx() * lifted.value
            alpha = PEEL_SHADOW_ALPHA * lifted.value
            color = palette.shadow
            offset = Offset(-PEEL_SHADOW_DRIFT.toPx() * lifted.value, FLAT)
        }
    }
}
