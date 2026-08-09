package fr.mandarine.todolist.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import fr.mandarine.todolist.DailyNotificationWork
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.DailyCheckSchedule
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.SystemClock
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class WorkManagerNotificationScheduler(
    private val context: Context,
    private val clock: Clock = SystemClock()
) : NotificationScheduler {

    override fun scheduleDailyCheck() {
        val initialDelayMillis =
            DailyCheckSchedule.millisUntilNextCheck(clock.now(), ZoneId.systemDefault())
        val request = PeriodicWorkRequestBuilder<DailyNotificationWork>(1, TimeUnit.DAYS)
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
