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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.ui.paper.GlyphFoot
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkBudget
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.InkRing
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.OnRuleSlot
import fr.mandarine.todolist.ui.paper.PaperFocusMark
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.RowVerb
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.fillingTheLine
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

/**
 * One item, drawn as one ruled line: the ring, the words, and the two glyphs at the
 * end of the row.
 *
 * ### Reading a composable like this one
 * A `@Composable` function draws; it returns nothing and holds nothing. Everything
 * it needs comes in as a parameter, and everything it wants to *change* it reports
 * by calling one of the `on…` lambdas — the "state down, events up" arrangement.
 * So this function cannot tick an item; it can only say that the ring was tapped,
 * and something above it decides what that means.
 *
 * That is why [checked] is a separate parameter from `item.isCompleted`: while a
 * tick is being *drawn* the two disagree deliberately, and the page (not the
 * database) owns that intermediate truth. See `TodoListScreenState.inked`.
 *
 * ### The `modifier` parameter
 * By convention every composable takes a `modifier` as its first optional parameter
 * and applies it to its outermost element, so a caller can position and size it
 * without this function knowing anything about the layout it lands in. Modifiers
 * chain left to right and **order matters** — `.padding().background()` and
 * `.background().padding()` draw differently.
 *
 * ### The three states of the row body
 * A row is either being read, being retyped, or being torn off. Reading and
 * retyping are swapped by [RowBody]'s `AnimatedContent`; tearing is the
 * `.tearOff(...)` modifier, which is why it wraps the whole [RuledRow] rather than
 * living inside it.
 *
 * Every lambda parameter is a *request*, not an action: `onDeleteRequested` raises
 * the confirmation prompt, it does not delete.
 */
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
    animated: Boolean = true,
    tearing: Boolean = false,
    onTorn: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val palette = LocalPaperPalette.current
    val verbs = rowVerbs(
        onMoveUp?.let { RowVerb(stringResource(R.string.move_up), it) },
        onMoveDown?.let { RowVerb(stringResource(R.string.move_down), it) }
    )
    RuledRow(modifier = modifier.tearOff(tearing, animated, onTorn)) {
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
        InkIconButton(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = stringResource(R.string.item_edit),
            onClick = onEditRequested,
            tint = palette.inked(InkTone.Margin),
            pressedTint = palette.inked(InkTone.Words),
            seat = IconSeat.OnRule,
            foot = GlyphFoot.pencil,
            glyphSize = PaperDimens.rowGlyph,
            buttonWidth = PaperDimens.rowGlyphButton
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.item_delete),
            onClick = onDeleteRequested,
            tint = palette.inked(InkTone.Margin),
            pressedTint = palette.inked(InkTone.Words),
            seat = IconSeat.OnRule,
            foot = GlyphFoot.trash,
            glyphSize = PaperDimens.rowGlyph,
            buttonWidth = PaperDimens.rowGlyphButton
        )
    }
}

/**
 * Swaps the row's text between "being read" and "being retyped", crossfading
 * between the two.
 *
 * `RowScope.` as a receiver is what makes `Modifier.weight(1f)` available — weight
 * only means something inside a `Row`, and declaring the receiver is how Compose
 * enforces that at compile time rather than at runtime. The weight is what makes
 * the words take all the space the ring and the two glyphs do not.
 *
 * `using null` disables the default size transform, so the swap does not also
 * animate the row's height; the two states are the same height and animating it
 * only introduces a wobble.
 */
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
                style = MaterialTheme.typography.bodyLarge.fillingTheLine(),
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

/**
 * The item's words, at rest.
 *
 * Three things are layered on the one text node, and the order in the modifier
 * chain is what makes them work: `seatOnRule` puts the baseline on the ruled line,
 * `penStrike` draws the strike-through *over* the glyphs, and `clickable` makes the
 * words themselves the target for retyping.
 *
 * `onTextLayout = strike::onTextLayout` is the key connection — the strike is drawn
 * as a real path along the measured glyph baseline, so it cannot be computed until
 * the text has been laid out. `::` is a method reference, the same as writing
 * `{ strike.onTextLayout(it) }`.
 *
 * `spokenVerbs` adds the move-up/move-down actions for screen readers, since
 * reordering is otherwise only reachable by dragging.
 */
@Composable
private fun RowTitle(
    item: TodoItem,
    checked: Boolean,
    animated: Boolean,
    verbs: List<RowVerb>,
    onEditRequested: () -> Unit
) {
    val palette = LocalPaperPalette.current
    val style = MaterialTheme.typography.bodyLarge.fillingTheLine()
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

/**
 * The row while it is being retyped.
 *
 * `BasicTextField` rather than Material's `TextField` because the latter draws a
 * container, a label and an indicator line — furniture that would sit on top of the
 * ruled paper. Here the paper *is* the decoration.
 *
 * The value is held as a `TextFieldValue` rather than a `String` so it can carry
 * the selection: `TextRange(title.length)` puts the caret at the end on open rather
 * than at the start. `remember(title)` re-seeds it if the underlying title changes.
 *
 * ### Committing
 * There are two ways out — the IME's Done key, and losing focus — and both funnel
 * into [commitTitle]. The `everFocused` flag guards the focus path: a text field is
 * unfocused for the first instant of its life, and without the flag it would commit
 * and dismiss itself before the reader ever saw it.
 *
 * `LaunchedEffect(Unit)` runs its body once when the composable first enters the
 * composition (and is cancelled when it leaves); it is how a composable does a
 * one-off side effect like grabbing focus.
 */
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
                        commitTitle(value.text, title, onCommit, onDismiss)
                    }
                },
            textStyle = style.trimmedToGlyphs().copy(color = palette.inked(InkTone.Words)),
            singleLine = true,
            cursorBrush = SolidColor(palette.inked(InkTone.Acted)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { commitTitle(value.text, title, onCommit, onDismiss) }
            )
        )
    }
}

/**
 * Blank text is a dismissal, not a rename — an emptied row keeps the words it had
 * rather than becoming a line with nothing on it. Either way the editor closes.
 */
internal fun commitTitle(
    text: String,
    original: String,
    onCommit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (text.isNotBlank() && text != original) {
        onCommit(text)
    }
    onDismiss()
}
