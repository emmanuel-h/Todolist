package fr.mandarine.todolist.ui.todolist

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.OnRuleSlot
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.penStrike
import fr.mandarine.todolist.ui.paper.rememberPenStrike
import fr.mandarine.todolist.ui.paper.seatOnRule

@Composable
fun TodoRow(
    item: TodoItem,
    editing: Boolean,
    onToggle: () -> Unit,
    onEditRequested: () -> Unit,
    onEditCommitted: (String) -> Unit,
    onEditDismissed: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
    toggleModifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val palette = LocalPaperPalette.current
    RuledRow(modifier = modifier) {
        DragHandle(visible = !item.isCompleted, modifier = handleModifier)
        if (editing) {
            RowTitleEditor(
                title = item.title,
                onCommit = onEditCommitted,
                onDismiss = onEditDismissed
            )
        } else {
            RowTitle(item = item, animated = animated, onToggle = onToggle)
        }
        InkIconButton(
            painter = painterResource(
                if (item.isCompleted) R.drawable.ic_undo else R.drawable.ic_check
            ),
            contentDescription = stringResource(
                if (item.isCompleted) R.string.item_mark_incomplete else R.string.item_mark_completed
            ),
            onClick = onToggle,
            modifier = toggleModifier,
            tint = palette.inkBlue,
            seat = IconSeat.OnRule
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = stringResource(R.string.item_edit),
            onClick = onEditRequested,
            tint = palette.pencil,
            seat = IconSeat.OnRule
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.item_delete),
            onClick = onDelete,
            tint = palette.inkRedSoft,
            seat = IconSeat.OnRule
        )
    }
}

/**
 * Completed rows keep the handle's 48dp of space but draw nothing in it, so item
 * titles stay on the same vertical line across both sections.
 */
@Composable
private fun DragHandle(visible: Boolean, modifier: Modifier) {
    OnRuleSlot(
        modifier = Modifier
            .width(PaperDimens.iconButton)
            .then(if (visible) modifier else Modifier)
    ) {
        if (visible) {
            InkIcon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.drag_handle),
                tint = LocalPaperPalette.current.pencil
            )
        }
    }
}

@Composable
private fun RowScope.RowTitle(item: TodoItem, animated: Boolean, onToggle: () -> Unit) {
    val palette = LocalPaperPalette.current
    val style = MaterialTheme.typography.bodyLarge
    val strike = rememberPenStrike(item.id, item.isCompleted, animated)
    val ink = if (item.isCompleted) palette.inkDone else palette.ink
    Text(
        text = remember(item.title) { handwritten(item.title) },
        modifier = Modifier
            .weight(1f)
            .seatOnRule(style)
            .penStrike(strike, ink)
            .pointerInput(item.id) {
                detectTapGestures(onDoubleTap = { onToggle() })
            },
        style = style,
        color = ink,
        onTextLayout = strike::onTextLayout
    )
}

@Composable
private fun RowScope.RowTitleEditor(
    title: String,
    onCommit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var value by remember(title) {
        mutableStateOf(TextFieldValue(title, TextRange(title.length)))
    }
    var everFocused by remember { mutableStateOf(false) }
    val palette = LocalPaperPalette.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier
            .weight(1f)
            .seatOnRule(MaterialTheme.typography.bodyLarge)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    everFocused = true
                } else if (everFocused) {
                    commitTitle(value.text, onCommit, onDismiss)
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.ink),
        singleLine = true,
        cursorBrush = SolidColor(palette.inkBlue),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { commitTitle(value.text, onCommit, onDismiss) }
        )
    )
}

internal fun commitTitle(
    text: String,
    onCommit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (text.isNotBlank()) {
        onCommit(text)
    }
    onDismiss()
}
