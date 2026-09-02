package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.mandarine.todolist.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.LocalRuledHand
import fr.mandarine.todolist.ui.paper.OnRuleSlot
import fr.mandarine.todolist.ui.paper.PaperDialog
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.rememberPaperHaptics
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.paper.trimmedToGlyphs

/**
 * The sheet a list is edited on: its name on the first rule with the caret already
 * on it, and on the rule beneath, the same date marks the add line wears. There is
 * no confirm row and no way to cancel — as on every other line in the app the
 * keyboard's own Done finishes the writing, and putting the sheet down keeps
 * whatever was written on it rather than throwing it away.
 */
@Composable
fun RenameListDialog(
    state: RenameState,
    onNameChange: (String) -> Unit,
    onKindChange: (DateKind) -> Unit,
    onPickDate: (DateKind) -> Unit,
    onClearDate: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    animated: Boolean = true
) {
    val haptics = rememberPaperHaptics()
    val said = rememberDateKindSaid()
    val putDown: () -> Unit = {
        if (state.name.isBlank()) {
            onDismiss()
        } else {
            haptics.submit()
            onConfirm()
        }
    }
    PaperDialog(onDismissRequest = putDown) {
        NameLine(name = state.name, onNameChange = onNameChange, onDone = putDown)
        SheetLine {
            DateMarks(
                selection = state.selection,
                said = said,
                onKindChange = onKindChange,
                onPickDate = onPickDate,
                onClearDate = onClearDate
            )
        }
        DateKindCaption(said = said, animated = animated)
    }
}

@Composable
private fun SheetLine(content: @Composable RowScope.() -> Unit) {
    val rule = LocalPaperPalette.current.rule
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxOf(LocalPagePitch.current, PaperDimens.touchTarget))
            .drawBehind {
                val thickness = PaperDimens.rule.toPx()
                drawRect(
                    color = rule,
                    topLeft = Offset(0f, size.height - thickness),
                    size = Size(size.width, thickness)
                )
            },
        verticalAlignment = Alignment.Top,
        content = content
    )
}

@Composable
private fun NameLine(name: String, onNameChange: (String) -> Unit, onDone: () -> Unit) {
    val palette = LocalPaperPalette.current
    val hand = LocalRuledHand.current.listLine
    val focusRequester = remember { FocusRequester() }
    val naming = stringResource(R.string.edit_list)
    var value by remember {
        mutableStateOf(TextFieldValue(name, TextRange(name.length)))
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    SheetLine {
        OnRuleSlot(modifier = Modifier.weight(1f), alignment = Alignment.TopStart) {
            BasicTextField(
                value = value,
                onValueChange = {
                    value = it
                    onNameChange(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .seatOnRule()
                    .semantics { contentDescription = naming }
                    .focusRequester(focusRequester),
                textStyle = hand.trimmedToGlyphs().copy(color = palette.inked(InkTone.Words)),
                singleLine = true,
                cursorBrush = SolidColor(palette.inked(InkTone.Acted)),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() })
            )
        }
    }
}
