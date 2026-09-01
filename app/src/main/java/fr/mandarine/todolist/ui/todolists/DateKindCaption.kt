package fr.mandarine.todolist.ui.todolists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.PaperSlipCaption
import kotlinx.coroutines.delay

const val DATE_KIND_SAID_MILLIS = 2600L

/**
 * Which kind was just chosen, for as long as it is worth saying so. The calendar
 * and the alarm are the one pair in this app that a glyph cannot tell apart — that
 * was settled after three wordless attempts — so choosing one says in words what
 * was chosen, and then stops saying it.
 */
@Stable
class DateKindSaid {

    var kind by mutableStateOf<DateKind?>(null)
        private set

    internal var last by mutableStateOf(DateKind.TARGET)
        private set

    fun say(said: DateKind) {
        last = said
        kind = said
    }

    fun hush() {
        kind = null
    }
}

@Composable
fun rememberDateKindSaid(): DateKindSaid {
    val said = remember { DateKindSaid() }
    LaunchedEffect(said.kind) {
        if (said.kind == null) return@LaunchedEffect
        delay(DATE_KIND_SAID_MILLIS)
        said.hush()
    }
    return said
}

/**
 * The words for the kind just chosen, on a slip laid under the marks. The glyphs
 * alone cannot tell target from due, so choosing one says in words what was chosen,
 * and then stops saying it.
 */
@Composable
fun DateKindCaption(said: DateKindSaid, animated: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = said.kind != null,
        modifier = modifier,
        enter = if (animated) fadeIn(PaperMotion.rowEnter) else EnterTransition.None,
        exit = if (animated) fadeOut(PaperMotion.rowExit) else ExitTransition.None
    ) {
        PaperSlipCaption(
            painter = painterResource(
                when (said.last) {
                    DateKind.TARGET -> R.drawable.ic_event
                    DateKind.DUE -> R.drawable.ic_alarm
                }
            ),
            text = stringResource(
                when (said.last) {
                    DateKind.TARGET -> R.string.date_kind_target_caption
                    DateKind.DUE -> R.string.date_kind_due_caption
                }
            )
        )
    }
}
