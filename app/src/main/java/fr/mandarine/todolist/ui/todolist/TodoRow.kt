package fr.mandarine.todolist.ui.todolist

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextDecoration
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperInk
import fr.mandarine.todolist.ui.paper.RuledRow

private const val COMPLETED_TITLE_ALPHA = 0.5f

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
    toggleModifier: Modifier = Modifier
) {
    RuledRow(modifier = modifier) {
        DragHandle(visible = !item.isCompleted, modifier = handleModifier)
        if (editing) {
            RowTitleEditor(
                title = item.title,
                onCommit = onEditCommitted,
                onDismiss = onEditDismissed
            )
        } else {
            RowTitle(item = item, onToggle = onToggle)
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
            tint = PaperInk.inkBlue
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = stringResource(R.string.item_edit),
            onClick = onEditRequested,
            tint = PaperInk.pencil
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.item_delete),
            onClick = onDelete,
            tint = PaperInk.inkRedSoft
        )
    }
}

/**
 * Completed rows keep the handle's 48dp of space but draw nothing in it, so item
 * titles stay on the same vertical line across both sections.
 */
@Composable
private fun DragHandle(visible: Boolean, modifier: Modifier) {
    Box(
        modifier = Modifier
            .size(PaperDimens.iconButton)
            .then(if (visible) modifier else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            InkIcon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.drag_handle),
                tint = PaperInk.pencil
            )
        }
    }
}

@Composable
private fun RowScope.RowTitle(item: TodoItem, onToggle: () -> Unit) {
    Text(
        text = item.title,
        modifier = Modifier
            .weight(1f)
            .pointerInput(item.id) {
                detectTapGestures(onDoubleTap = { onToggle() })
            },
        style = MaterialTheme.typography.bodyLarge,
        color = if (item.isCompleted) {
            PaperInk.ink.copy(alpha = COMPLETED_TITLE_ALPHA)
        } else {
            PaperInk.ink
        },
        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null
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

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    everFocused = true
                } else if (everFocused) {
                    commitTitle(value.text, onCommit, onDismiss)
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = PaperInk.ink),
        singleLine = true,
        cursorBrush = SolidColor(PaperInk.inkBlue),
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
