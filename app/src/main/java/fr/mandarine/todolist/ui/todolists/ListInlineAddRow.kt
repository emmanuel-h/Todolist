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
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.seatOnRule

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
    val palette = LocalPaperPalette.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    RuledRow(modifier = modifier) {
        InkIconButton(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.cancel),
            onClick = onCancel,
            tint = palette.inkBlue,
            seat = IconSeat.OnRule
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
            tint = if (selection.targetDate != null) palette.inkBlue else palette.pencil,
            seat = IconSeat.OnRule
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_alarm),
            contentDescription = stringResource(R.string.set_due_date),
            onClick = onPickDueDate,
            modifier = dueDateModifier,
            tint = if (selection.dueDate != null) palette.inkBlue else palette.pencil,
            seat = IconSeat.OnRule
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = stringResource(R.string.create_list),
            onClick = onSubmit,
            modifier = submitModifier,
            tint = palette.inkBlue,
            seat = IconSeat.OnRule
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
    val palette = LocalPaperPalette.current
    Box(modifier = Modifier.weight(1f)) {
        if (text.isEmpty()) {
            Text(
                text = stringResource(R.string.list_name_hint),
                modifier = Modifier.seatOnRule(MaterialTheme.typography.bodyMedium),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.pencil
            )
        }
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = modifier.seatOnRule(MaterialTheme.typography.bodyMedium),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
            singleLine = true,
            cursorBrush = SolidColor(palette.inkBlue),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )
    }
}
