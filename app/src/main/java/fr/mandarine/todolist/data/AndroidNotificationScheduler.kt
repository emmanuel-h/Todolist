package fr.mandarine.todolist.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.SystemClock
import java.util.Calendar

class AndroidNotificationScheduler(
    private val context: Context,
    private val clock: Clock = SystemClock()
) : NotificationScheduler {

    override fun scheduleNextDailyCheck() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next8AM(), pendingIntent)
    }

    private fun next8AM(): Long {
        val nowMillis = clock.now()
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
