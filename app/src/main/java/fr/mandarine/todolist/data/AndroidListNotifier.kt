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
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns the domain's [ListNotification] values into real Android notifications.
 *
 * Which window a tapped notification opens is a question about the app's shape,
 * not about notifications, so it is answered by whoever assembles the app — [opens]
 * is handed down by `AppContainer`. This layer used to import the window directly,
 * which pointed `data/` at `ui/` and made the two mutually dependent.
 *
 * ### Things worth knowing before changing this
 * - The channel is created on every post. `createNotificationChannel` is idempotent
 *   after the first call, and creating it here rather than at app start means the
 *   channel only exists once there is something to say.
 * - The notification **tag** is the list id and the **id** is a constant. That pair
 *   is what makes notifications unique per list: posting again for the same list
 *   replaces its notification instead of stacking a second one.
 * - `TaskStackBuilder` synthesises a back stack, so backing out of a list opened
 *   from a notification lands on the page of lists rather than leaving the app.
 * - The `data` URI is what makes each `PendingIntent` distinct. `PendingIntent`
 *   equality ignores extras, so without a differing URI (or the differing request
 *   code, which is also supplied) every list would share one intent and every
 *   notification would open the same list.
 * - `FLAG_IMMUTABLE` is required from Android 12; `FLAG_UPDATE_CURRENT` refreshes
 *   the extras of an intent that already exists.
 * - The text is a glyph plus a date, with no words, and the date pattern comes from
 *   `getBestDateTimePattern` so it follows the reader's locale rather than a
 *   hard-coded order.
 */
class AndroidListNotifier(
    private val context: Context,
    private val opens: Class<*>
) : ListNotifier {

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
        val intent = Intent(context, opens).apply {
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
