package fr.mandarine.todolist.data

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.ListNotification
import fr.mandarine.todolist.domain.ListNotifier
import fr.mandarine.todolist.ui.TodoListActivity

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
            .setContentText(context.getString(notificationText(notification)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun notificationText(notification: ListNotification): Int = when (notification) {
        is ListNotification.DueDateToday -> R.string.notification_due_today
        is ListNotification.TargetDateTomorrow -> R.string.notification_target_tomorrow
    }

    companion object {
        const val CHANNEL_ID = "todo_reminders"
        const val NOTIFICATION_ID = 1
    }
}
