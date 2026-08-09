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
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

class AppContainer(
    private val context: Context,
    private val databaseFactory: (Context) -> TodoDatabase = TodoDatabase::getInstance,
    private val schedulerFactory: (Context) -> NotificationScheduler = { WorkManagerNotificationScheduler(it) },
    val databaseDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
) {
    val clock: Clock = SystemClock()
    private val database by lazy { databaseFactory(context) }
    val todoListRepository: TodoListRepository by lazy { RoomTodoListRepository(database.todoListDao()) }
    val todoRepository: TodoRepository by lazy { RoomTodoRepository(database.todoItemDao(), clock) }
    val listNotifier: ListNotifier by lazy { AndroidListNotifier(context) }
    val notificationScheduler: NotificationScheduler by lazy { schedulerFactory(context) }
}
