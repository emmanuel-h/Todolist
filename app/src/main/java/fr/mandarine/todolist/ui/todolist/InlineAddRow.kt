package fr.mandarine.todolist.ui.todolist

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.GhostRow
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.seatOnRule

@Composable
fun InlineAddRow(
    expanded: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    ghostModifier: Modifier = Modifier,
    submitModifier: Modifier = Modifier
) {
    if (!expanded) {
        GhostRow(onClick = onExpand, modifier = modifier.then(ghostModifier))
        return
    }

    val palette = LocalPaperPalette.current
    val focusRequester = remember { FocusRequester() }
    var everFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    RuledRow(modifier = modifier) {
        InkIconButton(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = stringResource(R.string.submit_inline_add),
            onClick = onSubmit,
            modifier = submitModifier,
            tint = palette.inkBlue,
            seat = IconSeat.OnRule
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (text.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_item_hint),
                    modifier = Modifier.seatOnRule(MaterialTheme.typography.bodyMedium),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.pencil
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .seatOnRule(MaterialTheme.typography.bodyMedium)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            everFocused = true
                        } else if (everFocused) {
                            onCollapse()
                        }
                    },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
                singleLine = true,
                cursorBrush = SolidColor(palette.inkBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() })
            )
        }
    }
}
