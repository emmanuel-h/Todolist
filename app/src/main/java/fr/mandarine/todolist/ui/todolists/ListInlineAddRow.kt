package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.PaperInk
import fr.mandarine.todolist.ui.paper.RuledRow

@Composable
fun ListInlineAddRow(
    text: String,
    selection: DateSelection,
    onTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onPickTargetDate: () -> Unit,
    onPickDueDate: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    nameFieldModifier: Modifier = Modifier,
    targetDateModifier: Modifier = Modifier,
    dueDateModifier: Modifier = Modifier,
    submitModifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    RuledRow(modifier = modifier) {
        InkIconButton(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.cancel),
            onClick = onCancel,
            tint = PaperInk.inkBlue
        )
        NameField(
            text = text,
            onTextChange = onTextChange,
            onSubmit = onSubmit,
            modifier = nameFieldModifier.focusRequester(focusRequester)
        )
        InkIconButton(
            painter = painterResource(
                if (selection.targetDate != null) R.drawable.ic_event else R.drawable.ic_event_add
            ),
            contentDescription = stringResource(R.string.set_target_date),
            onClick = onPickTargetDate,
            modifier = targetDateModifier,
            tint = if (selection.targetDate != null) PaperInk.inkBlue else PaperInk.pencil
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_alarm),
            contentDescription = stringResource(R.string.set_due_date),
            onClick = onPickDueDate,
            modifier = dueDateModifier,
            tint = if (selection.dueDate != null) PaperInk.inkBlue else PaperInk.pencil
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = stringResource(R.string.create_list),
            onClick = onSubmit,
            modifier = submitModifier,
            tint = PaperInk.inkBlue
        )
    }
}

@Composable
private fun RowScope.NameField(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier
) {
    Box(modifier = Modifier.weight(1f)) {
        if (text.isEmpty()) {
            Text(
                text = stringResource(R.string.list_name_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = PaperInk.pencil
            )
        }
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = modifier,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PaperInk.ink),
            singleLine = true,
            cursorBrush = SolidColor(PaperInk.inkBlue),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )
    }
}
