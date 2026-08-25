package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.Composable
import fr.mandarine.todolist.ui.paper.PaperCalendar
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
    animated: Boolean,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit
) {
    PaperDialog(onDismissRequest = onDismiss) {
        PaperCalendar(
            selected = initial,
            today = today,
            onPick = onPicked,
            animated = animated
        )
    }
}
