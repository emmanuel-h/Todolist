package fr.mandarine.todolist

import android.content.Context
import fr.mandarine.todolist.data.AndroidListNotifier
import fr.mandarine.todolist.data.RoomTodoListRepository
import fr.mandarine.todolist.data.RoomTodoRepository
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.data.WorkManagerNotificationScheduler
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.ListNotifier
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.SystemClock
import fr.mandarine.todolist.domain.TodoListRepository
import fr.mandarine.todolist.domain.TodoRepository
import fr.mandarine.todolist.ui.TodoListsActivity
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

class AppContainer(
    private val context: Context,
    private val databaseFactory: (Context) -> TodoDatabase = TodoDatabase::getInstance,
    private val schedulerFactory: (Context) -> NotificationScheduler = {
        WorkManagerNotificationScheduler(it, DailyNotificationWork::class.java)
    },
    private val opensOnTap: Class<*> = TodoListsActivity::class.java,
    val databaseDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
) {
    /**
     * Where a write goes when the page that asked for it is already gone.
     *
     * A row torn off the paper is the reader's decision the moment it tears; the
     * undo slip that follows is a grace period, not a maybe. Left on the page's
     * own coroutine, walking back out of a list during those nine seconds took
     * the delete with it and the row came back on the next read. The page of
     * items is scoped to its entry on the back stack, so its own scope is exactly
     * the thing that cannot be trusted to outlive the decision.
     */
    val writeScope: CoroutineScope = CoroutineScope(SupervisorJob() + databaseDispatcher)

    val clock: Clock = SystemClock()
    private val database by lazy { databaseFactory(context) }
    val todoListRepository: TodoListRepository by lazy { RoomTodoListRepository(database.todoListDao()) }
    val todoRepository: TodoRepository by lazy { RoomTodoRepository(database.todoItemDao(), clock) }
    val listNotifier: ListNotifier by lazy { AndroidListNotifier(context, opensOnTap) }
    val notificationScheduler: NotificationScheduler by lazy { schedulerFactory(context) }
}
