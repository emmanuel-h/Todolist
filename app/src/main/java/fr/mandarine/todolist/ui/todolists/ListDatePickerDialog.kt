package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperCalendar
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperSlipCaption
import fr.mandarine.todolist.ui.paper.PaperDialog
import java.time.LocalDate

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
 */
@Composable
fun ListDatePickerDialog(
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
    val said = rememberDateKindSaid()
    val rule = LocalPaperPalette.current.rule
    PaperDialog(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LocalPagePitch.current)
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
                onClearDate = onCleared
            )
        }
        /**
         * The sheet says which kind of day is being circled, because this is where
         * the reader is looking when it matters. The rule the marks sit on is
         * behind this sheet by then, so a caption raised there would be telling
         * the reader something they cannot see.
         */
        PaperSlipCaption(
            painter = painterResource(
                when (kind) {
                    DateKind.TARGET -> R.drawable.ic_event
                    DateKind.DUE -> R.drawable.ic_alarm
                }
            ),
            text = stringResource(
                when (kind) {
                    DateKind.TARGET -> R.string.date_kind_target_caption
                    DateKind.DUE -> R.string.date_kind_due_caption
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        PaperCalendar(
            selected = initial,
            today = today,
            onPick = onPicked,
            animated = animated
        )
    }
}
