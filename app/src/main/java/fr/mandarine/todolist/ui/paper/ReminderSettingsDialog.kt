package fr.mandarine.todolist.ui.paper

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private const val ONE_LINE = 1
private val TITLE_BOTTOM = 8.dp
private val TIME_GLYPH_GAP = 8.dp
private val BUTTON_TOP = 4.dp
private val PARTIAL_BOTTOM = 4.dp

// Clock layout constants
private const val CLOCK_POSITIONS = 12
private const val CLOCK_MINUTE_STEP = 5
private const val CLOCK_RING_FRACTION = 0.72f
private const val CLOCK_FACE_FRACTION = 0.93f
private const val CLOCK_FACE_SEED = 0x1CE
private const val HALF_MORNING_SEED = 0x2A1
private const val HALF_AFTERNOON_SEED = 0x2B7
private val HALF_GAP = 12.dp
private val HALF_PADDING = 14.dp
private val HALF_PADDING_TOP = 6.dp
private const val CLOCK_WOBBLE = 0.012f
private const val CLOCK_RING_FIT = 0.80f
private val CLOCK_FACE_STROKE = 1.75.dp

private const val TWO_PI = 6.2831855f
private const val HALF_PI = 1.5707963f

private enum class ClockPhase { HOURS, MINUTES }

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

/**
 * Two-step clock: the hour is circled first (0–11 outer, 12–23 inner), then the
 * minute in five-minute steps. The time-in-progress shows in the header so the
 * reader can see the time forming before they commit with the second tap.
 */
@Composable
private fun ReminderClockPickerDialog(
    currentMinuteOfDay: Int,
    onMinuteOfDayPicked: (Int) -> Unit,
    onDismiss: () -> Unit,
    animated: Boolean
) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    val currentHour = currentMinuteOfDay / 60
    val currentMinute = currentMinuteOfDay % 60

    var phase by remember { mutableStateOf(ClockPhase.HOURS) }
    var pickedHour by remember { mutableIntStateOf(currentHour) }
    val pickedMinute by remember { mutableIntStateOf(currentMinute) }

    val partialLabel = when (phase) {
        ClockPhase.HOURS -> rememberFormattedTime(LocalTime.of(pickedHour, currentMinute), locale)
        ClockPhase.MINUTES -> "$pickedHour:--"
    }

    PaperDialog(onDismissRequest = onDismiss) {
        Text(
            text = handwritten(stringResource(R.string.reminder_hour_picker_title)),
            style = PaperType.field,
            color = palette.inked(InkTone.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PARTIAL_BOTTOM)
        )
        Text(
            text = handwritten(partialLabel),
            style = PaperType.field,
            color = palette.inked(InkTone.Acted),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PARTIAL_BOTTOM)
        )
        when (phase) {
            ClockPhase.HOURS -> HourFace(
                selectedHour = pickedHour,
                animated = animated,
                palette = palette,
                onHourPicked = { hour ->
                    pickedHour = hour
                    phase = ClockPhase.MINUTES
                }
            )
            ClockPhase.MINUTES -> MinuteFace(
                selectedMinute = pickedMinute,
                animated = animated,
                palette = palette,
                onMinutePicked = { minute -> onMinuteOfDayPicked(pickedHour * 60 + minute) }
            )
        }
    }
}

/**
 * The hour face: twelve numerals on one ring, and a pair beside it saying which
 * half of the day they belong to.
 *
 * It carried twenty-four numerals on two rings first, 0–11 outside and 12–23
 * inside. At this sheet's width the rings sat closer than a finger is wide, so
 * each outer numeral's target overlapped the inner one at the same angle and the
 * inner one — composed later, and therefore on top — took the tap: aiming at 7
 * chose 19. Two targets cannot share that much of the same paper, and no pair of
 * radii fixes it at this size. One ring cannot overlap itself.
 */
@Composable
private fun HourFace(
    selectedHour: Int,
    animated: Boolean,
    palette: PaperPalette,
    onHourPicked: (Int) -> Unit
) {
    val afternoon = selectedHour >= CLOCK_POSITIONS
    var secondHalf by remember(selectedHour) { mutableStateOf(afternoon) }
    Column(modifier = Modifier.fillMaxWidth()) {
        HalfOfDay(
            secondHalf = secondHalf,
            animated = animated,
            palette = palette,
            onChoose = { secondHalf = it }
        )
        ClockFace(
            count = CLOCK_POSITIONS,
            labelOf = { i -> if (i == 0) CLOCK_POSITIONS.toString() else i.toString() },
            seedOf = { i -> i },
            isSelected = { i -> i == selectedHour % CLOCK_POSITIONS && secondHalf == afternoon },
            animated = animated,
            palette = palette,
            onPick = { i -> onHourPicked(if (secondHalf) i + CLOCK_POSITIONS else i) }
        )
    }
}

/**
 * Which half of the day the numeral on the face means. The two are written out
 * rather than drawn, because there is no mark that says "the hours before noon"
 * that a reader would not have to be taught first.
 */
@Composable
private fun HalfOfDay(
    secondHalf: Boolean,
    animated: Boolean,
    palette: PaperPalette,
    onChoose: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = PARTIAL_BOTTOM),
        horizontalArrangement = Arrangement.Center
    ) {
        HalfOfDayMark(
            label = stringResource(R.string.reminder_morning),
            selected = !secondHalf,
            seed = HALF_MORNING_SEED,
            animated = animated,
            palette = palette,
            onChoose = { onChoose(false) }
        )
        Spacer(Modifier.width(HALF_GAP))
        HalfOfDayMark(
            label = stringResource(R.string.reminder_afternoon),
            selected = secondHalf,
            seed = HALF_AFTERNOON_SEED,
            animated = animated,
            palette = palette,
            onChoose = { onChoose(true) }
        )
    }
}

@Composable
private fun HalfOfDayMark(
    label: String,
    selected: Boolean,
    seed: Int,
    animated: Boolean,
    palette: PaperPalette,
    onChoose: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = PaperDimens.touchTarget)
            .selectable(selected = selected, role = Role.Button, onClick = onChoose)
            .semantics { contentDescription = label }
            .padding(horizontal = HALF_PADDING, vertical = HALF_PADDING_TOP),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .circledInInk(
                    circled = selected,
                    seed = seed,
                    color = palette.inked(InkTone.Acted),
                    animated = animated
                )
        )
        Text(
            text = handwritten(label),
            style = PaperType.prose,
            color = palette.inked(if (selected) InkTone.Acted else InkTone.Words),
            maxLines = ONE_LINE
        )
    }
}

/**
 * The minute face: twelve positions at five-minute intervals. The closest five-minute
 * mark to the stored minute is pre-ringed so the current time reads immediately.
 */
@Composable
private fun MinuteFace(
    selectedMinute: Int,
    animated: Boolean,
    palette: PaperPalette,
    onMinutePicked: (Int) -> Unit
) {
    val roundedPos = (selectedMinute + 2) / CLOCK_MINUTE_STEP % CLOCK_POSITIONS
    ClockFace(
        count = CLOCK_POSITIONS,
        labelOf = { i -> (i * CLOCK_MINUTE_STEP).toString() },
        seedOf = { i -> 0x100 + i },
        isSelected = { i -> i == roundedPos },
        animated = animated,
        palette = palette,
        onPick = { i -> onMinutePicked(i * CLOCK_MINUTE_STEP) }
    )
}

/**
 * The ink circle the numerals sit on: twelve selectable boxes placed by angle on
 * one ring. There were two rings once, so twenty-four hours could be shown at once,
 * and at this sheet's width they sat closer than a finger — each target overlapped
 * its neighbour on the other ring and aiming at one numeral chose the other. One
 * ring cannot do that.
 * The face ring is cut once in the cache block — only how much of a numeral's ring
 * is drawn changes per frame.
 */
@Composable
private fun ClockFace(
    count: Int,
    labelOf: (Int) -> String,
    seedOf: (Int) -> Int,
    isSelected: (Int) -> Boolean,
    animated: Boolean,
    palette: PaperPalette,
    onPick: (Int) -> Unit
) {
    val faceColor = palette.rule
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawWithCache {
                val faceDiam = size.minDimension * CLOCK_FACE_FRACTION
                val faceSize = Size(faceDiam, faceDiam)
                val jitter = faceDiam * CLOCK_WOBBLE
                val faceRing = ringPath(faceSize, seed = CLOCK_FACE_SEED, jitter = jitter)
                val faceNib = InkNib(CLOCK_FACE_STROKE.toPx())
                val dx = (size.width - faceDiam) / 2
                val dy = (size.height - faceDiam) / 2
                onDrawBehind {
                    withTransform({ translate(dx, dy) }) {
                        inked(faceRing, faceColor, faceNib)
                    }
                }
            }
    ) {
        val halfDiam = maxWidth / 2
        val ringR = halfDiam * CLOCK_RING_FRACTION
        val touchHalf = PaperDimens.touchTarget / 2

        for (i in 0 until count) {
            val pos = i % CLOCK_POSITIONS
            val angle = pos.toFloat() / CLOCK_POSITIONS.toFloat() * TWO_PI - HALF_PI
            val x = halfDiam + ringR * cos(angle) - touchHalf
            val y = halfDiam + ringR * sin(angle) - touchHalf

            Box(modifier = Modifier.absoluteOffset(x = x, y = y)) {
                ClockNumeral(
                    label = labelOf(i),
                    selected = isSelected(i),
                    seed = seedOf(i),
                    animated = animated,
                    palette = palette,
                    onPick = { onPick(i) }
                )
            }
        }
    }
}

@Composable
private fun ClockNumeral(
    label: String,
    selected: Boolean,
    seed: Int,
    animated: Boolean,
    palette: PaperPalette,
    onPick: () -> Unit
) {
    val ringSize = PaperDimens.touchTarget * CLOCK_RING_FIT
    Box(
        modifier = Modifier
            .size(PaperDimens.touchTarget)
            .selectable(
                selected = selected,
                role = Role.Button,
                onClick = onPick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(ringSize)
                .circledInInk(
                    circled = selected,
                    seed = seed,
                    color = palette.inked(InkTone.Acted),
                    animated = animated
                )
        )
        Text(
            text = label,
            style = LocalRuledHand.current.margin,
            color = palette.inked(if (selected) InkTone.Acted else InkTone.Words),
            maxLines = ONE_LINE
        )
    }
}

@Composable
internal fun rememberFormattedTime(time: LocalTime, locale: Locale): String {
    // "j" is the locale's own preferred hour field, so a French reader is shown
    // 20:00 and an en-US one 8 PM. "HH" would force twenty-four hours on everyone.
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "jm") }
    val formatter = remember(locale, pattern) { DateTimeFormatter.ofPattern(pattern, locale) }
    return remember(time, formatter) { time.format(formatter) }
}
