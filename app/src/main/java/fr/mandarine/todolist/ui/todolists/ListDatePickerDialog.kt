package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.DialogButton
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperCalendar
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperDialog
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.RemoveDateConfirmDialog
import fr.mandarine.todolist.ui.paper.inked
import java.time.LocalDate

private const val KIND_WORDS_LINES = 1
private val KIND_WORDS_GAP = 8.dp

/**
 * A smaller sheet with a month written on it, laid on whatever sheet asked for a
 * date. There is no confirm row: circling a day is the answer, exactly as ticking
 * a ring is the answer everywhere else on the page, and putting the sheet down
 * leaves the date as it was.
 *
 * The sheet carries the same two kind marks the line being written and the edit
 * sheet carry, answering to the same three presses — which is what makes a day
 * removable from wherever the reader pressed to see it. Reaching the marks used to
 * mean pressing the list's *name* instead, and nothing on a date said so.
 *
 * Removing a day is destructive — the reminder it scheduled goes with it — so both
 * routes (the Remove button and the already-ringed mark) ask first, naming the list
 * whose day will go. Nothing is cleared unless the confirm is pressed.
 */
@Composable
fun ListDatePickerDialog(
    listName: String,
    initial: LocalDate?,
    today: LocalDate,
    kind: DateKind,
    animated: Boolean,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
    onKindAsked: (DateKind) -> Unit,
    onKindChange: (DateKind) -> Unit,
    onCleared: () -> Unit
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val said = rememberDateKindSaid()
    val rule = LocalPaperPalette.current.rule
    PaperDialog(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxOf(LocalPagePitch.current, PaperDimens.touchTarget))
                .drawBehind {
                    drawLine(
                        color = rule,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = PaperDimens.rule.toPx()
                    )
                }
        ) {
            DateMarks(
                selection = DateSelection(kind, initial),
                said = said,
                onKindChange = onKindChange,
                onPickDate = onKindAsked,
                onClearDate = { confirmClear = true },
                ringsAskedKind = true,
                trailing = { KindWords(kind = kind, modifier = Modifier.weight(1f)) }
            )
        }
        if (initial != null) {
            val palette = LocalPaperPalette.current
            Row(modifier = Modifier.fillMaxWidth()) {
                DialogButton(
                    label = stringResource(R.string.remove_date),
                    tint = palette.inked(InkTone.Margin),
                    onClick = { confirmClear = true }
                )
                Spacer(Modifier.weight(1f))
            }
        }
        PaperCalendar(
            selected = initial,
            today = today,
            onPick = onPicked,
            animated = animated
        )
        if (confirmClear) {
            RemoveDateConfirmDialog(
                listName = listName,
                onCancel = { confirmClear = false },
                onRemove = {
                    confirmClear = false
                    onCleared()
                }
            )
        }
    }
}

/**
 * Which kind of day is being circled, written where the day itself used to be. The
 * day was written twice on this sheet — once on the rule and once ringed in the
 * month below it — and of the two the ring is the one the reader is looking at. The
 * kind is the thing the sheet cannot show any other way, so it takes the rule.
 *
 * The caption slip this replaces carried the same glyph again beside the words. The
 * ringed glyph two positions to the left already says which kind this is.
 *
 * These words are not seated on the rule the way the day was. The day was a thing
 * written on the line; this is a label on the two glyphs beside it, so it takes a
 * box of their own height, seated where theirs are, and centres itself in it. The
 * row can be taller than those boxes, so centring in the row would miss them.
 */
@Composable
private fun RowScope.KindWords(kind: DateKind, modifier: Modifier) {
    val palette = LocalPaperPalette.current
    Box(
        modifier = modifier
            .align(Alignment.Bottom)
            .height(PaperDimens.iconButton)
            .padding(start = KIND_WORDS_GAP),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = stringResource(
                when (kind) {
                    DateKind.TARGET -> R.string.date_kind_target_caption
                    DateKind.DUE -> R.string.date_kind_due_caption
                }
            ),
            style = PaperType.prose,
            color = palette.inked(InkTone.Words),
            maxLines = KIND_WORDS_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}
