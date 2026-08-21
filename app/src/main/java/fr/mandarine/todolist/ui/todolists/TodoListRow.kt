package fr.mandarine.todolist.ui.todolists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.paper.CountBadge
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperInk
import fr.mandarine.todolist.ui.paper.RuledRow
import java.util.Locale

private const val ALL_DONE_ALPHA = 0.5f
private const val HIDDEN_ALPHA = 0f
private val TARGET_ICON = 14.dp
private val DUE_LIMIT_ICON = 12.dp
private val CONFIRM_STRIP_INSET = 138.dp
private val CONFIRM_BUTTON_TRAVEL = 24.dp
private const val CONFIRM_FADE_IN_MILLIS = 150
private const val CONFIRM_FADE_OUT_MILLIS = 120
private const val CONFIRM_BUTTON_MILLIS = 200
private const val CONFIRM_BUTTON_STAGGER_MILLIS = 50

@Composable
fun TodoListRow(
    summary: TodoListSummary,
    confirmingDelete: Boolean,
    animated: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteCancelled: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
    deleteModifier: Modifier = Modifier,
    confirmModifier: Modifier = Modifier
) {
    val locale = Locale.getDefault(Locale.Category.FORMAT)
    Box(modifier) {
        RuledRow(
            modifier = Modifier.alpha(if (confirmingDelete) HIDDEN_ALPHA else 1f),
            minHeight = PaperDimens.listRowHeight,
            verticalPadding = 8.dp
        ) {
            DragHandle(handleModifier)
            Row(
                modifier = Modifier.weight(1f).clickable(onClick = onOpen),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InkIconButton(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.edit_list),
                    onClick = onRename,
                    tint = PaperInk.pencil
                )
                RowText(summary = summary, locale = locale)
                CountBadge(
                    painter = painterResource(R.drawable.ic_radio_button_unchecked),
                    count = summary.activeCount,
                    modifier = Modifier.padding(end = 4.dp),
                    tint = PaperInk.inkSoft,
                    borderColor = PaperInk.pencil
                )
                CountBadge(
                    painter = painterResource(R.drawable.ic_check_circle),
                    count = summary.completedCount,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = PaperInk.pencil,
                    borderColor = PaperInk.rule
                )
                InkIconButton(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete_list),
                    onClick = onDeleteRequested,
                    modifier = deleteModifier,
                    tint = PaperInk.inkRedSoft
                )
            }
        }
        AnimatedVisibility(
            visible = confirmingDelete,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(tween(if (animated) CONFIRM_FADE_IN_MILLIS else 0)),
            exit = fadeOut(tween(if (animated) CONFIRM_FADE_OUT_MILLIS else 0))
        ) {
            DeleteConfirmStrip(
                name = summary.list.name,
                animated = animated,
                onCancel = onDeleteCancelled,
                onConfirm = onDeleteConfirmed,
                confirmModifier = confirmModifier
            )
        }
    }
}

/**
 * The handle sits outside the row's click target on purpose: a clickable
 * ancestor swallows the handle's drag before it ever reaches the touch slop, so
 * the row opens from everything to the right of the handle instead.
 *
 * Done lists keep the handle in place but cannot be dragged — the reorder is
 * addressed within the active section, so a done row has no index to move to.
 */
@Composable
private fun DragHandle(modifier: Modifier) {
    Box(
        modifier = Modifier.size(PaperDimens.iconButton).then(modifier),
        contentAlignment = Alignment.Center
    ) {
        InkIcon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = stringResource(R.string.drag_handle),
            tint = PaperInk.pencil
        )
    }
}

@Composable
private fun RowScope.RowText(summary: TodoListSummary, locale: Locale) {
    Column(
        modifier = Modifier.weight(1f).padding(start = 2.dp, end = 8.dp)
    ) {
        Text(
            text = summary.list.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (summary.allDone) {
                PaperInk.ink.copy(alpha = ALL_DONE_ALPHA)
            } else {
                PaperInk.ink
            },
            textDecoration = if (summary.allDone) TextDecoration.LineThrough else null
        )
        val targetDate = summary.list.targetDate
        if (targetDate != null) {
            DateLine(
                text = formatListDate(targetDate, summary.showTargetYear, locale),
                tint = targetTint(summary.isTargetDateElapsed)
            ) {
                InkIcon(
                    painter = painterResource(R.drawable.ic_event),
                    contentDescription = null,
                    tint = it,
                    size = TARGET_ICON
                )
            }
        }
        val dueDate = summary.list.dueDate
        val dueStatus = summary.dueDateStatus
        if (dueDate != null && dueStatus != null) {
            DateLine(
                text = formatListDate(dueDate, summary.showDueDateYear, locale),
                tint = dueTint(dueStatus)
            ) {
                InkIcon(
                    painter = painterResource(R.drawable.ic_alarm),
                    contentDescription = null,
                    tint = it,
                    size = TARGET_ICON
                )
                InkIcon(
                    painter = painterResource(R.drawable.ic_tab_right),
                    contentDescription = null,
                    tint = it,
                    size = DUE_LIMIT_ICON
                )
            }
        }
    }
}

/**
 * A target date is an intention and fades once it has passed; a due date is a
 * limit and gets louder as it approaches. Neither ever borrows the other's ink.
 */
internal fun targetTint(elapsed: Boolean): Color =
    if (elapsed) PaperInk.inkSoft else PaperInk.inkBlue

internal fun dueTint(status: DueDateStatus): Color = when (status) {
    DueDateStatus.FUTURE -> PaperInk.inkBlue
    DueDateStatus.TODAY -> PaperInk.inkAmber
    DueDateStatus.OVERDUE -> PaperInk.inkRed
}

@Composable
private fun DateLine(
    text: String,
    tint: Color,
    icons: @Composable RowScope.(Color) -> Unit
) {
    Row(
        modifier = Modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icons(tint)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun AnimatedVisibilityScope.DeleteConfirmStrip(
    name: String,
    animated: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmModifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperInk.inkRedWash)
            .clickable(onClick = onCancel)
            .padding(start = CONFIRM_STRIP_INSET, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = PaperInk.inkRedDeep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_delete_cancel_ring),
            contentDescription = stringResource(R.string.cancel),
            onClick = onCancel,
            modifier = confirmActionEntry(animated, order = 0),
            tint = PaperInk.inkRedDeep
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_delete_confirm_ring),
            contentDescription = stringResource(R.string.delete),
            onClick = onConfirm,
            modifier = confirmModifier.then(confirmActionEntry(animated, order = 1)),
            tint = PaperInk.inkRed
        )
    }
}

/**
 * The two rings arrive one after the other so the strip reads as an escalation
 * rather than as a second row appearing all at once.
 */
@Composable
private fun AnimatedVisibilityScope.confirmActionEntry(animated: Boolean, order: Int): Modifier {
    if (!animated) return Modifier
    val travel = with(LocalDensity.current) { CONFIRM_BUTTON_TRAVEL.roundToPx() }
    val delay = order * CONFIRM_BUTTON_STAGGER_MILLIS
    return Modifier.animateEnterExit(
        enter = slideInHorizontally(tween(CONFIRM_BUTTON_MILLIS, delay)) { travel } +
            fadeIn(tween(CONFIRM_BUTTON_MILLIS, delay)),
        exit = ExitTransition.None
    )
}
