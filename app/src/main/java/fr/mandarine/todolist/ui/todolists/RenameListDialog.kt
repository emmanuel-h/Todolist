package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.mandarine.todolist.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.LocalRuledHand
import fr.mandarine.todolist.ui.paper.OnRuleSlot
import fr.mandarine.todolist.ui.paper.PaperDialog
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.circledInInk
import fr.mandarine.todolist.ui.paper.highlightWash
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.rememberPaperHaptics
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.paper.trimmedToGlyphs

/**
 * The sheet a list is edited on: its name on the first rule with the caret already
 * on it, on the rule beneath the same date marks the add line wears, and below
 * that a row of highlighter swatches so the list can be coloured. There is no
 * confirm row and no way to cancel — as on every other line in the app the
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
    onColourChange: (ListColour) -> Unit = {},
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
        ColourPicker(
            selected = state.colour,
            onSelect = onColourChange,
            animated = animated
        )
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

private val SWATCH_TOUCH = 36.dp
private val SWATCH_RING = 26.dp
private val SWATCH_STROKE = 1.dp
private val PICKER_TOP_GAP = 8.dp

/**
 * Seven swatches in a row — the six named hues and [ListColour.None] (bare paper).
 * The chosen one is circled in ink. None draws a hairline border so it reads as a
 * real choice on the sheet rather than an empty slot.
 */
@Composable
private fun ColourPicker(
    selected: ListColour,
    onSelect: (ListColour) -> Unit,
    animated: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SWATCH_TOUCH + PICKER_TOP_GAP),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        ListColour.entries.forEach { colour ->
            ColourSwatch(
                colour = colour,
                selected = colour == selected,
                onSelect = { onSelect(colour) },
                animated = animated
            )
        }
    }
}

@Composable
private fun ColourSwatch(
    colour: ListColour,
    selected: Boolean,
    onSelect: () -> Unit,
    animated: Boolean
) {
    val palette = LocalPaperPalette.current
    val label = stringResource(colourStringRes(colour))
    val swatchColour = if (colour == ListColour.None) palette.paper else palette.highlightWash(colour)
    val borderColour = palette.pencil
    val ringColour = palette.inked(InkTone.Acted)
    val isNone = colour == ListColour.None

    Box(
        modifier = Modifier
            .size(SWATCH_TOUCH)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(SWATCH_RING)
                .drawWithCache {
                    val strokeWidth = SWATCH_STROKE.toPx()
                    onDrawBehind {
                        drawCircle(color = swatchColour)
                        if (isNone) {
                            drawCircle(
                                color = borderColour,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                }
                .circledInInk(
                    circled = selected,
                    seed = colour.ordinal,
                    color = ringColour,
                    animated = animated
                ),
            contentAlignment = Alignment.Center
        ) { }
    }
}

private fun colourStringRes(colour: ListColour): Int = when (colour) {
    ListColour.None -> R.string.colour_none
    ListColour.Butter -> R.string.colour_butter
    ListColour.Mint -> R.string.colour_mint
    ListColour.Rose -> R.string.colour_rose
    ListColour.Sky -> R.string.colour_sky
    ListColour.Peach -> R.string.colour_peach
    ListColour.Lilac -> R.string.colour_lilac
}
