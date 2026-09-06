package fr.mandarine.todolist.domain

/**
 * Arranges for the daily check to run. Implemented by
 * [fr.mandarine.todolist.data.WorkManagerNotificationScheduler].
 *
 * Called on every activity launch and again whenever the reminder hour changes;
 * the implementation is idempotent, so calling it repeatedly is cheap and correct.
 * See that class for why it cancels and re-enqueues rather than updating.
 */
fun interface NotificationScheduler {
    fun scheduleDailyCheck()
}
