package fr.mandarine.todolist.ui.todolists

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.ListHorizon
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperGutter
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.formatLocale
import fr.mandarine.todolist.ui.paper.inked
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val COVER_SKELETON = "EEEEdMMMM"
private const val ONE_LINE = 1
private val COVER_FOOT = 14.dp
private val LINE_GAP = 2.dp

/** How many rules the cover takes off the top of the page of lists. */
internal const val COVER_PITCHES = 4

internal fun coverHeight(pitch: Dp): Dp = pitch * COVER_PITCHES

/**
 * The head of the page of lists, printed the way the front of a good notebook is:
 * the day, the notebook's name, and one line saying how things stand. It is the
 * notebook speaking, so it is set in print rather than in the reader's hand, and it
 * sits above the ruling — nothing is written on it.
 *
 * The standing line is left out on a blank page. There is nothing to report, and
 * the pad in the corner is the invitation.
 */
@Composable
internal fun ListsCover(state: TodoListsState, today: LocalDate, modifier: Modifier = Modifier) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    val day = remember(today, locale) {
        today.format(
            DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, COVER_SKELETON), locale)
        ).uppercase(locale)
    }
    val standing = standingLine(state)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(coverHeight(LocalPagePitch.current))
            .padding(
                start = LocalPaperGutter.current,
                end = PaperDimens.rowEndPadding,
                bottom = COVER_FOOT
            ),
        verticalArrangement = Arrangement.spacedBy(LINE_GAP, Alignment.Bottom)
    ) {
        Text(
            text = day,
            style = PaperType.kicker,
            color = palette.inked(InkTone.Margin),
            maxLines = ONE_LINE,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.semantics { heading() },
            style = PaperType.masthead,
            color = palette.inked(InkTone.Words),
            maxLines = ONE_LINE,
            overflow = TextOverflow.Ellipsis
        )
        if (standing != null) {
            Text(
                text = standing,
                style = PaperType.note,
                color = palette.inked(InkTone.Margin),
                maxLines = ONE_LINE,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun standingLine(state: TodoListsState): String? {
    val content = state as? TodoListsState.Content ?: return null
    val open = content.activeSummaries
    if (open.isEmpty()) return stringResource(R.string.summary_all_finished)
    val today = open.count { it.horizon == ListHorizon.TODAY }
    val overdue = open.count { it.horizon == ListHorizon.OVERDUE }
    val parts = buildList {
        add(pluralStringResource(R.plurals.summary_open_lists, open.size, open.size))
        if (today > 0) add(pluralStringResource(R.plurals.summary_today, today, today))
        if (overdue > 0) add(pluralStringResource(R.plurals.summary_overdue, overdue, overdue))
    }
    return parts.joinToString(stringResource(R.string.summary_separator))
}
