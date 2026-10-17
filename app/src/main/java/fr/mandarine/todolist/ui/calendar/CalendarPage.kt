package fr.mandarine.todolist.ui.calendar

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.nav.travellingName
import fr.mandarine.todolist.ui.paper.CorkDimens
import fr.mandarine.todolist.ui.paper.CorkSurface
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperTape
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.formatLocale
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.leadingBlanks
import fr.mandarine.todolist.ui.paper.pinnedPaper
import fr.mandarine.todolist.ui.paper.tapeFor
import fr.mandarine.todolist.ui.paper.weekdayInitials
import fr.mandarine.todolist.ui.todolists.BoardBarHeight
import fr.mandarine.todolist.ui.todolists.CardDay
import fr.mandarine.todolist.ui.todolists.cardDate
import fr.mandarine.todolist.ui.todolists.cardDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

private const val WEEK_DAYS = 7
private const val MONTH_SKELETON = "LLLL"
private const val SHEET_TILT_DEGREES = -0.4f
private const val SLIP_TILT_DEGREES = 0.6f
private val PAGE_WIDTH = 560.dp
private val PAGE_TOP = 12.dp
private val PAGE_FOOT = 24.dp
private val PAGE_SIDE = 16.dp
private val SHEET_PADDING = 14.dp
private val DAY_CELL = 40.dp
private val TODAY_DISC = 32.dp
private val DAY_MARK = 6.dp
private val MARK_GAP = 1.dp
private val SLIP_START = 16.dp
private val SLIP_VERTICAL = 12.dp
private val SLIP_GAP = 12.dp
private val TAPE_INSET = 20.dp
private val LABEL_PADDING = 10.dp

/**
 * The Calendar tab: the dated lists, and only those, laid out the way a wall
 * calendar and the slips pinned under it would be. A month sheet marks every day a
 * list is about; under it the dated lists themselves, late first, then today, then
 * what is coming. Most lists have no day, and the page says so in so many words, so
 * nobody takes the calendar for the place everything lives.
 */
@Composable
fun CalendarPage(
    state: TodoListsState,
    today: LocalDate,
    month: YearMonth?,
    onMonthTurned: (YearMonth) -> Unit,
    onOpenList: (TodoList) -> Unit,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val content = state as? TodoListsState.Content
    val agenda = content?.agenda.orEmpty()
    val undated = content?.undatedCount ?: 0
    val shown = month ?: YearMonth.from(today)
    val bars = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical).asPaddingValues()
    CorkSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = PAGE_WIDTH)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(
                start = PAGE_SIDE,
                end = PAGE_SIDE,
                top = bars.calculateTopPadding() + PAGE_TOP,
                bottom = bars.calculateBottomPadding() + BoardBarHeight + PAGE_FOOT
            ),
            verticalArrangement = Arrangement.spacedBy(CorkDimens.cardGap)
        ) {
            item(key = "month") {
                MonthSheet(
                    month = shown,
                    today = today,
                    marks = remember(agenda, shown) { marksIn(agenda, shown) },
                    onTurn = onMonthTurned
                )
            }
            items(agenda, key = { it.list.id }) { summary ->
                AgendaSlip(summary = summary, onOpen = { onOpenList(summary.list) })
            }
            if (content != null) {
                item(key = "closing") {
                    ClosingLabel(datedCount = agenda.size, undatedCount = undated)
                }
            }
        }
    }
}

/**
 * The month, pinned at the top: its name and year in print, the two arrows that turn
 * it, the weekday initials, and the days. Today is filled in ink; a day a list is
 * about carries a dot in the ink that list's day is written in — the loudest one
 * when several lists share a day.
 */
@Composable
private fun MonthSheet(
    month: YearMonth,
    today: LocalDate,
    marks: Map<Int, InkTone>,
    onTurn: (YearMonth) -> Unit
) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val initials = remember(firstDay, locale) { weekdayInitials(firstDay, locale) }
    val name = remember(month, locale) {
        month.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, MONTH_SKELETON), locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    val blanks = leadingBlanks(month, firstDay)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = SHEET_TILT_DEGREES }
            .pinnedPaper()
            .padding(SHEET_PADDING)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = month.year.toString(),
                    style = PaperType.kicker,
                    color = palette.inked(InkTone.Margin)
                )
                Text(
                    text = name,
                    modifier = Modifier.semantics { heading() },
                    style = PaperType.masthead,
                    color = palette.inked(InkTone.Words),
                    maxLines = 1
                )
            }
            InkIconButton(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.previous_month),
                onClick = { onTurn(month.minusMonths(1)) },
                tint = palette.inked(InkTone.Words)
            )
            InkIconButton(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.next_month),
                onClick = { onTurn(month.plusMonths(1)) },
                tint = palette.inked(InkTone.Words)
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = SHEET_PADDING / 2)) {
            initials.forEach { initial ->
                Text(
                    text = initial,
                    modifier = Modifier.weight(1f).clearAndSetSemantics { },
                    style = PaperType.kicker,
                    color = palette.inked(InkTone.Margin),
                    textAlign = TextAlign.Center
                )
            }
        }
        val cells = blanks + month.lengthOfMonth()
        val weeks = (cells + WEEK_DAYS - 1) / WEEK_DAYS
        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(WEEK_DAYS) { column ->
                    val day = week * WEEK_DAYS + column - blanks + 1
                    Box(modifier = Modifier.weight(1f).height(DAY_CELL), contentAlignment = Alignment.Center) {
                        if (day in 1..month.lengthOfMonth()) {
                            DayCell(date = month.atDay(day), today = today, mark = marks[day])
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, mark: InkTone?) {
    val palette = LocalPaperPalette.current
    val isToday = date == today
    val disc = palette.inked(InkTone.Words)
    val dot = mark?.let { palette.inked(it) }
    Box(
        modifier = Modifier
            .size(TODAY_DISC)
            .drawWithCache {
                val markRadius = DAY_MARK.toPx() / 2
                onDrawBehind {
                    if (isToday) drawCircle(disc)
                    if (dot != null) {
                        drawCircle(
                            dot,
                            radius = markRadius,
                            center = center.copy(y = size.height + markRadius + MARK_GAP.toPx())
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = PaperType.numeral,
            color = when {
                isToday -> palette.paper
                date.isBefore(today) -> palette.inked(InkTone.Margin)
                else -> palette.inked(InkTone.Words)
            }
        )
    }
}

/**
 * One dated list, as a slip taped under the month: what its day is, its name in the
 * reader's hand, which kind of day it is in words, and how far through it they are.
 * A tap opens the list.
 */
@Composable
private fun AgendaSlip(summary: TodoListSummary, onOpen: () -> Unit) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    val list = summary.list
    val date = list.targetDate ?: list.dueDate ?: return
    val day = cardDay(summary) ?: return
    val showYear = if (list.targetDate != null) summary.showTargetYear else summary.showDueDateYear
    val dated = remember(date, showYear, locale) { cardDate(date, showYear, locale).uppercase(locale) }
    val phrase = when (day) {
        CardDay.Late -> stringResource(R.string.agenda_late, dated)
        CardDay.Today -> stringResource(R.string.card_today)
        CardDay.Spent, CardDay.Ahead -> dated
    }
    val caption = stringResource(
        if (list.targetDate != null) R.string.date_kind_target_caption else R.string.date_kind_due_caption
    )
    val total = summary.activeCount + summary.completedCount
    val note = if (total > 0) {
        caption + stringResource(R.string.summary_separator) +
            stringResource(R.string.progress_fraction, summary.completedCount, total)
    } else {
        caption
    }
    Box(modifier = Modifier.fillMaxWidth().graphicsLayer { rotationZ = SLIP_TILT_DEGREES }) {
        Row(
            modifier = Modifier
                .padding(top = CorkDimens.tapeOverlap)
                .fillMaxWidth()
                .pinnedPaper()
                .clickable(onClick = onOpen)
                .padding(start = SLIP_START, top = SLIP_VERTICAL, end = SLIP_START / 2, bottom = SLIP_VERTICAL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SLIP_GAP)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phrase,
                    style = PaperType.kicker,
                    color = palette.inked(day.tone),
                    textDecoration = if (day == CardDay.Spent) TextDecoration.LineThrough else null,
                    maxLines = 1
                )
                Text(
                    text = remember(list.name) { handwritten(list.name) },
                    modifier = Modifier.travellingName(list.id),
                    style = PaperType.listLine,
                    color = palette.inked(InkTone.Words),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = note,
                    style = PaperType.meta,
                    color = palette.inked(InkTone.Margin),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            InkIcon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = palette.inked(InkTone.Margin),
                size = PaperDimens.rowGlyph
            )
        }
        PaperTape(colour = palette.tapeFor(list.colour), modifier = Modifier.padding(start = TAPE_INSET))
    }
}

/**
 * The last word on the calendar, on a small label: the lists with no day stay on
 * the board. With nothing dated at all it says that instead.
 */
@Composable
private fun ClosingLabel(datedCount: Int, undatedCount: Int) {
    val palette = LocalPaperPalette.current
    val words = when {
        datedCount == 0 -> stringResource(R.string.calendar_nothing_dated)
        undatedCount > 0 -> pluralStringResource(R.plurals.calendar_undated_stay, undatedCount, undatedCount)
        else -> return
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = words,
            modifier = Modifier.pinnedPaper().padding(horizontal = LABEL_PADDING, vertical = LABEL_PADDING / 2),
            style = PaperType.note,
            color = palette.inked(InkTone.Words),
            textAlign = TextAlign.Center
        )
    }
}

internal fun marksIn(agenda: List<TodoListSummary>, month: YearMonth): Map<Int, InkTone> {
    val marks = HashMap<Int, CardDay>()
    agenda.forEach { summary ->
        val date = summary.list.targetDate ?: summary.list.dueDate ?: return@forEach
        val day = cardDay(summary) ?: return@forEach
        if (YearMonth.from(date) != month) return@forEach
        val held = marks[date.dayOfMonth]
        if (held == null || day.ordinal < held.ordinal) marks[date.dayOfMonth] = day
    }
    return marks.mapValues { it.value.tone }
}
