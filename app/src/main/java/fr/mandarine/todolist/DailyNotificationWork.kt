package fr.mandarine.todolist

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import fr.mandarine.todolist.domain.ComputePendingNotificationsUseCase
import fr.mandarine.todolist.domain.DailyNotificationWorker

class DailyNotificationWork(
    context: Context,
    parameters: WorkerParameters
) : Worker(context, parameters) {

    override fun doWork(): Result {
        val container = (applicationContext as TodoListApplication).container
        DailyNotificationWorker(
            container.todoListRepository,
            ComputePendingNotificationsUseCase(container.clock),
            container.listNotifier
        ).execute()
        return Result.success()
    }
}
