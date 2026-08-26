package fr.mandarine.todolist.ui.listmeta

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.LocalRuledHand
import fr.mandarine.todolist.ui.paper.OnRuleSlot
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.formatLocale
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.DateSelection
import fr.mandarine.todolist.ui.todolists.listDateFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val JOT_SKELETON = "dMMM"
private const val JOT_YEAR_SKELETON = "dMMMyy"
private val JOT_SPACING = 4.dp
private val JOT_END_PADDING = 8.dp
private const val ONE_LINE = 1

/**
 * A date written in the margin of a line. Wherever the same mark is drawn it means
 * the same thing, so a jot that can be rewritten carries its calendar with it
 * rather than leaving the line it sits on to answer for the tap.
 *
 * The tap and the words are declared on the one node: a click hung on the row that
 * holds the jot is a separate node from the one that says the date, which leaves a
 * screen reader a mark it can read but not press and a press it cannot name.
 */
@Composable
fun DateJot(
    date: LocalDate,
    kind: DateKind,
    showYear: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    onRewrite: ((DateSelection) -> Unit)? = null
) {
    val locale = formatLocale
    val formatter = remember(locale, showYear) { jotFormatter(locale, showYear) }
    val jotted = remember(date, formatter) { date.format(formatter) }
    val spokenFormatter = remember(locale) { listDateFormatter(locale, showYear = true) }
    val spoken = stringResource(
        when (kind) {
            DateKind.TARGET -> R.string.target_date_description
            DateKind.DUE -> R.string.due_date_description
        },
        remember(date, spokenFormatter) { date.format(spokenFormatter) }
    )
    val rewriting = stringResource(
        when (kind) {
            DateKind.TARGET -> R.string.set_target_date
            DateKind.DUE -> R.string.set_due_date
        }
    )
    val rewrite = onRewrite?.let { write -> { write(DateSelection(kind, date)) } }
    Row(
        modifier = modifier
            .padding(end = JOT_END_PADDING)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .then(
                if (rewrite == null) {
                    Modifier
                } else {
                    Modifier.clickable(onClickLabel = rewriting, onClick = rewrite)
                }
            ),
        verticalAlignment = Alignment.Top
    ) {
        OnRuleSlot {
            InkIcon(
                painter = painterResource(
                    when (kind) {
                        DateKind.TARGET -> R.drawable.ic_event
                        DateKind.DUE -> R.drawable.ic_alarm
                    }
                ),
                contentDescription = null,
                tint = tint,
                size = PaperDimens.jotGlyph
            )
        }
        Spacer(Modifier.width(JOT_SPACING))
        Text(
            text = jotted,
            modifier = Modifier.seatOnRule(),
            style = LocalRuledHand.current.margin,
            color = tint,
            softWrap = false,
            maxLines = ONE_LINE
        )
    }
}

fun formatJotDate(date: LocalDate, showYear: Boolean, locale: Locale): String =
    date.format(jotFormatter(locale, showYear))

private fun jotFormatter(locale: Locale, showYear: Boolean): DateTimeFormatter {
    val skeleton = if (showYear) JOT_YEAR_SKELETON else JOT_SKELETON
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}
