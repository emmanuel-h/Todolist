package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.listmeta.ListProgress
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDialog
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperFocusMark
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.inked

private const val TWO_LINES = 2
private val NAME_TO_PROGRESS = 6.dp
private val HEAD_TO_RULE = 14.dp
private val ACTION_GLYPH = 20.dp
private val GLYPH_TO_LABEL = 16.dp

/**
 * Everything that can be done to a list besides opening it, held out at the foot of
 * the page under the thumb. It used to be three marks at the end of every row —
 * a pencil, a calendar and a bin — which on a page of lists added up to a column of
 * glyphs louder than the names they belonged to, and still said nothing about what
 * each did. One mark on the row, and the words here, say it once.
 *
 * Every choice puts this sheet down and hands over to the sheet that does the work:
 * the edit sheet, the calendar, or the question before a delete.
 */
@Composable
fun ListActionsSheet(
    summary: TodoListSummary,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onPickDate: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalPaperPalette.current
    val list = summary.list
    val dated = list.targetDate != null || list.dueDate != null
    PaperDialog(onDismissRequest = onDismiss, alignment = Alignment.BottomCenter) {
        Text(
            text = remember(list.name) { handwritten(list.name) },
            style = PaperType.field,
            color = palette.inked(InkTone.Words),
            maxLines = TWO_LINES,
            overflow = TextOverflow.Ellipsis
        )
        ListProgress(
            done = summary.completedCount,
            total = summary.activeCount + summary.completedCount,
            modifier = Modifier.padding(top = NAME_TO_PROGRESS)
        )
        Spacer(Modifier.height(HEAD_TO_RULE))
        HorizontalDivider(thickness = PaperDimens.rule, color = palette.rule)
        ActionLine(
            painter = painterResource(R.drawable.ic_edit),
            label = stringResource(R.string.action_rename_list),
            tint = palette.inked(InkTone.Words),
            onClick = onRename
        )
        ActionLine(
            painter = painterResource(R.drawable.ic_event),
            label = stringResource(if (dated) R.string.action_change_day else R.string.action_give_day),
            tint = palette.inked(InkTone.Words),
            onClick = onPickDate
        )
        ActionLine(
            painter = painterResource(R.drawable.ic_delete),
            label = stringResource(R.string.delete_list),
            tint = palette.inked(InkTone.Lost),
            onClick = onDelete
        )
    }
}

@Composable
private fun ActionLine(
    painter: Painter,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PaperDimens.touchTarget + 4.dp)
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = PaperFocusMark,
                role = Role.Button,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InkIcon(painter = painter, contentDescription = null, tint = tint, size = ACTION_GLYPH)
        Spacer(Modifier.width(GLYPH_TO_LABEL))
        Text(text = label, style = PaperType.label, color = tint)
    }
}
