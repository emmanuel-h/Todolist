package fr.mandarine.todolist.ui.todolists

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import java.time.LocalDate

/**
 * The tutorial no longer reaches for a positive button by id — it sets the date
 * on the screen state and this dialog simply closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDatePickerDialog(
    initial: LocalDate?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: today).toPickerMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            InkIconButton(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.save),
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis == null) onDismiss() else onPicked(localDateFromPickerMillis(millis))
                },
                tint = LocalPaperPalette.current.inkBlue
            )
        },
        dismissButton = {
            InkIconButton(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.cancel),
                onClick = onDismiss,
                tint = LocalPaperPalette.current.inkSoft
            )
        }
    ) {
        DatePicker(state = state, title = null)
    }
}
