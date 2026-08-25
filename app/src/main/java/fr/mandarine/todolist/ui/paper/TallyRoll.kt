package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

private const val TALLY_LABEL = "tally"

/**
 * A number on the page turns the way a counter wheel does rather than being
 * replaced between two frames: the numeral leaving goes up when the tally grows
 * and down when it shrinks, and the new one follows it in from the other side, so
 * which way the count went is visible without reading it.
 */
@Composable
fun TallyRoll(
    count: Int,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    content: @Composable (Int) -> Unit
) {
    AnimatedContent(
        targetState = count,
        modifier = modifier.height(LocalPagePitch.current).clipToBounds(),
        transitionSpec = {
            if (!animated) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val rising = targetState > initialState
                val arriving = slideInVertically(PaperMotion.rowPlacement) {
                    if (rising) it else -it
                } + fadeIn(PaperMotion.rowEnter)
                val leaving = slideOutVertically(PaperMotion.rowPlacement) {
                    if (rising) -it else it
                } + fadeOut(PaperMotion.rowExit)
                arriving togetherWith leaving
            }
        },
        label = TALLY_LABEL
    ) { tally ->
        content(tally)
    }
}
