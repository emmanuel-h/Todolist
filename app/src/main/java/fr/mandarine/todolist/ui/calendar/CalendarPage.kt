package fr.mandarine.todolist.ui.calendar

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private const val WEEK_DAYS = 7
private const val MONTH_SKELETON = "LLLL"
private const val MONTH_YEAR_SKELETON = "LLLLy"
private const val DAY_SKELETON = "dMMMM"
private const val DAY_YEAR_SKELETON = "dMMMMy"
private const val SPAN_DAY_SKELETON = "dMMM"
private const val WEEKDAY_SKELETON = "EEEE"
private const val SHORT_WEEKDAY_SKELETON = "EEE"
private const val SPOKEN_DAY_SKELETON = "EEEEdMMMMy"
private const val SPAN_DASH = " – "
private const val SHEET_TILT_DEGREES = -0.4f
private const val SLIP_TILT_DEGREES = 0.6f
private const val UNDERLINE_SHARE = 0.36f
private val PAGE_WIDTH = 560.dp
private val PAGE_TOP = 12.dp
private val PAGE_FOOT = 24.dp
private val PAGE_SIDE = 16.dp
private val SHEET_PADDING = 14.dp
private val DAY_CELL = 48.dp
private val TODAY_DISC = 32.dp
private val SELECTED_RING = 2.dp
private val DAY_MARK = 6.dp
private val MARK_GAP = 1.dp
private val VIEW_TAB_HEIGHT = 48.dp
private val UNDERLINE_STROKE = 2.dp
private val UNDERLINE_HEIGHT = 6.dp
private val WEEK_ROW_HEIGHT = 56.dp
private val WEEK_WHEN_WIDTH = 56.dp
private val CHIP_GAP = 6.dp
private val CHIP_HORIZONTAL = 8.dp
private val CHIP_DOT = 7.dp
private val SLIP_START = 16.dp
private val SLIP_VERTICAL = 12.dp
private val SLIP_GAP = 12.dp
private val TAPE_INSET = 20.dp
private val LABEL_PADDING = 10.dp
private val HEADING_START = 4.dp

/**
 * The Calendar tab: the dated lists, and only those, laid out the way a wall
 * calendar and the slips pinned under it would be. The sheet at the top shows a
 * month, a week or a day, and every day on it can be pressed to open that day.
 * Most lists have no day, and the page says so in so many words, so nobody takes
 * the calendar for the place everything lives.
 */
@Composable
fun CalendarPage(
    state: TodoListsState,
    today: LocalDate,
    view: CalendarView,
    month: YearMonth?,
    day: LocalDate?,
    onViewChosen: (CalendarView) -> Unit,
    onMonthTurned: (YearMonth) -> Unit,
    onDayTurned: (LocalDate) -> Unit,
    onDayOpened: (LocalDate) -> Unit,
    onOpenList: (TodoList) -> Unit,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val content = state as? TodoListsState.Content
    val agenda = content?.agenda.orEmpty()
    val undated = content?.undatedCount ?: 0
    val shownMonth = month ?: YearMonth.from(today)
    val shownDay = day ?: today
    val locale = formatLocale
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val marks = remember(agenda) { marksOn(agenda) }
    val bars = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical).asPaddingValues()
    val slips = when (view) {
        CalendarView.Month -> remember(agenda, shownMonth, today) { agendaFor(agenda, shownMonth, today) }
        CalendarView.Week -> emptyList()
        CalendarView.Day -> remember(agenda, shownDay) { listsOn(agenda, shownDay) }
    }
    val stillLate = if (view == CalendarView.Day && shownDay == today) {
        remember(agenda, today) { agenda.filter { dayOf(it)?.isBefore(today) == true } }
    } else {
        emptyList()
    }
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
            item(key = "sheet") {
                when (view) {
                    CalendarView.Month -> MonthSheet(
                        month = shownMonth,
                        today = today,
                        firstDay = firstDay,
                        marks = marks,
                        onTurn = onMonthTurned,
                        onViewChosen = onViewChosen,
                        onDayOpened = onDayOpened
                    )
                    CalendarView.Week -> WeekSheet(
                        days = remember(shownDay, firstDay) { weekOf(shownDay, firstDay) },
                        today = today,
                        agenda = agenda,
                        onTurn = onDayTurned,
                        onViewChosen = onViewChosen,
                        onDayOpened = onDayOpened,
                        onOpenList = onOpenList
                    )
                    CalendarView.Day -> DaySheet(
                        day = shownDay,
                        today = today,
                        firstDay = firstDay,
                        marks = marks,
                        onTurn = onDayTurned,
                        onViewChosen = onViewChosen
                    )
                }
            }
            items(slips, key = { it.list.id }) { summary ->
                AgendaSlip(
                    summary = summary,
                    onOpen = { onOpenList(summary.list) },
                    modifier = if (animated) Modifier.animateItem() else Modifier
                )
            }
            if (view == CalendarView.Day && slips.isEmpty() && content != null) {
                item(key = "nothing") { PinnedLabel(stringResource(R.string.calendar_nothing_on_day)) }
            }
            if (stillLate.isNotEmpty()) {
                item(key = "still-late") { StillLateHeading() }
                items(stillLate, key = { it.list.id }) { summary ->
                    AgendaSlip(
                        summary = summary,
                        onOpen = { onOpenList(summary.list) },
                        modifier = if (animated) Modifier.animateItem() else Modifier
                    )
                }
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
 * The sheet pinned at the top, whatever it is showing: a line in small print over
 * a heading, the two arrows that turn it, the three views to choose between, and
 * then the days themselves.
 */
@Composable
private fun CalendarSheet(
    kicker: String,
    title: String,
    previous: String,
    next: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    view: CalendarView,
    onViewChosen: (CalendarView) -> Unit,
    days: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalPaperPalette.current
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
                    text = kicker,
                    style = PaperType.kicker,
                    color = palette.inked(InkTone.Margin)
                )
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = PaperType.masthead,
                    color = palette.inked(InkTone.Words),
                    maxLines = 1
                )
            }
            InkIconButton(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = previous,
                onClick = onPrevious,
                tint = palette.inked(InkTone.Words)
            )
            InkIconButton(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = next,
                onClick = onNext,
                tint = palette.inked(InkTone.Words)
            )
        }
        ViewTabs(selected = view, onSelect = onViewChosen)
        days()
    }
}

/**
 * Day, Week, Month, in words between two rules. The open one is in ink, a little
 * heavier and underlined by hand, the way the bar at the foot marks its open tab.
 */
@Composable
private fun ViewTabs(selected: CalendarView, onSelect: (CalendarView) -> Unit) {
    val palette = LocalPaperPalette.current
    val rule = palette.rule
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SHEET_PADDING / 2)
            .selectableGroup()
            .drawWithCache {
                val stroke = Dp.Hairline.toPx().coerceAtLeast(1f)
                onDrawBehind {
                    drawLine(rule, Offset.Zero, Offset(size.width, 0f), stroke)
                    drawLine(rule, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                }
            }
    ) {
        CalendarView.entries.forEach { view ->
            ViewTab(
                label = stringResource(view.label),
                selected = view == selected,
                onClick = { onSelect(view) }
            )
        }
    }
}

@Composable
private fun RowScope.ViewTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPaperPalette.current
    val ink = palette.inked(if (selected) InkTone.Words else InkTone.Margin)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(VIEW_TAB_HEIGHT)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = PaperType.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = ink,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(UNDERLINE_SHARE)
                .height(UNDERLINE_HEIGHT)
                .drawWithCache {
                    val stroke = Stroke(width = UNDERLINE_STROKE.toPx(), cap = StrokeCap.Round)
                    val path = Path().apply {
                        moveTo(stroke.width, size.height * 0.7f)
                        cubicTo(
                            size.width * 0.3f, size.height * 0.15f,
                            size.width * 0.65f, size.height * 0.2f,
                            size.width - stroke.width, size.height * 0.45f
                        )
                    }
                    onDrawBehind { if (selected) drawPath(path, ink, style = stroke) }
                }
        )
    }
}

/**
 * The month: the year in small print, the month's name, the weekday initials and
 * the days. Today is filled in ink; a day a list is about carries a dot in the ink
 * that list's day is written in. Pressing a day opens it.
 */
@Composable
private fun MonthSheet(
    month: YearMonth,
    today: LocalDate,
    firstDay: DayOfWeek,
    marks: Map<LocalDate, InkTone>,
    onTurn: (YearMonth) -> Unit,
    onViewChosen: (CalendarView) -> Unit,
    onDayOpened: (LocalDate) -> Unit
) {
    val locale = formatLocale
    val name = remember(month, locale) { formatted(month.atDay(1), MONTH_SKELETON, locale) }
    val blanks = leadingBlanks(month, firstDay)
    CalendarSheet(
        kicker = month.year.toString(),
        title = name,
        previous = stringResource(R.string.previous_month),
        next = stringResource(R.string.next_month),
        onPrevious = { onTurn(month.minusMonths(1)) },
        onNext = { onTurn(month.plusMonths(1)) },
        view = CalendarView.Month,
        onViewChosen = onViewChosen
    ) {
        WeekdayInitials(firstDay)
        val cells = blanks + month.lengthOfMonth()
        val weeks = (cells + WEEK_DAYS - 1) / WEEK_DAYS
        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(WEEK_DAYS) { column ->
                    val number = week * WEEK_DAYS + column - blanks + 1
                    Box(modifier = Modifier.weight(1f).height(DAY_CELL), contentAlignment = Alignment.Center) {
                        if (number in 1..month.lengthOfMonth()) {
                            val date = month.atDay(number)
                            DayCell(
                                date = date,
                                today = today,
                                mark = marks[date],
                                selected = false,
                                onPress = { onDayOpened(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The week, written down the sheet a ruled line to a day: the day on the left, and
 * on its line every list it is about as a scrap of that list's tape in the reader's
 * hand. Pressing a day opens it; pressing a scrap opens its list.
 */
@Composable
private fun WeekSheet(
    days: List<LocalDate>,
    today: LocalDate,
    agenda: List<TodoListSummary>,
    onTurn: (LocalDate) -> Unit,
    onViewChosen: (CalendarView) -> Unit,
    onDayOpened: (LocalDate) -> Unit,
    onOpenList: (TodoList) -> Unit
) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    val first = days.first()
    val last = days.last()
    val sameMonth = YearMonth.from(first) == YearMonth.from(last)
    val kicker = remember(first, last, locale) {
        if (sameMonth) {
            formatted(first, MONTH_YEAR_SKELETON, locale)
        } else if (first.year == last.year) {
            first.year.toString()
        } else {
            first.year.toString() + SPAN_DASH + last.year
        }
    }
    val heading = remember(first, last, locale) {
        if (sameMonth) {
            first.dayOfMonth.toString() + SPAN_DASH + last.dayOfMonth
        } else {
            formatted(first, SPAN_DAY_SKELETON, locale) + SPAN_DASH + formatted(last, SPAN_DAY_SKELETON, locale)
        }
    }
    CalendarSheet(
        kicker = kicker,
        title = heading,
        previous = stringResource(R.string.previous_week),
        next = stringResource(R.string.next_week),
        onPrevious = { onTurn(first.minusWeeks(1)) },
        onNext = { onTurn(first.plusWeeks(1)) },
        view = CalendarView.Week,
        onViewChosen = onViewChosen
    ) {
        days.forEach { date ->
            WeekRow(
                date = date,
                today = today,
                lists = remember(agenda, date) { listsOn(agenda, date) },
                onPress = { onDayOpened(date) },
                onOpenList = onOpenList
            )
            HorizontalRule(palette.rule)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekRow(
    date: LocalDate,
    today: LocalDate,
    lists: List<TodoListSummary>,
    onPress: () -> Unit,
    onOpenList: (TodoList) -> Unit
) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    val spoken = remember(date, locale) { formatted(date, SPOKEN_DAY_SKELETON, locale) }
    val weekday = remember(date, locale) { formatted(date, SHORT_WEEKDAY_SKELETON, locale) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WEEK_ROW_HEIGHT)
            .clickable(onClick = onPress),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(WEEK_WHEN_WIDTH)
                .clearAndSetSemantics { contentDescription = spoken },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = weekday, style = PaperType.kicker, color = palette.inked(InkTone.Margin), maxLines = 1)
            DayNumeral(date = date, today = today, mark = null, selected = false)
        }
        FlowRow(
            modifier = Modifier.weight(1f).padding(vertical = CHIP_GAP),
            horizontalArrangement = Arrangement.spacedBy(CHIP_GAP),
            verticalArrangement = Arrangement.spacedBy(CHIP_GAP)
        ) {
            lists.forEach { summary -> ListScrap(summary = summary, onOpen = { onOpenList(summary.list) }) }
        }
    }
}

/** One list on a week's line: a scrap of its tape with its name on it in the reader's hand. */
@Composable
private fun ListScrap(summary: TodoListSummary, onOpen: () -> Unit) {
    val palette = LocalPaperPalette.current
    val list = summary.list
    val dot = cardDay(summary)?.tone?.let { palette.inked(it) }
    Row(
        modifier = Modifier
            .background(palette.tapeFor(list.colour))
            .clickable(onClick = onOpen)
            .padding(horizontal = CHIP_HORIZONTAL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CHIP_GAP)
    ) {
        if (dot != null) Box(modifier = Modifier.size(CHIP_DOT).drawBehind { drawCircle(dot) })
        Text(
            text = remember(list.name) { handwritten(list.name) },
            style = PaperType.cardLine,
            color = palette.inked(InkTone.Words),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * One day: its weekday and date in print, and a strip of the week it falls in with
 * the day ringed in ink, so a neighbouring day is one press away. The arrows step a
 * day at a time.
 */
@Composable
private fun DaySheet(
    day: LocalDate,
    today: LocalDate,
    firstDay: DayOfWeek,
    marks: Map<LocalDate, InkTone>,
    onTurn: (LocalDate) -> Unit,
    onViewChosen: (CalendarView) -> Unit
) {
    val locale = formatLocale
    val weekday = remember(day, locale) { formatted(day, WEEKDAY_SKELETON, locale) }
    val kicker = if (day == today) {
        weekday + stringResource(R.string.summary_separator) + stringResource(R.string.card_today)
    } else {
        weekday
    }
    val heading = remember(day, today, locale) {
        formatted(day, if (day.year == today.year) DAY_SKELETON else DAY_YEAR_SKELETON, locale)
    }
    CalendarSheet(
        kicker = kicker,
        title = heading,
        previous = stringResource(R.string.previous_day),
        next = stringResource(R.string.next_day),
        onPrevious = { onTurn(day.minusDays(1)) },
        onNext = { onTurn(day.plusDays(1)) },
        view = CalendarView.Day,
        onViewChosen = onViewChosen
    ) {
        WeekdayInitials(firstDay)
        Row(modifier = Modifier.fillMaxWidth()) {
            remember(day, firstDay) { weekOf(day, firstDay) }.forEach { date ->
                Box(modifier = Modifier.weight(1f).height(DAY_CELL), contentAlignment = Alignment.Center) {
                    DayCell(
                        date = date,
                        today = today,
                        mark = marks[date],
                        selected = date == day,
                        onPress = { onTurn(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayInitials(firstDay: DayOfWeek) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    val initials = remember(firstDay, locale) { weekdayInitials(firstDay, locale) }
    Row(modifier = Modifier.fillMaxWidth()) {
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
}

/** A day that can be pressed, read out as its whole date. */
@Composable
private fun DayCell(date: LocalDate, today: LocalDate, mark: InkTone?, selected: Boolean, onPress: () -> Unit) {
    val locale = formatLocale
    val spoken = remember(date, locale) { formatted(date, SPOKEN_DAY_SKELETON, locale) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onPress)
            .clearAndSetSemantics {
                contentDescription = spoken
                this.selected = selected
                onClick { onPress(); true }
            },
        contentAlignment = Alignment.Center
    ) {
        DayNumeral(date = date, today = today, mark = mark, selected = selected)
    }
}

/**
 * The numeral of a day: in pencil once it has gone by, on an ink disc if it is
 * today, ringed in ink if it is the day open, with a dot under it in the loudest
 * ink of the lists it is about.
 */
@Composable
private fun DayNumeral(date: LocalDate, today: LocalDate, mark: InkTone?, selected: Boolean) {
    val palette = LocalPaperPalette.current
    val isToday = date == today
    val ink = palette.inked(InkTone.Words)
    val dot = mark?.let { palette.inked(it) }
    Box(
        modifier = Modifier
            .size(TODAY_DISC)
            .drawWithCache {
                val markRadius = DAY_MARK.toPx() / 2
                val ring = SELECTED_RING.toPx()
                onDrawBehind {
                    if (isToday) drawCircle(ink)
                    if (selected) {
                        drawCircle(ink, radius = size.minDimension / 2 + ring * 2, style = Stroke(width = ring))
                    }
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
                else -> ink
            }
        )
    }
}

@Composable
private fun HorizontalRule(colour: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colour)
    )
}

/** On today, what was missed follows what is due, so nothing late drops out of sight. */
@Composable
private fun StillLateHeading() {
    Text(
        text = stringResource(R.string.calendar_still_late),
        modifier = Modifier
            .padding(start = HEADING_START, top = SHEET_PADDING)
            .semantics { heading() },
        style = PaperType.kicker,
        color = LocalPaperPalette.current.inked(InkTone.Words)
    )
}

/**
 * One dated list, as a slip taped under the month: what its day is, its name in the
 * reader's hand, which kind of day it is in words, and how far through it they are.
 * A tap opens the list.
 */
@Composable
private fun AgendaSlip(summary: TodoListSummary, onOpen: () -> Unit, modifier: Modifier = Modifier) {
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
    Box(modifier = modifier.fillMaxWidth().graphicsLayer { rotationZ = SLIP_TILT_DEGREES }) {
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
    val words = when {
        datedCount == 0 -> stringResource(R.string.calendar_nothing_dated)
        undatedCount > 0 -> pluralStringResource(R.plurals.calendar_undated_stay, undatedCount, undatedCount)
        else -> return
    }
    PinnedLabel(words)
}

@Composable
private fun PinnedLabel(words: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = words,
            modifier = Modifier.pinnedPaper().padding(horizontal = LABEL_PADDING, vertical = LABEL_PADDING / 2),
            style = PaperType.note,
            color = LocalPaperPalette.current.inked(InkTone.Words),
            textAlign = TextAlign.Center
        )
    }
}

private val CalendarView.label: Int
    get() = when (this) {
        CalendarView.Day -> R.string.calendar_view_day
        CalendarView.Week -> R.string.calendar_view_week
        CalendarView.Month -> R.string.calendar_view_month
    }

private fun formatted(date: LocalDate, skeleton: String, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
        .replaceFirstChar { it.titlecase(locale) }

internal fun dayOf(summary: TodoListSummary): LocalDate? = summary.list.targetDate ?: summary.list.dueDate

/** The seven days of the week [day] falls in, starting on [firstDay]. */
internal fun weekOf(day: LocalDate, firstDay: DayOfWeek): List<LocalDate> {
    val start = day.minusDays(Math.floorMod(day.dayOfWeek.value - firstDay.value, WEEK_DAYS).toLong())
    return List(WEEK_DAYS) { start.plusDays(it.toLong()) }
}

internal fun listsOn(agenda: List<TodoListSummary>, day: LocalDate): List<TodoListSummary> =
    agenda.filter { dayOf(it) == day }

/** Under the month: what is dated in it, and anything still outstanding from before today. */
internal fun agendaFor(agenda: List<TodoListSummary>, month: YearMonth, today: LocalDate): List<TodoListSummary> =
    agenda.filter { summary ->
        val date = dayOf(summary)
        date != null && (YearMonth.from(date) == month || date.isBefore(today))
    }

internal fun marksOn(agenda: List<TodoListSummary>): Map<LocalDate, InkTone> {
    val marks = HashMap<LocalDate, CardDay>()
    agenda.forEach { summary ->
        val date = dayOf(summary) ?: return@forEach
        val day = cardDay(summary) ?: return@forEach
        val held = marks[date]
        if (held == null || day.ordinal < held.ordinal) marks[date] = day
    }
    return marks.mapValues { it.value.tone }
}
