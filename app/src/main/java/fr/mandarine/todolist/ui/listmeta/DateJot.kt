package fr.mandarine.todolist.ui.listmeta

import android.text.format.DateFormat
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
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.listDateFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val JOT_SKELETON = "dMMM"
private const val JOT_YEAR_SKELETON = "dMMMyy"
private val JOT_SPACING = 4.dp
private val JOT_END_PADDING = 8.dp
private const val ONE_LINE = 1

@Composable
fun DateJot(
    date: LocalDate,
    kind: DateKind,
    showYear: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault(Locale.Category.FORMAT)
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
    Row(
        modifier = modifier
            .padding(end = JOT_END_PADDING)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
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
