package fr.mandarine.todolist.ui.paper

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayNameStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WEEK_ROWS = 6
private const val WEEK_DAYS = 7
private const val YEAR_COLUMNS = 4
private const val YEAR_REACH = 100
private const val MONTHS_IN_YEAR = 12
private const val ONE_MONTH = 1
private const val NO_PICK = 0
private const val FIRST_PAGE = 0
private const val ONE_LINE = 1
private const val WEEKDAY_BAND = 0.55f
private const val RING_SPREAD = 1.55f
private const val RING_FIT = 0.9f
private const val MONTH_SKELETON = "MMMM y"
private const val SPOKEN_DAY_SKELETON = "EEEEdMMMMy"
private const val DAY_RING_SEED = 0x3C7B
private const val INK_DWELL_MILLIS = 200L
private val TODAY_DOT = 2.dp
private val TODAY_DOT_DROP = 2.dp
private val WEEKDAY_LIFT = 3.dp

/**
 * A month written on the same ruled sheet as everything else: seven pencil
 * initials over the columns, the day numbers sitting on the page's own rules one
 * pitch per week, the chosen day circled in a hand-drawn ink ring that draws
 * itself as it is picked, today marked with a pencil dot and every day already
 * gone written in pencil. Months turn with a swipe or with the two ink glyphs
 * either side of the header; the header itself opens the years on the same rules.
 */
@Composable
fun PaperCalendar(
    selected: LocalDate?,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val locale = formatLocale
    val pitch = LocalPagePitch.current
    val cellHeight = maxOf(pitch, PaperDimens.touchTarget)
    val haptics = rememberPaperHaptics()
    val scope = rememberCoroutineScope()
    val opened = remember(selected, today) { YearMonth.from(selected ?: today) }
    val firstMonth = remember(opened) { YearMonth.of(opened.year - YEAR_REACH, Month.JANUARY) }
    val pager = rememberPagerState(initialPage = pageOf(firstMonth, opened)) {
        (YEAR_REACH * 2 + 1) * MONTHS_IN_YEAR
    }
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val initials = remember(locale, firstDayOfWeek) { weekdayInitials(firstDayOfWeek, locale) }
    val spokenDay = rememberDatePattern(SPOKEN_DAY_SKELETON, locale)

    /**
     * Keyed on the day handed in, not remembered once. A reader only ever changes
     * this by pressing a day here, and the sheet closes on that — so for them the
     * key never fires. The demonstration circles a day from outside, and without
     * the key its calendar showed nothing chosen at all.
     */
    var inked by remember(selected) { mutableStateOf(selected) }
    var picks by remember { mutableIntStateOf(NO_PICK) }
    var yearsOpen by remember { mutableStateOf(false) }
    val pick = rememberUpdatedState(onPick)

    LaunchedEffect(picks) {
        if (picks == NO_PICK) return@LaunchedEffect
        val day = inked ?: return@LaunchedEffect
        if (animated) delay(INK_DWELL_MILLIS)
        pick.value(day)
    }

    val choose: (LocalDate) -> Unit = { day ->
        inked = day
        picks++
        haptics.tick()
    }

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        CalendarHeader(
            pager = pager,
            firstMonth = firstMonth,
            locale = locale,
            yearsOpen = yearsOpen,
            onToggleYears = { yearsOpen = !yearsOpen }
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val glyph = with(LocalDensity.current) {
                LocalRuledHand.current.itemLine.fontSize.toDp()
            }
            val ring = minOf(glyph * RING_SPREAD, maxWidth / WEEK_DAYS * RING_FIT)
            val band = pitch * WEEKDAY_BAND
            if (yearsOpen) {
                val shown = firstMonth.plusMonths(pager.targetPage.toLong())
                YearGrid(
                    firstMonth = firstMonth,
                    shown = shown,
                    today = today,
                    height = band + cellHeight * WEEK_ROWS,
                    onChooseYear = { year ->
                        yearsOpen = false
                        val page = pageOf(firstMonth, shown.withYear(year))
                        scope.launch { pager.scrollToPage(page) }
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    WeekdayInitials(initials = initials, band = band)
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.height(cellHeight * WEEK_ROWS)
                    ) { page ->
                        MonthGrid(
                            month = firstMonth.plusMonths(page.toLong()),
                            firstDayOfWeek = firstDayOfWeek,
                            today = today,
                            inked = inked,
                            spokenDay = spokenDay,
                            ring = ring,
                            glyph = glyph,
                            cellHeight = cellHeight,
                            animated = animated,
                            onChoose = choose
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    pager: PagerState,
    firstMonth: YearMonth,
    locale: Locale,
    yearsOpen: Boolean,
    onToggleYears: () -> Unit
) {
    val palette = LocalPaperPalette.current
    val scope = rememberCoroutineScope()
    val monthPattern = rememberDatePattern(MONTH_SKELETON, locale)
    val shown = firstMonth.plusMonths(pager.targetPage.toLong())
    val written = remember(shown, monthPattern) { shown.format(monthPattern) }
    val back = stringResource(R.string.previous_month)
    val forward = stringResource(R.string.next_month)
    val turn: (Int) -> Unit = { step ->
        scope.launch { pager.animateScrollToPage(pager.targetPage + step) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxOf(LocalPagePitch.current, PaperDimens.touchTarget))
            .ruleUnder(palette.rule),
        verticalAlignment = Alignment.Top
    ) {
        TurnGlyph(
            shown = !yearsOpen,
            description = back,
            glyph = R.drawable.ic_chevron_left,
            enabled = pager.targetPage > FIRST_PAGE,
            onClick = { turn(-ONE_MONTH) }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(maxOf(LocalPagePitch.current, PaperDimens.touchTarget))
                .clickable(
                    onClickLabel = stringResource(
                        if (yearsOpen) R.string.choose_month else R.string.choose_year
                    ),
                    role = Role.Button,
                    onClick = onToggleYears
                )
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(back) { turn(-ONE_MONTH); true },
                        CustomAccessibilityAction(forward) { turn(ONE_MONTH); true }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = written,
                modifier = Modifier.seatOnRule(),
                style = LocalRuledHand.current.listLine,
                color = palette.inked(InkTone.Words),
                maxLines = ONE_LINE
            )
        }
        TurnGlyph(
            shown = !yearsOpen,
            description = forward,
            glyph = R.drawable.ic_chevron_right,
            enabled = pager.targetPage < pager.pageCount - ONE_MONTH,
            onClick = { turn(ONE_MONTH) }
        )
    }
}

/**
 * The glyphs are put away while the years are open rather than swapped out from
 * under the finger: they fade and give their width back to the month, and the
 * header keeps the same height either way.
 */
@Composable
private fun RowScope.TurnGlyph(
    shown: Boolean,
    description: String,
    glyph: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(PaperMotion.rowEnter) + expandHorizontally(PaperMotion.rowUnfold),
        exit = fadeOut(PaperMotion.rowExit) + shrinkHorizontally(PaperMotion.rowFold)
    ) {
        InkIconButton(
            painter = painterResource(glyph),
            contentDescription = description,
            onClick = onClick,
            tint = LocalPaperPalette.current.inkSoft,
            enabled = enabled,
            seat = IconSeat.OnRule,
            foot = GlyphFoot.chevron
        )
    }
}

@Composable
private fun WeekdayInitials(initials: List<String>, band: Dp) {
    val palette = LocalPaperPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(band)
            .ruleUnder(palette.rule),
        verticalAlignment = Alignment.Bottom
    ) {
        initials.forEach { initial ->
            Text(
                text = initial,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = WEEKDAY_LIFT),
                style = PaperType.caption,
                color = palette.inked(InkTone.Margin),
                textAlign = TextAlign.Center,
                maxLines = ONE_LINE
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    inked: LocalDate?,
    spokenDay: DateTimeFormatter,
    ring: Dp,
    glyph: Dp,
    cellHeight: Dp,
    animated: Boolean,
    onChoose: (LocalDate) -> Unit
) {
    val palette = LocalPaperPalette.current
    val lead = leadingBlanks(month, firstDayOfWeek)
    val length = month.lengthOfMonth()
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(WEEK_ROWS) { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight)
                    .ruleUnder(palette.rule),
                verticalAlignment = Alignment.Top
            ) {
                repeat(WEEK_DAYS) { column ->
                    val dayOfMonth = week * WEEK_DAYS + column - lead + 1
                    if (dayOfMonth in 1..length) {
                        val date = month.atDay(dayOfMonth)
                        DayCell(
                            date = date,
                            selected = date == inked,
                            isToday = date == today,
                            past = date.isBefore(today),
                            spoken = remember(date, spokenDay) { date.format(spokenDay) },
                            ring = ring,
                            glyph = glyph,
                            cellHeight = cellHeight,
                            animated = animated,
                            onChoose = onChoose
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    past: Boolean,
    spoken: String,
    ring: Dp,
    glyph: Dp,
    cellHeight: Dp,
    animated: Boolean,
    onChoose: (LocalDate) -> Unit
) {
    val palette = LocalPaperPalette.current
    val pencil = palette.inked(InkTone.Margin)
    val pitchPx = with(LocalDensity.current) { LocalPagePitch.current.toPx() }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(cellHeight)
            .selectable(
                selected = selected,
                role = Role.Button,
                onClick = { onChoose(date) }
            )
            .semantics { contentDescription = spoken }
            .drawBehind { if (isToday) drawTodayDot(pencil, pitchPx) },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = LocalPagePitch.current - glyph / 2 - ring / 2)
                .size(ring)
                .circledInInk(
                    circled = selected,
                    seed = ringSeed(date),
                    color = palette.inked(InkTone.Acted),
                    animated = animated
                )
        )
        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier.seatOnRule(),
            style = LocalRuledHand.current.itemLine,
            color = palette.inked(if (past) InkTone.Margin else InkTone.Words),
            maxLines = ONE_LINE
        )
    }
}

/**
 * The years are written on the same rules the days are, so the sheet keeps its
 * size when the header swaps one for the other: the block the six weeks and their
 * initials occupied is exactly the block the years scroll inside.
 */
@Composable
private fun YearGrid(
    firstMonth: YearMonth,
    shown: YearMonth,
    today: LocalDate,
    height: Dp,
    onChooseYear: (Int) -> Unit
) {
    val palette = LocalPaperPalette.current
    val cellHeight = maxOf(LocalPagePitch.current, PaperDimens.touchTarget)
    val firstYear = firstMonth.year
    val years = YEAR_REACH * 2 + 1
    val state = rememberLazyGridState(
        initialFirstVisibleItemIndex = (shown.year - firstYear - YEAR_COLUMNS).coerceAtLeast(0)
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(YEAR_COLUMNS),
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        items(count = years) { index ->
            val year = firstYear + index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight)
                    .ruleUnder(palette.rule)
                    .selectable(
                        selected = year == shown.year,
                        role = Role.Button,
                        onClick = { onChooseYear(year) }
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = year.toString(),
                    modifier = Modifier.seatOnRule(),
                    style = LocalRuledHand.current.itemLine,
                    color = when {
                        year == shown.year -> palette.inked(InkTone.Acted)
                        year < today.year -> palette.inked(InkTone.Margin)
                        else -> palette.inked(InkTone.Words)
                    },
                    maxLines = ONE_LINE
                )
            }
        }
    }
}

@Composable
private fun rememberDatePattern(skeleton: String, locale: Locale): DateTimeFormatter =
    remember(skeleton, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }

private fun Modifier.ruleUnder(color: Color): Modifier = drawBehind {
    val thickness = ruleThickness()
    drawRect(
        color = color,
        topLeft = Offset(0f, size.height - thickness),
        size = Size(size.width, thickness)
    )
}

/**
 * A day number sits at the foot of its own line, so the only blank paper in a
 * cell is above it. The dot marking today therefore rests just over the numeral
 * rather than under it, close enough to belong to it and clear of the rule the
 * row above is written on.
 */
/**
 * Under the numeral, not under the cell. The cell is a finger tall and the writing
 * is a rule tall, and those stopped being the same number when the page's line
 * pitch came off its row height — a dot seated on the cell fell a rule below the
 * day it marks.
 */
private fun DrawScope.drawTodayDot(color: Color, pitch: Float) {
    drawCircle(
        color = color,
        radius = TODAY_DOT.toPx(),
        center = Offset(size.width / 2f, pitch + TODAY_DOT_DROP.toPx())
    )
}

internal fun weekdayInitials(firstDayOfWeek: DayOfWeek, locale: Locale): List<String> =
    List(WEEK_DAYS) { step ->
        firstDayOfWeek.plus(step.toLong()).getDisplayName(DayNameStyle.NARROW, locale)
    }

internal fun leadingBlanks(month: YearMonth, firstDayOfWeek: DayOfWeek): Int =
    Math.floorMod(month.atDay(1).dayOfWeek.value - firstDayOfWeek.value, WEEK_DAYS)

internal fun pageOf(firstMonth: YearMonth, month: YearMonth): Int =
    ChronoUnit.MONTHS.between(firstMonth, month).toInt()

internal fun ringSeed(date: LocalDate): Int = date.toEpochDay().toInt() xor DAY_RING_SEED
