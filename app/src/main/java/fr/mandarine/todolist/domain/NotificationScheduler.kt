package fr.mandarine.todolist.domain

/**
 * Arms the daily check that posts a list's reminder.
 *
 * The two methods differ only in what they are allowed to disturb, and the
 * difference is the whole point:
 *
 * - [ensureDailyCheck] is what a launch calls. It arms the check if nothing is
 *   armed and otherwise leaves the pending one exactly where it is. A check that
 *   the device deferred past its hour is still going to run, and cancelling it
 *   because the reader happened to open the app is how a list due today loses its
 *   only notification.
 * - [rescheduleDailyCheck] is what a changed hour calls, and what the check itself
 *   calls once it has run. It moves the next run to the next occurrence of the
 *   chosen hour *in the current zone*, which is what keeps a fixed twenty-four
 *   hour period from walking off the hour after a daylight-saving change or a
 *   deferred run.
 */
interface NotificationScheduler {
    fun ensureDailyCheck()

    fun rescheduleDailyCheck()
}
