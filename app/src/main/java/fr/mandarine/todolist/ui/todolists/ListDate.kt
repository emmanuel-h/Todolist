package fr.mandarine.todolist.ui.todolists

import android.text.format.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

/**
 * A due date is what a notification is for, so the ask belongs to the moment one
 * starts existing — whichever hand wrote it. Circling a day while the alarm is
 * ringed and ringing the alarm over a day already written are the same event to
 * the reader, and both have to be answered for.
 */
internal fun dueDateWritten(before: DateSelection, after: DateSelection): Boolean =
    after.dueDate != null && after.dueDate != before.dueDate

fun formatListDate(date: LocalDate, showYear: Boolean, locale: Locale): String {
    val skeleton = if (showYear) "EEEdMMMy" else "EEEdMMM"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}
