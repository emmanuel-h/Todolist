package fr.mandarine.todolist.domain

import java.time.LocalTime

/**
 * Arms the daily checks that post list reminders.
 *
 * The two methods differ only in what they are allowed to disturb, and the
 * difference is the whole point:
 *
 * - [ensureDailyChecks] is what a launch calls. It arms the app-wide check and one
 *   check per distinct own time if nothing is already armed for that slot, leaving
 *   any pending run exactly where it is. It also cancels own-time checks for times
 *   that are no longer in [ownTimes]. A check the device deferred past its hour is
 *   still going to run, and cancelling it because the reader happened to open the
 *   app is how a list due today loses its only notification.
 * - [rescheduleDailyCheck] is what a changed hour calls, and what the check itself
 *   calls once it has run. It moves the named slot's next run to the next occurrence
 *   of the chosen hour *in the current zone*, which is what keeps a fixed twenty-four
 *   hour period from walking off the hour after a daylight-saving change or a
 *   deferred run.
 */
interface NotificationScheduler {
    fun ensureDailyChecks(ownTimes: Set<LocalTime>)

    fun rescheduleDailyCheck(slot: ReminderSlot)
}
