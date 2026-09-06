package fr.mandarine.todolist.domain

/**
 * The daily check, as three plain calls: read the lists, decide what is worth
 * saying, say it.
 *
 * This is the whole of the job and it has no Android in it, so it is unit-testable
 * with three fakes. The Android `Worker` that WorkManager actually instantiates is
 * [fr.mandarine.todolist.DailyNotificationWork], up at the root of the package; all
 * it does is pull these three collaborators out of `AppContainer` and call
 * [execute]. Keeping the two apart is what lets this be covered by the quality
 * gates, which cannot instrument a `Worker`.
 */
class DailyNotificationWorker(
    private val todoListRepository: TodoListRepository,
    private val computeUseCase: ComputePendingNotificationsUseCase,
    private val listNotifier: ListNotifier,
    private val notificationScheduler: NotificationScheduler
) {
    /**
     * Posting is only half of a run. The other half is aiming the next one at the
     * next occurrence of the reader's hour in the zone the device is in now, which
     * is what keeps a twenty-four hour period from walking off the hour once a
     * daylight-saving change or a deferred run has moved it.
     */
    fun execute() {
        val lists = todoListRepository.getAll()
        listNotifier.postNotifications(computeUseCase(lists))
        notificationScheduler.rescheduleDailyCheck()
    }
}
