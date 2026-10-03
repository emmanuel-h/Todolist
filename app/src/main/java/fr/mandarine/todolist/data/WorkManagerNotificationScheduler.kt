package fr.mandarine.todolist.data

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.DailyCheckSchedule
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.ReminderSlot
import fr.mandarine.todolist.domain.ReminderTimeRepository
import fr.mandarine.todolist.domain.SystemClock
import java.time.LocalTime
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
 *
 * ### App-wide vs own-time checks
 * The app-wide check keeps the existing unique name [WORK_NAME] and carries no
 * input data, so work already enqueued on existing installs continues to mean
 * app-wide. Own-time checks use the name `[WORK_NAME]@<minuteOfDay>` and carry
 * the minute in input data under the key [SLOT_MINUTE_KEY]. They also carry a
 * per-minute tag `[OWN_TIME_TAG]_<minuteOfDay>` and the shared [OWN_TIME_TAG],
 * so stale ones can be found by the shared tag and then identified by their
 * per-minute tag.
 */
class WorkManagerNotificationScheduler(
    private val context: Context,
    private val runs: Class<out ListenableWorker>,
    private val reminderTimeRepository: ReminderTimeRepository,
    private val clock: Clock = SystemClock()
) : NotificationScheduler {

    override fun ensureDailyChecks(ownTimes: Set<LocalTime>) {
        enqueue(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, appWideRequest())
        ownTimes.forEach { time ->
            val minuteOfDay = time.hour * 60 + time.minute
            enqueue(ownTimeName(minuteOfDay), ExistingPeriodicWorkPolicy.KEEP, ownTimeRequest(minuteOfDay, time))
        }
        cancelStaleOwnTimeChecks(ownTimes)
    }

    override fun rescheduleDailyCheck(slot: ReminderSlot) {
        when (slot) {
            ReminderSlot.AppWide -> enqueue(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, appWideRequest())
            is ReminderSlot.At -> {
                val minuteOfDay = slot.time.hour * 60 + slot.time.minute
                enqueue(ownTimeName(minuteOfDay), ExistingPeriodicWorkPolicy.UPDATE, ownTimeRequest(minuteOfDay, slot.time))
            }
        }
    }

    private fun cancelStaleOwnTimeChecks(ownTimes: Set<LocalTime>) {
        val activeMinutes = ownTimes.map { it.hour * 60 + it.minute }.toSet()
        val wm = WorkManager.getInstance(context)
        wm.getWorkInfosByTag(OWN_TIME_TAG).get()
            .filter { it.state != WorkInfo.State.CANCELLED && it.state != WorkInfo.State.SUCCEEDED && it.state != WorkInfo.State.FAILED }
            .forEach { info ->
                val minute = minuteFromTags(info.tags)
                if (minute != null && minute !in activeMinutes) {
                    wm.cancelUniqueWork(ownTimeName(minute))
                }
            }
    }

    private fun minuteFromTags(tags: Set<String>): Int? {
        val prefix = "${OWN_TIME_TAG}_"
        return tags.firstOrNull { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.toIntOrNull()
    }

    private fun enqueue(name: String, policy: ExistingPeriodicWorkPolicy, request: PeriodicWorkRequest) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(name, policy, request)
    }

    private fun appWideRequest(): PeriodicWorkRequest {
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

    private fun ownTimeRequest(minuteOfDay: Int, time: LocalTime): PeriodicWorkRequest {
        val now = clock.now()
        val untilNextCheck = DailyCheckSchedule.millisUntilNextCheck(now, ZoneId.systemDefault(), time)
        return PeriodicWorkRequest.Builder(runs, 1, TimeUnit.DAYS)
            .setNextScheduleTimeOverride(now + untilNextCheck)
            .setInputData(Data.Builder().putInt(SLOT_MINUTE_KEY, minuteOfDay).build())
            .addTag(OWN_TIME_TAG)
            .addTag("${OWN_TIME_TAG}_$minuteOfDay")
            .build()
    }

    private fun ownTimeName(minuteOfDay: Int) = "$WORK_NAME@$minuteOfDay"

    companion object {
        const val WORK_NAME = "daily_notification_check"
        const val OWN_TIME_TAG = "daily_notification_check_own"
        const val SLOT_MINUTE_KEY = "slot_minute"
    }
}
