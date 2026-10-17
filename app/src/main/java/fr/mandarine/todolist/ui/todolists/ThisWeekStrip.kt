package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.pinnedPaper

private const val NAMES_SHOWN = 2
private const val STRIP_TILT_DEGREES = -0.7f
private val STRIP_HORIZONTAL = 14.dp
private val STRIP_VERTICAL = 8.dp
private val STRIP_GAP = 12.dp
private val NAME_GAP = 10.dp
private val DOT = 7.dp
private val DOT_GAP = 5.dp
private val PIN = 12.dp
private val PIN_RISE = 6.dp

/**
 * A narrow slip pinned under the board's head, naming the lists that need the
 * reader this week — anything missed, anything for today, anything in the next
 * seven days — each after a dot in the ink its day is written in. It is the only
 * place the board talks about days, and it is drawn only while there is something
 * to say: a reader who never dates a list never sees it.
 *
 * The whole slip is one press, and it opens the calendar.
 */
@Composable
fun ThisWeekStrip(
    thisWeek: List<TodoListSummary>,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPaperPalette.current
    val shown = thisWeek.take(NAMES_SHOWN)
    val more = thisWeek.size - shown.size
    val names = shown.joinToString(stringResource(R.string.list_separator)) { it.list.name }
    val spoken = if (more > 0) {
        pluralStringResource(R.plurals.this_week_spoken_more, more, names, more)
    } else {
        stringResource(R.string.this_week_spoken, names)
    }
    val opens = stringResource(R.string.open_calendar)
    Box(modifier = modifier.fillMaxWidth().graphicsLayer { rotationZ = STRIP_TILT_DEGREES }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PaperDimens.touchTarget)
                .pinnedPaper()
                .clickable(onClick = onOpenCalendar)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    role = Role.Button
                    onClick(label = opens) { onOpenCalendar(); true }
                }
                .padding(horizontal = STRIP_HORIZONTAL, vertical = STRIP_VERTICAL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(STRIP_GAP)
        ) {
            InkIcon(
                painter = painterResource(R.drawable.ic_event),
                contentDescription = null,
                tint = palette.inked(InkTone.Words),
                size = PaperDimens.rowGlyph
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.this_week_kicker),
                    style = PaperType.kicker,
                    color = palette.inked(InkTone.Margin),
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NAME_GAP)
                ) {
                    shown.forEach { summary -> WeekName(summary, Modifier.weight(1f, fill = false)) }
                    if (more > 0) {
                        Text(
                            text = stringResource(R.string.more_count, more),
                            style = PaperType.meta,
                            color = palette.inked(InkTone.Margin),
                            maxLines = 1
                        )
                    }
                }
            }
            InkIcon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = palette.inked(InkTone.Margin),
                size = PaperDimens.rowGlyph
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(bottom = PIN_RISE)
                .graphicsLayer { translationY = -PIN_RISE.toPx() }
                .size(PIN)
                .drawWithCache {
                    val head = palette.inked(InkTone.Words)
                    val rim = palette.shadow
                    onDrawBehind {
                        drawCircle(rim)
                        drawCircle(head, radius = size.minDimension * 0.38f)
                    }
                }
        )
    }
}

@Composable
private fun WeekName(summary: TodoListSummary, modifier: Modifier = Modifier) {
    val palette = LocalPaperPalette.current
    val tone = cardDay(summary)?.tone ?: InkTone.Margin
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(DOT)
                .drawWithCache {
                    val ink = palette.inked(tone)
                    onDrawBehind { drawCircle(ink) }
                }
        )
        Box(Modifier.size(DOT_GAP))
        Text(
            text = remember(summary.list.name) { handwritten(summary.list.name) },
            style = PaperType.cardLine,
            color = palette.inked(InkTone.Words),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
