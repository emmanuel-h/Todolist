package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.PaperCalendar
import fr.mandarine.todolist.ui.paper.PaperSlipCaption
import fr.mandarine.todolist.ui.paper.PaperDialog
import java.time.LocalDate

/**
 * A smaller sheet with a month written on it, laid on whatever sheet asked for a
 * date. There is no confirm row: circling a day is the answer, exactly as ticking
 * a ring is the answer everywhere else on the page, and putting the sheet down
 * leaves the date as it was.
 *
 * The tutorial no longer reaches for a positive button by id — it sets the date
 * on the screen state and this dialog simply closes.
 */
@Composable
fun ListDatePickerDialog(
    initial: LocalDate?,
    today: LocalDate,
    kind: DateKind,
    animated: Boolean,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit
) {
    PaperDialog(onDismissRequest = onDismiss) {
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
