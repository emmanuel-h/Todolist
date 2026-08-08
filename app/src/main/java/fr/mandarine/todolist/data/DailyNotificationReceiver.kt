package fr.mandarine.todolist.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.mandarine.todolist.domain.ComputePendingNotificationsUseCase
import fr.mandarine.todolist.domain.DailyNotificationWorker
import fr.mandarine.todolist.domain.SystemClock

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val clock = SystemClock()
        DailyNotificationWorker(
            RoomTodoListRepository(TodoDatabase.getInstance(context).todoListDao()),
            ComputePendingNotificationsUseCase(clock),
            AndroidListNotifier(context),
            AndroidNotificationScheduler(context, clock)
        ).execute()
    }
}
