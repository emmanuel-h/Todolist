package fr.mandarine.todolist.ui.todolist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import fr.mandarine.todolist.ui.paper.PaperFocusMark
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.ui.paper.InkBudget
import fr.mandarine.todolist.ui.paper.InkRing
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.OnRuleSlot
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.RowVerb
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.SwipeMark
import fr.mandarine.todolist.ui.paper.SwipeReveal
import fr.mandarine.todolist.ui.paper.SwipeRow
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.penStrike
import fr.mandarine.todolist.ui.paper.rememberPenStrike
import fr.mandarine.todolist.ui.paper.rowVerbs
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.paper.spokenVerbs
import fr.mandarine.todolist.ui.paper.tearOff
import fr.mandarine.todolist.ui.paper.trimmedToGlyphs

private const val ROW_BODY_LABEL = "rowBody"

@Composable
fun TodoRow(
    item: TodoItem,
    checked: Boolean,
    editing: Boolean,
    onToggle: () -> Unit,
    onEditRequested: () -> Unit,
    onEditCommitted: (String) -> Unit,
    onEditDismissed: () -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
    toggleModifier: Modifier = Modifier,
    animated: Boolean = true,
    tearing: Boolean = false,
    onTorn: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val verbs = rowVerbs(
        RowVerb(
            stringResource(
                if (checked) R.string.item_mark_incomplete else R.string.item_mark_completed
            ),
            onToggle
        ),
        RowVerb(stringResource(R.string.item_edit), onEditRequested),
        RowVerb(stringResource(R.string.item_delete), onDeleteRequested),
        onMoveUp?.let { RowVerb(stringResource(R.string.move_up), it) },
        onMoveDown?.let { RowVerb(stringResource(R.string.move_down), it) }
    )
    SwipeRow(
        key = item.id,
        onDelete = onDeleteRequested,
        reveal = SwipeReveal(SwipeMark.Check, onToggle),
        enabled = !editing,
        animated = animated,
        modifier = modifier.tearOff(tearing, animated, onTorn)
    ) {
        RuledRow {
            InkRing(
                checked = checked,
                onToggle = onToggle,
                seed = item.id.hashCode(),
                contentDescription = stringResource(
                    if (checked) R.string.item_mark_incomplete else R.string.item_mark_completed
                ),
                stateDescription = stringResource(
                    if (checked) R.string.item_state_completed else R.string.item_state_active
                ),
                modifier = toggleModifier,
                animated = animated
            )
            RowBody(
                item = item,
                checked = checked,
                editing = editing,
                animated = animated,
                verbs = verbs,
                onEditRequested = onEditRequested,
                onEditCommitted = onEditCommitted,
                onEditDismissed = onEditDismissed
            )
        }
    }
}

@Composable
private fun RowScope.RowBody(
    item: TodoItem,
    checked: Boolean,
    editing: Boolean,
    animated: Boolean,
    verbs: List<RowVerb>,
    onEditRequested: () -> Unit,
    onEditCommitted: (String) -> Unit,
    onEditDismissed: () -> Unit
) {
    AnimatedContent(
        targetState = editing,
        modifier = Modifier.weight(1f),
        transitionSpec = {
            val enter = if (animated) fadeIn(PaperMotion.rowEnter) else EnterTransition.None
            val exit = if (animated) fadeOut(PaperMotion.rowExit) else ExitTransition.None
            (enter togetherWith exit) using null
        },
        label = ROW_BODY_LABEL
    ) { typing ->
        if (typing) {
            RowTitleEditor(
                title = item.title,
                style = MaterialTheme.typography.bodyLarge,
                onCommit = onEditCommitted,
                onDismiss = onEditDismissed
            )
        } else {
            RowTitle(
                item = item,
                checked = checked,
                animated = animated,
                verbs = verbs,
                onEditRequested = onEditRequested
            )
        }
    }
}

@Composable
private fun RowTitle(
    item: TodoItem,
    checked: Boolean,
    animated: Boolean,
    verbs: List<RowVerb>,
    onEditRequested: () -> Unit
) {
    val palette = LocalPaperPalette.current
    val style = MaterialTheme.typography.bodyLarge
    val strike = rememberPenStrike(item.id, checked, animated, INK_TICK_MILLIS)
    val ink = palette.inked(InkBudget.words(item.isCompleted))
    Text(
        text = remember(item.title) { handwritten(item.title) },
        modifier = Modifier
            .fillMaxWidth()
            .seatOnRule()
            .penStrike(strike, ink)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = PaperFocusMark,
                onClickLabel = stringResource(R.string.item_edit),
                onClick = onEditRequested
            )
            .spokenVerbs(verbs),
        style = style,
        color = ink,
        onTextLayout = strike::onTextLayout
    )
}

@Composable
internal fun RowTitleEditor(
    title: String,
    style: TextStyle,
    onCommit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var value by remember(title) {
        mutableStateOf(TextFieldValue(title, TextRange(title.length)))
    }
    var everFocused by remember { mutableStateOf(false) }
    val palette = LocalPaperPalette.current
    val editing = stringResource(R.string.item_edit)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    OnRuleSlot(modifier = Modifier.fillMaxWidth(), alignment = Alignment.TopStart) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .seatOnRule()
                .semantics { contentDescription = editing }
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        everFocused = true
                    } else if (everFocused) {
                        commitTitle(value.text, onCommit, onDismiss)
                    }
                },
            textStyle = style.trimmedToGlyphs().copy(color = palette.inked(InkTone.Words)),
            singleLine = true,
            cursorBrush = SolidColor(palette.inked(InkTone.Acted)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { commitTitle(value.text, onCommit, onDismiss) }
            )
        )
    }
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
