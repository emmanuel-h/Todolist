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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.paper.CountBadge
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.OnRuleSlot
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperPalette
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.penStrike
import fr.mandarine.todolist.ui.paper.rememberPenStrike
import fr.mandarine.todolist.ui.paper.seatOnRule
import java.util.Locale

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
    val palette = LocalPaperPalette.current
    Box(modifier) {
        RuledRow(modifier = Modifier.alpha(if (confirmingDelete) HIDDEN_ALPHA else 1f)) {
            DragHandle(handleModifier)
            Row(
                modifier = Modifier.weight(1f).clickable(onClick = onOpen),
                verticalAlignment = Alignment.Top
            ) {
                InkIconButton(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.edit_list),
                    onClick = onRename,
                    tint = palette.pencil,
                    seat = IconSeat.OnRule
                )
                RowText(summary = summary, locale = locale, animated = animated)
                OnRuleSlot(Modifier.padding(end = 4.dp)) {
                    CountBadge(
                        painter = painterResource(R.drawable.ic_radio_button_unchecked),
                        count = summary.activeCount,
                        tint = palette.inkSoft,
                        borderColor = palette.pencil
                    )
                }
                OnRuleSlot(Modifier.padding(end = 8.dp)) {
                    CountBadge(
                        painter = painterResource(R.drawable.ic_check_circle),
                        count = summary.completedCount,
                        tint = palette.pencil,
                        borderColor = palette.rule
                    )
                }
                InkIconButton(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete_list),
                    onClick = onDeleteRequested,
                    modifier = deleteModifier,
                    tint = palette.inkRedSoft,
                    seat = IconSeat.OnRule
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
    OnRuleSlot(modifier = Modifier.width(PaperDimens.iconButton).then(modifier)) {
        InkIcon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = stringResource(R.string.drag_handle),
            tint = LocalPaperPalette.current.pencil
        )
    }
}

@Composable
private fun RowScope.RowText(summary: TodoListSummary, locale: Locale, animated: Boolean) {
    val palette = LocalPaperPalette.current
    val targetDate = summary.list.targetDate
    val dueDate = summary.list.dueDate
    val dueStatus = summary.dueDateStatus
    Column(
        modifier = Modifier.weight(1f).padding(start = 2.dp, end = 8.dp)
    ) {
        val style = MaterialTheme.typography.titleMedium
        val strike = rememberPenStrike(summary.list.id, summary.allDone, animated)
        val ink = if (summary.allDone) palette.inkDone else palette.ink
        Text(
            text = remember(summary.list.name) { handwritten(summary.list.name) },
            modifier = Modifier.seatOnRule(style).penStrike(strike, ink),
            style = style,
            color = ink,
            onTextLayout = strike::onTextLayout
        )
        if (targetDate == null && dueDate == null) return@Column
        DateLines {
            if (targetDate != null) {
                DateLine(
                    text = formatListDate(targetDate, summary.showTargetYear, locale),
                    tint = targetTint(palette, summary.isTargetDateElapsed)
                ) {
                    InkIcon(
                        painter = painterResource(R.drawable.ic_event),
                        contentDescription = null,
                        tint = it,
                        size = TARGET_ICON
                    )
                }
            }
            if (dueDate != null && dueStatus != null) {
                DateLine(
                    text = formatListDate(dueDate, summary.showDueDateYear, locale),
                    tint = dueTint(palette, dueStatus)
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
}

/**
 * A target date is an intention and fades once it has passed; a due date is a
 * limit and gets louder as it approaches. Neither ever borrows the other's ink.
 */
internal fun targetTint(palette: PaperPalette, elapsed: Boolean): Color =
    if (elapsed) palette.inkSoft else palette.inkBlue

internal fun dueTint(palette: PaperPalette, status: DueDateStatus): Color = when (status) {
    DueDateStatus.FUTURE -> palette.inkBlue
    DueDateStatus.TODAY -> palette.inkAmber
    DueDateStatus.OVERDUE -> palette.inkRed
}

@Composable
private fun DateLines(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.height(LocalPagePitch.current),
        verticalArrangement = Arrangement.Bottom,
        content = content
    )
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
    val palette = LocalPaperPalette.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.inkRedWash)
            .clickable(onClick = onCancel)
            .padding(start = CONFIRM_STRIP_INSET, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = palette.inkRedDeep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_delete_cancel_ring),
            contentDescription = stringResource(R.string.cancel),
            onClick = onCancel,
            modifier = confirmActionEntry(animated, order = 0),
            tint = palette.inkRedDeep
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_delete_confirm_ring),
            contentDescription = stringResource(R.string.delete),
            onClick = onConfirm,
            modifier = confirmModifier.then(confirmActionEntry(animated, order = 1)),
            tint = palette.inkRed
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
