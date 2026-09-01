package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.listmeta.DateJot
import fr.mandarine.todolist.ui.listmeta.OpenCount
import fr.mandarine.todolist.ui.nav.travellingName
import fr.mandarine.todolist.ui.paper.InkBudget
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.RowVerb
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.SwipeMark
import fr.mandarine.todolist.ui.paper.SwipeReveal
import fr.mandarine.todolist.ui.paper.SwipeRow
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.penStrike
import fr.mandarine.todolist.ui.paper.rememberPenStrike
import fr.mandarine.todolist.ui.paper.rowVerbs
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.paper.spokenVerbs
import fr.mandarine.todolist.ui.paper.tearOff

private val NAME_END_GAP = 8.dp
private val CORNER_ROOM = 52.dp

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
    /**
     * Delete is not spoken here any more: the row carries a tear tab that says so
     * out loud and can be pressed. Naming it twice on the one row gave a screen
     * reader the same verb from two places.
     */
    val verbs = rowVerbs(
        onMoveUp?.let { RowVerb(stringResource(R.string.move_up), it) },
        onMoveDown?.let { RowVerb(stringResource(R.string.move_down), it) }
    )
    SwipeRow(
        key = summary.list.id,
        onDelete = onDeleteRequested,
        reveal = onRenameRequested?.let {
            SwipeReveal(SwipeMark.Pencil, stringResource(R.string.edit_list), it)
        },
        tearLabel = stringResource(R.string.delete_list),
        animated = animated,
        modifier = modifier.tearOff(tearing, animated, onTorn)
    ) {
        RuledRow(modifier = Modifier.spokenVerbs(verbs), onClick = onOpen) {
            RowName(summary = summary, animated = animated)
            Marginalia(summary = summary, animated = animated, onRewriteDate = onRewriteDate)
            Spacer(Modifier.width(CORNER_ROOM))
        }
    }
}

@Composable
private fun RowScope.RowName(summary: TodoListSummary, animated: Boolean) {
    val palette = LocalPaperPalette.current
    val style = MaterialTheme.typography.titleMedium
    val strike = rememberPenStrike(summary.list.id, summary.allDone, animated)
    val ink = palette.inked(InkBudget.words(summary.allDone))
    Text(
        text = remember(summary.list.name) { handwritten(summary.list.name) },
        modifier = Modifier
            .weight(1f)
            .padding(end = NAME_END_GAP)
            .seatOnRule()
            .penStrike(strike, ink)
            .travellingName(summary.list.id),
        style = style,
        color = ink,
        onTextLayout = strike::onTextLayout
    )
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
