package fr.mandarine.todolist.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.DailyCheckSchedule
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.ReminderTimeRepository
import fr.mandarine.todolist.domain.SystemClock
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class WorkManagerNotificationScheduler(
    private val context: Context,
    private val runs: Class<out ListenableWorker>,
    private val reminderTimeRepository: ReminderTimeRepository,
    private val clock: Clock = SystemClock()
) : NotificationScheduler {

    /**
     * The whole schedule is thrown away and laid again, rather than updated.
     *
     * `KEEP` left a changed hour never taking effect, which is the bug; `UPDATE`
     * looks like the answer and is not — it replaces the request but keeps the
     * period already running, so the new initial delay is ignored and the check
     * still lands at the old hour. Only cancelling and re-enqueuing moves it.
     *
     * Doing that on every launch costs nothing: the delay is computed as the time
     * until the next occurrence of the chosen hour, so laying it again always
     * points at the same moment it already pointed at.
     */
    override fun scheduleDailyCheck() {
        val checkTime = reminderTimeRepository.getReminderTime()
        val initialDelayMillis =
            DailyCheckSchedule.millisUntilNextCheck(clock.now(), ZoneId.systemDefault(), checkTime)
        val request = PeriodicWorkRequest.Builder(runs, 1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request
        )
    }

    companion object {
        const val WORK_NAME = "daily_notification_check"
    }
}
