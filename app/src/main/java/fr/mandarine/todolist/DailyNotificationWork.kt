package fr.mandarine.todolist

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import fr.mandarine.todolist.domain.ComputePendingNotificationsUseCase
import fr.mandarine.todolist.domain.DailyNotificationWorker

/**
 * The Android end of the daily check — the class WorkManager actually instantiates.
 *
 * It is a thin shell on purpose. WorkManager constructs this itself, with a
 * signature it dictates, so nothing here can be injected and nothing here can be
 * unit-tested under the project's quality gates. All it does is pull the three
 * collaborators out of the [AppContainer] and hand them to
 * [DailyNotificationWorker], which is plain Kotlin in `domain/` and carries all of
 * the actual logic.
 *
 * Note the near-identical names: `DailyNotificationWork` (here, Android) versus
 * `DailyNotificationWorker` (domain, testable).
 *
 * `doWork()` runs on a WorkManager background thread, so the repository's blocking
 * calls are fine here without a dispatcher. It lives at the root of the package
 * rather than in `data/` because it depends on both the container and the domain,
 * which is a composition-root job.
 */
class DailyNotificationWork(
    context: Context,
    parameters: WorkerParameters
) : Worker(context, parameters) {

    override fun doWork(): Result {
        val container = (applicationContext as TodoListApplication).container
        DailyNotificationWorker(
            container.todoListRepository,
            ComputePendingNotificationsUseCase(container.clock),
            container.listNotifier,
            container.notificationScheduler
        ).execute()
        return Result.success()
    }
}
