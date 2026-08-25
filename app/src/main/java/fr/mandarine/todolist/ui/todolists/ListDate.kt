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

private const val SPOKEN_SKELETON = "EEEdMMM"
private const val SPOKEN_YEAR_SKELETON = "EEEdMMMy"

/**
 * Resolving a skeleton against a locale and compiling the pattern it yields costs
 * more than writing the date does, and the answer is the same for every row on the
 * page — so the hand is asked for once and kept.
 */
fun listDateFormatter(locale: Locale, showYear: Boolean): DateTimeFormatter {
    val skeleton = if (showYear) SPOKEN_YEAR_SKELETON else SPOKEN_SKELETON
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

fun formatListDate(date: LocalDate, showYear: Boolean, locale: Locale): String =
    date.format(listDateFormatter(locale, showYear))
