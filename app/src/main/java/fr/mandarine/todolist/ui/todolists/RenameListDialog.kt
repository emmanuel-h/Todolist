package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.PaperDialog
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import java.util.Locale

private val DATE_BOX_HEIGHT = 40.dp
private val TOGGLE_GLYPH = 20.dp

@Composable
fun RenameListDialog(
    state: RenameState,
    onNameChange: (String) -> Unit,
    onKindChange: (DateKind) -> Unit,
    onPickDate: () -> Unit,
    onClearDate: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val locale = Locale.getDefault(Locale.Category.FORMAT)
    val palette = LocalPaperPalette.current
    PaperDialog(onDismissRequest = onDismiss) {
        NameField(name = state.name, onNameChange = onNameChange, onConfirm = onConfirm)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KindToggle(kind = state.selection.kind, onKindChange = onKindChange)
            DateBox(
                selection = state.selection,
                locale = locale,
                onClick = onPickDate,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            if (state.selection.date != null) {
                InkIconButton(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(
                        if (state.selection.kind == DateKind.TARGET) {
                            R.string.clear_target_date
                        } else {
                            R.string.clear_due_date
                        }
                    ),
                    onClick = onClearDate,
                    tint = palette.pencil
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            InkIconButton(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.cancel),
                onClick = onDismiss,
                tint = palette.inkSoft
            )
            InkIconButton(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.rename_list),
                onClick = onConfirm,
                tint = palette.inkBlue
            )
        }
    }
}

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit, onConfirm: () -> Unit) {
    val palette = LocalPaperPalette.current
    val focusRequester = remember { FocusRequester() }
    var value by remember {
        mutableStateOf(TextFieldValue(name, TextRange(name.length)))
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = {
            value = it
            onNameChange(it.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .padding(bottom = 6.dp),
        textStyle = PaperType.field.copy(color = palette.ink),
        singleLine = true,
        cursorBrush = SolidColor(palette.inkBlue),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onConfirm() })
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PaperDimens.rule)
            .background(palette.rule)
    )
}

@Composable
private fun KindToggle(kind: DateKind, onKindChange: (DateKind) -> Unit) {
    val palette = LocalPaperPalette.current
    Row(modifier = Modifier.border(PaperDimens.rule, palette.pencil)) {
        ToggleCell(
            iconRes = R.drawable.ic_event,
            descriptionRes = R.string.set_target_date,
            selected = kind == DateKind.TARGET,
            onClick = { onKindChange(DateKind.TARGET) }
        )
        Box(
            modifier = Modifier
                .size(PaperDimens.rule, PaperDimens.iconButton)
                .background(palette.pencil)
        )
        ToggleCell(
            iconRes = R.drawable.ic_alarm,
            descriptionRes = R.string.set_due_date,
            selected = kind == DateKind.DUE,
            onClick = { onKindChange(DateKind.DUE) }
        )
    }
}

@Composable
private fun ToggleCell(
    iconRes: Int,
    descriptionRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalPaperPalette.current
    Box(
        modifier = Modifier
            .size(PaperDimens.iconButton)
            .background(if (selected) palette.inkBluePale else palette.paperSheet)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        InkIcon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
            tint = if (selected) palette.inkBlue else palette.pencil,
            size = TOGGLE_GLYPH
        )
    }
}

@Composable
private fun DateBox(
    selection: DateSelection,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val palette = LocalPaperPalette.current
    val description = stringResource(
        if (selection.kind == DateKind.TARGET) R.string.set_target_date else R.string.set_due_date
    )
    Box(
        modifier = modifier
            .border(PaperDimens.rule, palette.rule)
            .heightIn(min = DATE_BOX_HEIGHT)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val date = selection.date
        if (date == null) {
            InkIcon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = palette.pencil,
                size = TOGGLE_GLYPH
            )
        } else {
            Text(
                text = formatListDate(date, showYear = true, locale = locale),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
