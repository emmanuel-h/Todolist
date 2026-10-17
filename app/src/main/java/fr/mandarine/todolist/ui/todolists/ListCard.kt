package fr.mandarine.todolist.ui.todolists

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.ListHorizon
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.nav.travellingName
import fr.mandarine.todolist.ui.paper.CorkDimens
import fr.mandarine.todolist.ui.paper.GlyphFoot
import fr.mandarine.todolist.ui.paper.InkBudget
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperTape
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.RowVerb
import fr.mandarine.todolist.ui.paper.TearOffDay
import fr.mandarine.todolist.ui.paper.cardTilt
import fr.mandarine.todolist.ui.paper.formatLocale
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.penStrike
import fr.mandarine.todolist.ui.paper.pinnedPaper
import fr.mandarine.todolist.ui.paper.rememberPenStrike
import fr.mandarine.todolist.ui.paper.rowVerbs
import fr.mandarine.todolist.ui.paper.spokenVerbs
import fr.mandarine.todolist.ui.paper.tapeFor
import fr.mandarine.todolist.ui.paper.tearOff
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DAY_SKELETON = "EEEMMMd"
private const val DAY_YEAR_SKELETON = "EEEMMMdy"
private const val MONTH_SKELETON = "MMM"
private const val FINISHED_OPACITY = 0.78f
private val CARD_START = 14.dp
private val CARD_TOP = 18.dp
private val CARD_END = 10.dp
private val CARD_BOTTOM = 4.dp
private val NAME_CLEARS_DAY = 30.dp
private val STATUS_GAP = 2.dp
private val PREVIEW_TOP = 6.dp
private val PREVIEW_RING = 11.dp
private val PREVIEW_RING_STROKE = 1.5.dp
private val PREVIEW_RING_GAP = 8.dp
private val TAPE_INSET = 22.dp
private val FOOT_HEIGHT = 36.dp

/**
 * How a card's day stands, which decides the ink its status line and its torn-off
 * calendar page are written in. A missed due day is the one thing allowed to go red;
 * a target day that has gone by is spent, not missed, and is crossed out in pencil
 * like the jot it replaces.
 */
internal enum class CardDay(val tone: InkTone) {
    Late(InkTone.Lost),
    Today(InkTone.Today),
    Spent(InkTone.Crossed),
    Ahead(InkTone.Margin)
}

internal fun cardDay(summary: TodoListSummary): CardDay? {
    val list = summary.list
    return when {
        list.dueDate != null && summary.dueDateStatus == null -> null
        list.dueDate != null && summary.dueDateStatus == DueDateStatus.OVERDUE -> CardDay.Late
        list.targetDate != null && summary.isTargetDateElapsed -> CardDay.Spent
        list.targetDate == null && list.dueDate == null -> null
        summary.horizon == ListHorizon.TODAY -> CardDay.Today
        else -> CardDay.Ahead
    }
}

/**
 * One list, pinned to the board as a card: taped up in the reader's colour for it,
 * its name in their hand, one printed line saying how it stands, and the first few
 * things still to do on it — enough to know what is on a list without opening it.
 *
 * A list with a day carries the day as a page torn off a calendar and clipped to the
 * card's corner; most lists have none, and their cards are simply quieter for it.
 * The one control that acts on the list sits at the card's foot, where the thumb
 * finds it, and everything else about the card opens the list.
 */
@Composable
fun ListCard(
    summary: TodoListSummary,
    animated: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    tearing: Boolean = false,
    onTorn: () -> Unit = {},
    onActionsRequested: (() -> Unit)? = null,
    onRewriteDate: ((DateSelection) -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val palette = LocalPaperPalette.current
    val list = summary.list
    val day = cardDay(summary)
    val tilt = remember(list.id) { cardTilt(list.id) }
    val verbs = rowVerbs(
        onMoveUp?.let { RowVerb(stringResource(R.string.move_up), it) },
        onMoveDown?.let { RowVerb(stringResource(R.string.move_down), it) }
    )
    Box(
        modifier = modifier
            .tearOff(tearing, animated, onTorn)
            .graphicsLayer { rotationZ = tilt }
    ) {
        Column(
            modifier = Modifier
                .padding(top = CorkDimens.tapeOverlap)
                .fillMaxWidth()
                .alpha(if (summary.allDone) FINISHED_OPACITY else 1f)
                .pinnedPaper(tone = if (summary.allDone) palette.paperShade else palette.paperSheet)
                .spokenVerbs(verbs)
                .clickable(onClick = onOpen)
                .padding(start = CARD_START, top = CARD_TOP, end = CARD_END, bottom = CARD_BOTTOM)
        ) {
            CardName(
                summary = summary,
                animated = animated,
                modifier = Modifier.padding(end = if (day != null) NAME_CLEARS_DAY else 0.dp)
            )
            CardStatus(summary = summary, day = day)
            if (!summary.allDone) CardPreview(summary)
            CardFoot(summary = summary, onActionsRequested = onActionsRequested)
        }
        PaperTape(
            colour = palette.tapeFor(list.colour),
            modifier = Modifier.padding(start = TAPE_INSET)
        )
        if (day != null) {
            CardDayPage(
                summary = summary,
                day = day,
                onRewriteDate = onRewriteDate,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = CorkDimens.dayOverhang / 2, y = -CorkDimens.dayOverhang / 2)
            )
        }
    }
}

@Composable
private fun CardName(summary: TodoListSummary, animated: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPaperPalette.current
    val strike = rememberPenStrike(summary.list.id, summary.allDone, animated)
    val ink = palette.inked(InkBudget.words(summary.allDone))
    Text(
        text = remember(summary.list.name) { handwritten(summary.list.name) },
        modifier = modifier
            .fillMaxWidth()
            .penStrike(strike, ink)
            .travellingName(summary.list.id),
        style = PaperType.listLine,
        color = ink,
        onTextLayout = strike::onTextLayout
    )
}

/**
 * One printed line: where the list's day stands, then how far through it the
 * reader is. A list with neither says nothing, rather than printing a zero.
 */
@Composable
private fun CardStatus(summary: TodoListSummary, day: CardDay?) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    val total = summary.activeCount + summary.completedCount
    val fraction = if (total > 0) {
        stringResource(R.string.progress_fraction, summary.completedCount, total)
    } else {
        null
    }
    val spokenFraction = pluralStringResource(R.plurals.progress_done, total, summary.completedCount, total)
    val date = summary.list.targetDate ?: summary.list.dueDate
    val showYear = if (summary.list.targetDate != null) summary.showTargetYear else summary.showDueDateYear
    val dayPhrase = when {
        day == null -> null
        day == CardDay.Late && summary.daysLate > 0 ->
            pluralStringResource(R.plurals.card_days_late, summary.daysLate, summary.daysLate)
        day == CardDay.Today -> stringResource(R.string.card_today)
        else -> date?.let { cardDate(it, showYear, locale).uppercase(locale) }
    }
    if (dayPhrase == null && fraction == null) return
    Row(
        modifier = Modifier.padding(top = STATUS_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dayPhrase != null) {
            Text(
                text = dayPhrase,
                style = PaperType.kicker,
                color = palette.inked(day?.tone ?: InkTone.Margin),
                textDecoration = if (day == CardDay.Spent) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (dayPhrase != null && fraction != null) {
            Text(
                text = stringResource(R.string.summary_separator),
                style = PaperType.kicker,
                color = palette.inked(InkTone.Margin)
            )
        }
        if (fraction != null) {
            Text(
                text = fraction,
                modifier = Modifier.semantics { contentDescription = spokenFraction },
                style = PaperType.kicker,
                color = palette.inked(InkTone.Margin),
                maxLines = 1
            )
        }
    }
}

/**
 * The first few things still to do, each after a small pencilled ring. The rings
 * are drawn, not offered: a card is for reading at a glance, and ticking happens
 * on the list itself.
 */
@Composable
private fun CardPreview(summary: TodoListSummary) {
    val palette = LocalPaperPalette.current
    if (summary.preview.isEmpty()) return
    val ring = palette.inked(InkTone.Margin)
    Column(
        modifier = Modifier.padding(top = PREVIEW_TOP),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        summary.preview.forEach { title ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(PREVIEW_RING)
                        .drawWithCache {
                            val stroke = Stroke(width = PREVIEW_RING_STROKE.toPx())
                            val radius = (size.minDimension - stroke.width) / 2f
                            onDrawBehind { drawCircle(ring, radius = radius, style = stroke) }
                        }
                )
                Spacer(Modifier.width(PREVIEW_RING_GAP))
                Text(
                    text = remember(title) { handwritten(title) },
                    style = PaperType.cardLine,
                    color = palette.inked(InkTone.Words),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * The foot of the card: how much more the list holds than the card can show, and
 * the ⋯ that holds out everything else that can be done to it.
 */
@Composable
private fun CardFoot(summary: TodoListSummary, onActionsRequested: (() -> Unit)?) {
    val palette = LocalPaperPalette.current
    val more = if (summary.allDone) 0 else summary.activeCount - summary.preview.size
    Row(
        modifier = Modifier.fillMaxWidth().height(FOOT_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (more > 0) {
            Text(
                text = pluralStringResource(R.plurals.card_more_items, more, more),
                style = PaperType.note,
                color = palette.inked(InkTone.Margin),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (onActionsRequested != null) {
            InkIconButton(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.list_actions),
                onClick = onActionsRequested,
                tint = palette.inked(InkTone.Margin),
                pressedTint = palette.inked(InkTone.Words),
                foot = GlyphFoot.pencil,
                glyphSize = PaperDimens.rowGlyph,
                buttonWidth = PaperDimens.touchTarget
            )
        }
    }
}

@Composable
private fun CardDayPage(
    summary: TodoListSummary,
    day: CardDay,
    onRewriteDate: ((DateSelection) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val list = summary.list
    val date = list.targetDate ?: list.dueDate ?: return
    val kind = if (list.targetDate != null) DateKind.TARGET else DateKind.DUE
    val locale = formatLocale
    val month = remember(date, locale) { monthOf(date, locale) }
    val spokenDate = remember(date, locale) { formatListDate(date, showYear = true, locale = locale) }
    val spoken = stringResource(
        when (kind) {
            DateKind.TARGET -> R.string.target_date_description
            DateKind.DUE -> R.string.due_date_description
        },
        spokenDate
    )
    val pressLabel = stringResource(
        when (kind) {
            DateKind.TARGET -> R.string.set_target_date
            DateKind.DUE -> R.string.set_due_date
        }
    )
    TearOffDay(
        month = month,
        day = date.dayOfMonth.toString(),
        tone = day.tone,
        spoken = spoken,
        struck = day == CardDay.Spent,
        pressLabel = pressLabel,
        onPress = onRewriteDate?.let { write -> { write(DateSelection(kind, date)) } },
        modifier = modifier
    )
}

internal fun cardDate(date: LocalDate, showYear: Boolean, locale: Locale): String =
    date.format(
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(locale, if (showYear) DAY_YEAR_SKELETON else DAY_SKELETON),
            locale
        )
    )

private fun monthOf(date: LocalDate, locale: Locale): String =
    date.format(
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, MONTH_SKELETON), locale)
    ).trimEnd('.').uppercase(locale)

/**
 * A date the reader set for themselves is marginalia until the day is behind them,
 * and then it is spent like anything else that is done. A due date is the one jot
 * allowed to raise its voice: amber the day it falls, red once it is missed.
 *
 * Spent is carried by the strike through the date rather than by this tone alone:
 * the two greys sit a twentieth of a step apart, which is a distinction only the
 * code can see.
 */
internal fun targetTone(elapsed: Boolean): InkTone =
    if (elapsed) InkTone.Crossed else InkTone.Margin

internal fun dueTone(status: DueDateStatus): InkTone = when (status) {
    DueDateStatus.FUTURE -> InkTone.Margin
    DueDateStatus.TODAY -> InkTone.Today
    DueDateStatus.OVERDUE -> InkTone.Lost
}
