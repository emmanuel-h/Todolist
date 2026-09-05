package fr.mandarine.todolist.ui.paper

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val HOUR_ROWS = 4
private const val HOUR_COLUMNS = 6
private const val RING_SPREAD = 1.55f
private const val RING_FIT = 0.9f
private const val ONE_LINE = 1
private val TITLE_BOTTOM = 8.dp
private val TIME_GLYPH_GAP = 8.dp
private val BUTTON_TOP = 4.dp

/**
 * The settings slip: a paper sheet carrying the hour the daily reminder arrives.
 * Pressing the time jot opens the hour grid where a new hour can be circled.
 */
@Composable
fun ReminderSettingsDialog(
    reminderTime: LocalTime,
    onSetReminderTime: (Int) -> Unit,
    onDismiss: () -> Unit,
    animated: Boolean = true
) {
    val palette = LocalPaperPalette.current
    var hourPickerOpen by remember { mutableStateOf(false) }
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
                    onClick = { hourPickerOpen = true }
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
                maxLines = ONE_LINE
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

    if (hourPickerOpen) {
        ReminderHourPickerDialog(
            currentHour = reminderTime.hour,
            onHourPicked = { hour ->
                onSetReminderTime(hour * 60)
                hourPickerOpen = false
            },
            onDismiss = { hourPickerOpen = false },
            animated = animated
        )
    }
}

/**
 * The hour grid: 24 cells, 6 across, each numbered 0–23. The chosen hour is
 * circled in the same ink ring the calendar throws around a chosen day.
 */
@Composable
private fun ReminderHourPickerDialog(
    currentHour: Int,
    onHourPicked: (Int) -> Unit,
    onDismiss: () -> Unit,
    animated: Boolean
) {
    val palette = LocalPaperPalette.current
    val pitch = LocalPagePitch.current
    val cellHeight = maxOf(pitch, PaperDimens.touchTarget)

    PaperDialog(onDismissRequest = onDismiss) {
        Text(
            text = handwritten(stringResource(R.string.reminder_hour_picker_title)),
            style = PaperType.field,
            color = palette.inked(InkTone.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TITLE_BOTTOM)
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val glyph = with(LocalDensity.current) {
                LocalRuledHand.current.itemLine.fontSize.toDp()
            }
            val ring = minOf(glyph * RING_SPREAD, maxWidth / HOUR_COLUMNS * RING_FIT)
            Column(modifier = Modifier.fillMaxWidth()) {
                repeat(HOUR_ROWS) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellHeight)
                            .hourRuleUnder(palette.rule),
                        verticalAlignment = Alignment.Top
                    ) {
                        repeat(HOUR_COLUMNS) { col ->
                            val hour = row * HOUR_COLUMNS + col
                            HourCell(
                                hour = hour,
                                selected = hour == currentHour,
                                ring = ring,
                                glyph = glyph,
                                cellHeight = cellHeight,
                                animated = animated,
                                palette = palette,
                                onPick = { onHourPicked(hour) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.HourCell(
    hour: Int,
    selected: Boolean,
    ring: Dp,
    glyph: Dp,
    cellHeight: Dp,
    animated: Boolean,
    palette: PaperPalette,
    onPick: () -> Unit
) {
    val label = hour.toString()
    Box(
        modifier = Modifier
            .weight(1f)
            .height(cellHeight)
            .selectable(
                selected = selected,
                role = Role.Button,
                onClick = onPick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = LocalPagePitch.current - glyph / 2 - ring / 2)
                .size(ring)
                .circledInInk(
                    circled = selected,
                    seed = hour,
                    color = palette.inked(InkTone.Acted),
                    animated = animated
                )
        )
        Text(
            text = label,
            modifier = Modifier.seatOnRule(),
            style = LocalRuledHand.current.itemLine,
            color = palette.inked(InkTone.Words),
            maxLines = ONE_LINE
        )
    }
}

private fun Modifier.hourRuleUnder(color: Color): Modifier =
    drawBehind {
        val thickness = PaperDimens.rule.toPx()
        drawRect(
            color = color,
            topLeft = Offset(0f, size.height - thickness),
            size = Size(size.width, thickness)
        )
    }

@Composable
internal fun rememberFormattedTime(time: LocalTime, locale: Locale): String {
    // "j" is the locale's own preferred hour field, so a French reader is shown
    // 20:00 and an en-US one 8 PM. "HH" would force twenty-four hours on everyone.
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "jm") }
    val formatter = remember(locale, pattern) { DateTimeFormatter.ofPattern(pattern, locale) }
    return remember(time, formatter) { time.format(formatter) }
}
