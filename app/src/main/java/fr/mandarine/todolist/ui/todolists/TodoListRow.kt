package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.listmeta.DateJot
import fr.mandarine.todolist.ui.listmeta.OpenCount
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperPalette
import fr.mandarine.todolist.ui.paper.RowVerb
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.SwipeMark
import fr.mandarine.todolist.ui.paper.SwipeReveal
import fr.mandarine.todolist.ui.paper.SwipeRow
import fr.mandarine.todolist.ui.paper.handwritten
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
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val verbs = rowVerbs(
        onRenameRequested?.let { RowVerb(stringResource(R.string.edit_list), it) },
        RowVerb(stringResource(R.string.delete_list), onDeleteRequested),
        onMoveUp?.let { RowVerb(stringResource(R.string.move_up), it) },
        onMoveDown?.let { RowVerb(stringResource(R.string.move_down), it) }
    )
    SwipeRow(
        key = summary.list.id,
        onDelete = onDeleteRequested,
        reveal = onRenameRequested?.let { SwipeReveal(SwipeMark.Pencil, it) },
        animated = animated,
        modifier = modifier.tearOff(tearing, animated, onTorn)
    ) {
        RuledRow(modifier = Modifier.spokenVerbs(verbs), onClick = onOpen) {
            RowName(summary = summary, animated = animated)
            Marginalia(summary = summary)
        }
    }
}

@Composable
private fun RowScope.RowName(summary: TodoListSummary, animated: Boolean) {
    val palette = LocalPaperPalette.current
    val style = MaterialTheme.typography.titleMedium
    val strike = rememberPenStrike(summary.list.id, summary.allDone, animated)
    val ink = if (summary.allDone) palette.inkDone else palette.inkRest
    Text(
        text = remember(summary.list.name) { handwritten(summary.list.name) },
        modifier = Modifier
            .weight(1f)
            .padding(end = NAME_END_GAP)
            .seatOnRule()
            .penStrike(strike, ink),
        style = style,
        color = ink,
        onTextLayout = strike::onTextLayout
    )
}

@Composable
private fun Marginalia(summary: TodoListSummary) {
    val palette = LocalPaperPalette.current
    val targetDate = summary.list.targetDate
    val dueDate = summary.list.dueDate
    val dueStatus = summary.dueDateStatus
    if (targetDate != null) {
        DateJot(
            date = targetDate,
            kind = DateKind.TARGET,
            showYear = summary.showTargetYear,
            tint = targetTint(palette, summary.isTargetDateElapsed)
        )
    }
    if (dueDate != null && dueStatus != null) {
        DateJot(
            date = dueDate,
            kind = DateKind.DUE,
            showYear = summary.showDueDateYear,
            tint = dueTint(palette, dueStatus)
        )
    }
    OpenCount(count = summary.activeCount)
}

internal fun targetTint(palette: PaperPalette, elapsed: Boolean): Color =
    if (elapsed) palette.inkDone else palette.inkMargin

internal fun dueTint(palette: PaperPalette, status: DueDateStatus): Color = when (status) {
    DueDateStatus.FUTURE -> palette.inkMargin
    DueDateStatus.TODAY -> palette.inkAmber
    DueDateStatus.OVERDUE -> palette.inkDanger
}
