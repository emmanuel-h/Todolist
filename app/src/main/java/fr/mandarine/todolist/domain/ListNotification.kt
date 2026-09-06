package fr.mandarine.todolist.domain

/**
 * A notification the app has decided to post, before anything Android-specific is
 * built out of it.
 *
 * `sealed class` means the set of subclasses is closed and known at compile time,
 * so a `when` over one needs no `else` branch — add a third kind here and every
 * `when` that handles these stops compiling until it is updated. That is the point:
 * it is a checklist the compiler keeps.
 *
 * The two kinds differ in which date fired them, and that difference survives all
 * the way to the notification text (a `⏰` for the hard date, a `📅` for the soft
 * one) in [fr.mandarine.todolist.data.AndroidListNotifier].
 */
sealed class ListNotification {
    abstract val list: TodoList

    /** The list's hard date is today. */
    data class DueDateToday(override val list: TodoList) : ListNotification()

    /** The list's soft date is tomorrow — a nudge the day before. */
    data class TargetDateTomorrow(override val list: TodoList) : ListNotification()
}
