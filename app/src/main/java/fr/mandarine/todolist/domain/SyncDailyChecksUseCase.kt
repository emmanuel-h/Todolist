package fr.mandarine.todolist.domain

/**
 * Reconciles the set of WorkManager daily checks with what the lists actually need.
 *
 * Reads all lists and collects the distinct own times ([TodoList.reminderTime] is
 * non-null), then calls [NotificationScheduler.ensureDailyChecks] with that set.
 * The scheduler arms one check per distinct time (plus the app-wide one) and
 * cancels any own-time checks whose time is no longer in use.
 */
class SyncDailyChecksUseCase(
    private val todoListRepository: TodoListRepository,
    private val notificationScheduler: NotificationScheduler
) {
    operator fun invoke() {
        val ownTimes = todoListRepository.getAll().mapNotNull { it.reminderTime }.toSet()
        notificationScheduler.ensureDailyChecks(ownTimes)
    }
}
