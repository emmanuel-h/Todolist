package fr.mandarine.todolist.data

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.ListNotification
import fr.mandarine.todolist.domain.ListNotifier
import fr.mandarine.todolist.ui.TodoListActivity
import java.time.format.DateTimeFormatter
import java.util.Locale

class AndroidListNotifier(private val context: Context) : ListNotifier {

    override fun postNotifications(notifications: List<ListNotification>) {
        if (notifications.isEmpty()) return
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_name))
                .build()
        )
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notifications.forEach { notification ->
            notificationManager.notify(notification.list.id, NOTIFICATION_ID, build(notification))
        }
    }

    private fun build(notification: ListNotification): android.app.Notification {
        val list = notification.list
        val intent = Intent(context, TodoListActivity::class.java).apply {
            data = Uri.parse("todolist://list/" + list.id)
            putExtra("LIST_ID", list.id)
            putExtra("LIST_NAME", list.name)
        }
        val pendingIntent = requireNotNull(
            TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(
                    list.id.hashCode(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_checklist)
            .setContentTitle(list.name)
            .setContentText(contentText(notification))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun contentText(notification: ListNotification): String {
        val emoji = when (notification) {
            is ListNotification.DueDateToday -> "⏰"
            is ListNotification.TargetDateTomorrow -> "📅"
        }
        val date = when (notification) {
            is ListNotification.DueDateToday -> requireNotNull(notification.list.dueDate)
            is ListNotification.TargetDateTomorrow -> requireNotNull(notification.list.targetDate)
        }
        val locale = Locale.getDefault(Locale.Category.FORMAT)
        val pattern = DateFormat.getBestDateTimePattern(locale, "dM")
        return emoji + " " + date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    companion object {
        const val CHANNEL_ID = "todo_reminders"
        const val NOTIFICATION_ID = 1
    }
}
