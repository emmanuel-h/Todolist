package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import java.time.LocalTime

private const val ONE_LINE = 1
private val TITLE_BOTTOM = 8.dp
private val TIME_GLYPH_GAP = 8.dp
private val BUTTON_TOP = 4.dp

/**
 * The settings slip: a paper sheet carrying the hour the daily reminder arrives.
 * The time jot carries a small pen mark at its end so a reader can see by looking
 * that it can be pressed. Pressing it opens the clock, where an hour is circled
 * first and then a minute in five-minute steps, giving any time of day.
 */
@Composable
fun ReminderSettingsDialog(
    reminderTime: LocalTime,
    onSetReminderTime: (Int) -> Unit,
    onDismiss: () -> Unit,
    animated: Boolean = true
) {
    val palette = LocalPaperPalette.current
    var clockOpen by remember { mutableStateOf(false) }
    val locale = formatLocale
    val timeText = rememberFormattedTime(reminderTime, locale)
    val everyDayAt = stringResource(R.string.reminder_every_day_at)
    val timeLabel = "$everyDayAt $timeText"

    PaperDialog(onDismissRequest = onDismiss) {
        Text(
            text = handwritten(stringResource(R.string.reminders_title)),
            style = PaperType.field,
            color = palette.inked(InkTone.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TITLE_BOTTOM)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PaperDimens.touchTarget)
                .selectable(
                    selected = false,
                    role = Role.Button,
                    onClick = { clockOpen = true }
                )
                .semantics { contentDescription = timeLabel },
            verticalAlignment = Alignment.CenterVertically
        ) {
            InkIcon(
                painter = painterResource(R.drawable.ic_alarm),
                contentDescription = null,
                tint = palette.inked(InkTone.Margin)
            )
            Spacer(Modifier.width(TIME_GLYPH_GAP))
            Text(
                text = handwritten(timeLabel),
                style = PaperType.prose,
                color = palette.inked(InkTone.Words),
                maxLines = ONE_LINE,
                modifier = Modifier.weight(1f)
            )
            InkIcon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = null,
                tint = palette.inked(InkTone.Margin),
                size = PaperDimens.jotGlyph
            )
        }
        Spacer(Modifier.height(BUTTON_TOP))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            DialogButton(
                label = stringResource(R.string.done),
                tint = palette.inked(InkTone.Words),
                onClick = onDismiss
            )
        }
    }

    if (clockOpen) {
        ReminderClockPickerDialog(
            currentMinuteOfDay = reminderTime.hour * 60 + reminderTime.minute,
            onMinuteOfDayPicked = { minuteOfDay ->
                onSetReminderTime(minuteOfDay)
                clockOpen = false
            },
            onDismiss = { clockOpen = false },
            animated = animated
        )
    }
}
