package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.mandarine.todolist.R

private val QUESTION_BOTTOM = 8.dp
private val HAND_IN_PRINT = 1.1.em
private val BUTTONS_TOP = 4.dp
private val BUTTON_GAP = 16.dp

/**
 * The confirmation sheet for a destructive delete. A smaller sheet laid on the
 * page, carrying the name of what is about to go. A list delete adds a second
 * line naming how many items go with it — only when the count is above zero.
 * An item delete carries no second line. Only Delete commits the write; every
 * other way to leave — Cancel, back, the veil — is a Cancel.
 */
@Composable
fun DeleteConfirmDialog(
    name: String,
    cascadeCount: Int?,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalPaperPalette.current
    PaperDialog(onDismissRequest = onCancel) {
        Text(
            text = printedAround(stringResource(R.string.delete_prompt, name), name),
            style = PaperType.question,
            color = palette.inked(InkTone.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = QUESTION_BOTTOM)
        )
        if (cascadeCount != null && cascadeCount > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.delete_list_cascade,
                    cascadeCount,
                    cascadeCount
                ),
                style = PaperType.note,
                color = palette.inked(InkTone.Margin),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(BUTTONS_TOP))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            DialogButton(
                label = stringResource(R.string.cancel),
                tint = palette.inked(InkTone.Margin),
                onClick = onCancel
            )
            Spacer(Modifier.width(BUTTON_GAP))
            DialogButton(
                label = stringResource(R.string.delete),
                tint = palette.inked(InkTone.Lost),
                onClick = onDelete
            )
        }
    }
}

/**
 * Confirmation slip for removing a date from a list. A smaller sheet carrying the
 * list name, with Cancel and Remove — nothing cleared unless Remove is pressed.
 */
@Composable
fun RemoveDateConfirmDialog(
    listName: String,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = LocalPaperPalette.current
    PaperDialog(onDismissRequest = onCancel) {
        Text(
            text = printedAround(stringResource(R.string.remove_date_prompt, listName), listName),
            style = PaperType.question,
            color = palette.inked(InkTone.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = QUESTION_BOTTOM)
        )
        Spacer(Modifier.height(BUTTONS_TOP))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            DialogButton(
                label = stringResource(R.string.cancel),
                tint = palette.inked(InkTone.Margin),
                onClick = onCancel
            )
            Spacer(Modifier.width(BUTTON_GAP))
            DialogButton(
                label = stringResource(R.string.remove_date),
                tint = palette.inked(InkTone.Words),
                onClick = onRemove
            )
        }
    }
}

@Composable
internal fun DialogButton(label: String, tint: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(PaperDimens.touchTarget)
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = source,
                indication = PaperFocusMark,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = PaperType.label,
            color = tint
        )
    }
}

/**
 * A question the page puts about something the reader wrote: the question is
 * printed, and the reader's own words inside it stay in their hand.
 */
internal fun printedAround(question: String, written: String): AnnotatedString = buildAnnotatedString {
    append(question)
    val at = if (written.isEmpty()) -1 else question.indexOf(written)
    if (at >= 0) {
        addStyle(SpanStyle(fontFamily = PaperType.hand, fontSize = HAND_IN_PRINT), at, at + written.length)
    }
}
