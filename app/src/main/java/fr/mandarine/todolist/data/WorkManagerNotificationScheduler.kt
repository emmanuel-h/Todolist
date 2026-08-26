package fr.mandarine.todolist.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.DailyCheckSchedule
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.SystemClock
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Which worker runs the daily check is a question about the app's shape, not
 * about scheduling, so it is answered by whoever assembles the app rather than
 * imported from the composition root into this layer.
 */
class WorkManagerNotificationScheduler(
    private val context: Context,
    private val runs: Class<out ListenableWorker>,
    private val clock: Clock = SystemClock()
) : NotificationScheduler {

    override fun scheduleDailyCheck() {
        val initialDelayMillis =
            DailyCheckSchedule.millisUntilNextCheck(clock.now(), ZoneId.systemDefault())
        val request = PeriodicWorkRequest.Builder(runs, 1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val WORK_NAME = "daily_notification_check"
    }
}
