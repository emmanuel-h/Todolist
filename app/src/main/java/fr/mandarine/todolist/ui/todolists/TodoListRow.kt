package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.listmeta.DateJot
import fr.mandarine.todolist.ui.listmeta.OpenCount
import fr.mandarine.todolist.ui.nav.travellingName
import fr.mandarine.todolist.ui.paper.GlyphFoot
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkBudget
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PenStrikeState
import fr.mandarine.todolist.ui.paper.RowVerb
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.highlightWash
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.penStrike
import fr.mandarine.todolist.ui.paper.rememberPenStrike
import fr.mandarine.todolist.ui.paper.rowVerbs
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.paper.spokenVerbs
import fr.mandarine.todolist.ui.paper.tearOff

private val NAME_END_GAP = 8.dp

@Composable
fun TodoListRow(
    summary: TodoListSummary,
    animated: Boolean,
    onOpen: () -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
    tearing: Boolean = false,
    onTorn: () -> Unit = {},
    onRenameRequested: (() -> Unit)? = null,
    onRewriteDate: ((DateSelection) -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val palette = LocalPaperPalette.current
    val verbs = rowVerbs(
        onMoveUp?.let { RowVerb(stringResource(R.string.move_up), it) },
        onMoveDown?.let { RowVerb(stringResource(R.string.move_down), it) }
    )
    val hasDate = summary.list.targetDate != null ||
        (summary.list.dueDate != null && summary.dueDateStatus != null)
    RuledRow(
        modifier = modifier.tearOff(tearing, animated, onTorn).spokenVerbs(verbs),
        onClick = onOpen
    ) {
        RowName(summary = summary, animated = animated)
        Marginalia(summary = summary, animated = animated, onRewriteDate = onRewriteDate)
        if (onRenameRequested != null) {
            InkIconButton(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.edit_list),
                onClick = onRenameRequested,
                tint = palette.inked(InkTone.Margin),
                pressedTint = palette.inked(InkTone.Words),
                seat = IconSeat.OnRule,
                foot = GlyphFoot.pencil
            )
        }
        if (!hasDate && onRewriteDate != null) {
            InkIconButton(
                painter = painterResource(R.drawable.ic_event),
                contentDescription = stringResource(R.string.give_list_a_day),
                onClick = { onRewriteDate(DateSelection(DateKind.TARGET, null)) },
                tint = palette.inked(InkTone.Margin),
                pressedTint = palette.inked(InkTone.Words),
                seat = IconSeat.OnRule,
                foot = GlyphFoot.calendar
            )
        }
        InkIconButton(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.delete_list),
            onClick = onDeleteRequested,
            tint = palette.inked(InkTone.Margin),
            pressedTint = palette.inked(InkTone.Words),
            seat = IconSeat.OnRule,
            foot = GlyphFoot.trash
        )
    }
}

@Composable
private fun RowScope.RowName(summary: TodoListSummary, animated: Boolean) {
    val palette = LocalPaperPalette.current
    val style = MaterialTheme.typography.titleMedium
    val strike = rememberPenStrike(summary.list.id, summary.allDone, animated)
    val ink = palette.inked(InkBudget.words(summary.allDone))
    val wash = palette.highlightWash(summary.list.colour)
    Text(
        text = remember(summary.list.name) { handwritten(summary.list.name) },
        modifier = Modifier
            .weight(1f)
            .padding(end = NAME_END_GAP)
            .seatOnRule()
            .nameWash(wash, strike)
            .penStrike(strike, ink)
            .travellingName(summary.list.id),
        style = style,
        color = ink,
        onTextLayout = strike::onTextLayout
    )
}

/**
 * A highlighter band behind the name: one filled rectangle per text line, each
 * ending where that line's last glyph ends rather than at the composable's full
 * width. Transparent for [ListColour.None] so no rectangle is drawn at all.
 *
 * Nothing is allocated inside the draw lambda: the layout is read in the cache
 * block so a change to it rebuilds the cache rather than allocating per frame.
 */
private fun Modifier.nameWash(wash: Color, state: PenStrikeState): Modifier {
    if (wash == Color.Transparent) return this
    return drawWithCache {
        val layout = state.layout
        onDrawBehind {
            if (layout == null) return@onDrawBehind
            for (i in 0 until layout.lineCount) {
                val top = layout.getLineTop(i)
                val bottom = layout.getLineBottom(i)
                val right = layout.getLineRight(i).coerceAtMost(size.width)
                if (right <= 0f) continue
                drawRect(
                    color = wash,
                    topLeft = Offset(0f, top),
                    size = Size(right, bottom - top)
                )
            }
        }
    }
}

@Composable
private fun Marginalia(
    summary: TodoListSummary,
    animated: Boolean,
    onRewriteDate: ((DateSelection) -> Unit)?
) {
    val palette = LocalPaperPalette.current
    val targetDate = summary.list.targetDate
    val dueDate = summary.list.dueDate
    val dueStatus = summary.dueDateStatus
    if (targetDate != null) {
        DateJot(
            date = targetDate,
            kind = DateKind.TARGET,
            showYear = summary.showTargetYear,
            tint = palette.inked(targetTone(summary.isTargetDateElapsed)),
            struck = summary.isTargetDateElapsed,
            onRewrite = onRewriteDate
        )
    }
    if (dueDate != null && dueStatus != null) {
        DateJot(
            date = dueDate,
            kind = DateKind.DUE,
            showYear = summary.showDueDateYear,
            tint = palette.inked(dueTone(dueStatus)),
            onRewrite = onRewriteDate
        )
    }
    OpenCount(count = summary.activeCount, animated = animated)
}

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
