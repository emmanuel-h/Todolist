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

/**
 * The WorkManager end of the daily check.
 *
 * ### Why the next run is an absolute moment, not a delay
 * A `PeriodicWorkRequest` repeats every twenty-four hours of elapsed time, and
 * WorkManager works out each run from the previous one. That is not the same as
 * "every day at eight": an hour lost to a daylight-saving change, or a run the
 * device deferred while dozing, moves every following run by the same amount and
 * it never comes back. `setNextScheduleTimeOverride` names the moment instead of
 * the gap, so each run is aimed at the next occurrence of the chosen hour in
 * whatever zone the device is in *now*. The check re-aims itself after it runs,
 * which is what stops the drift accumulating.
 *
 * ### Why the two policies
 * `KEEP` leaves a pending run alone — including one that is already late — so
 * opening the app cannot cancel a check the device merely has not got to yet.
 * `UPDATE` keeps the same work and moves its next run, which is what a changed
 * hour needs; it does not cancel, so the check may call it from inside its own
 * run and have the new moment apply to the next one.
 */
class WorkManagerNotificationScheduler(
    private val context: Context,
    private val runs: Class<out ListenableWorker>,
    private val reminderTimeRepository: ReminderTimeRepository,
    private val clock: Clock = SystemClock()
) : NotificationScheduler {

    override fun ensureDailyCheck() {
        enqueue(ExistingPeriodicWorkPolicy.KEEP)
    }

    override fun rescheduleDailyCheck() {
        enqueue(ExistingPeriodicWorkPolicy.UPDATE)
    }

    private fun enqueue(policy: ExistingPeriodicWorkPolicy) {
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, policy, request())
    }

    private fun request(): PeriodicWorkRequest {
        val now = clock.now()
        val untilNextCheck = DailyCheckSchedule.millisUntilNextCheck(
            now,
            ZoneId.systemDefault(),
            reminderTimeRepository.getReminderTime()
        )
        return PeriodicWorkRequest.Builder(runs, 1, TimeUnit.DAYS)
            .setNextScheduleTimeOverride(now + untilNextCheck)
            .build()
    }

    companion object {
        const val WORK_NAME = "daily_notification_check"
    }
}
