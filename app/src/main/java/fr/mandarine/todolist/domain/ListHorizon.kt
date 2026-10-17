package fr.mandarine.todolist.domain

import java.time.LocalDate

private const val WEEK_AHEAD_DAYS = 6L

/**
 * How far off a list's day is, read the way an agenda reads it: missed, today,
 * within the coming week, further out, or not dated at all. The declaration order
 * is the order the page of lists draws its sections in.
 *
 * It does not care which kind of date the list carries. A target date and a due
 * date both say "this list is about that day"; the ink a date is written in is
 * what tells the two apart.
 */
enum class ListHorizon {
    OVERDUE,
    TODAY,
    THIS_WEEK,
    LATER,
    UNDATED;

    companion object {
        fun of(day: LocalDate?, today: LocalDate): ListHorizon = when {
            day == null -> UNDATED
            day.isBefore(today) -> OVERDUE
            day == today -> TODAY
            !day.isAfter(today.plusDays(WEEK_AHEAD_DAYS)) -> THIS_WEEK
            else -> LATER
        }
    }
}
