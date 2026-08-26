package fr.mandarine.todolist.ui.paper

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val SLIP_PADDING = 14.dp
const val REMINDER_SLIP_MILLIS = 2600L
private const val BELL = "🔔"
private const val ALARM = "⏰"
private const val DAY_SKELETON = "dM"

/**
 * A reminder as the reader will meet it: the list's name with a bell in front of it
 * and the day it will ring on after it. It is written the same way whether the
 * tutorial is showing what one looks like or the app has just made one.
 */
@Immutable
class ReminderNote(val listName: String, val day: LocalDate?)

fun reminderSlipText(note: ReminderNote, locale: Locale): String {
    val day = note.day ?: return "$BELL ${note.listName}"
    val pattern = DateFormat.getBestDateTimePattern(locale, DAY_SKELETON)
    return "$BELL ${note.listName} $ALARM ${day.format(DateTimeFormatter.ofPattern(pattern, locale))}"
}

/**
 * What a reminder written just now looks like, dropped from the top of the page for
 * a beat and taken away again. The tutorial ends by showing one of these so the
 * reader knows what to expect; writing a real date shows the same slip, so the
 * promise the tour made is kept in the reader's own handwriting.
 */
@Stable
class ReminderNotes {

    var raised by mutableStateOf<ReminderNote?>(null)
        private set

    internal var last by mutableStateOf<ReminderNote?>(null)
        private set

    fun raise(note: ReminderNote) {
        last = note
        raised = note
    }

    fun lower() {
        raised = null
    }
}

@Composable
fun rememberReminderNotes(): ReminderNotes {
    val notes = remember { ReminderNotes() }
    LaunchedEffect(notes.raised) {
        if (notes.raised == null) return@LaunchedEffect
        delay(REMINDER_SLIP_MILLIS)
        notes.lower()
    }
    return notes
}

@Composable
fun ReminderSlip(notes: ReminderNotes, animated: Boolean, modifier: Modifier = Modifier) {
    val shown = notes.last ?: return
    AnimatedVisibility(
        visible = notes.raised != null,
        modifier = modifier,
        enter = if (animated) {
            slideInVertically { height -> -height } + fadeIn(PaperMotion.rowEnter)
        } else {
            EnterTransition.None
        },
        exit = if (animated) {
            slideOutVertically { height -> -height } + fadeOut(PaperMotion.rowExit)
        } else {
            ExitTransition.None
        }
    ) {
        val palette = LocalPaperPalette.current
        Text(
            text = reminderSlipText(shown, formatLocale),
            modifier = Modifier
                .paperSlip()
                .padding(SLIP_PADDING)
                .semantics { liveRegion = LiveRegionMode.Polite },
            color = palette.inked(InkTone.Words),
            style = PaperType.prose
        )
    }
}
