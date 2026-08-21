package fr.mandarine.todolist.ui.todolists

import android.text.format.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MILLIS_PER_DAY = 86_400_000L

enum class DateKind { TARGET, DUE }

/**
 * A list carries a target date or a due date, never both. Holding one kind and
 * one date instead of two nullable dates is what makes switching kinds carry the
 * date across for free, rather than by moving it between two fields.
 */
data class DateSelection(val kind: DateKind, val date: LocalDate?) {

    val targetDate: LocalDate? get() = date.takeIf { kind == DateKind.TARGET }

    val dueDate: LocalDate? get() = date.takeIf { kind == DateKind.DUE }

    fun withKind(newKind: DateKind): DateSelection = copy(kind = newKind)

    fun withDate(newDate: LocalDate): DateSelection = copy(date = newDate)

    fun cleared(): DateSelection = copy(date = null)

    companion object {
        val None = DateSelection(DateKind.TARGET, null)

        fun of(targetDate: LocalDate?, dueDate: LocalDate?): DateSelection =
            if (dueDate != null) {
                DateSelection(DateKind.DUE, dueDate)
            } else {
                DateSelection(DateKind.TARGET, targetDate)
            }
    }
}

fun formatListDate(date: LocalDate, showYear: Boolean, locale: Locale): String {
    val skeleton = if (showYear) "EEEdMMMy" else "EEEdMMM"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

/**
 * The Material date picker speaks UTC epoch millis; the domain speaks
 * [LocalDate]. Both directions floor rather than truncate so dates before the
 * epoch survive the round trip.
 */
fun LocalDate.toPickerMillis(): Long = toEpochDay() * MILLIS_PER_DAY

fun localDateFromPickerMillis(millis: Long): LocalDate =
    LocalDate.ofEpochDay(Math.floorDiv(millis, MILLIS_PER_DAY))
