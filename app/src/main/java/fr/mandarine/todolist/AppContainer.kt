package fr.mandarine.todolist

import android.content.Context
import fr.mandarine.todolist.data.AndroidListNotifier
import fr.mandarine.todolist.data.RoomTodoListRepository
import fr.mandarine.todolist.data.RoomTodoRepository
import fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepository
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.data.WorkManagerNotificationScheduler
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.GetReminderTimeUseCase
import fr.mandarine.todolist.domain.ListNotifier
import fr.mandarine.todolist.domain.NotificationScheduler
import fr.mandarine.todolist.domain.ReminderTimeRepository
import fr.mandarine.todolist.domain.SetReminderTimeUseCase
import fr.mandarine.todolist.domain.SystemClock
import fr.mandarine.todolist.domain.TodoListRepository
import fr.mandarine.todolist.domain.TodoRepository
import fr.mandarine.todolist.ui.TodoListsActivity
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * The composition root: the one place that knows which concrete class implements
 * each domain interface. Hand-rolled dependency injection — no Hilt, no Dagger.
 *
 * Everything above this is written against interfaces
 * ([TodoRepository], [Clock], [ListNotifier]…), so this file is where the arrows
 * that point *downhill* through the layers get resolved. It is also, deliberately,
 * the only file that is allowed to know about all of `data/`, `domain/` and `ui/`
 * at once: `AndroidListNotifier` needs to know which window a notification opens
 * and `WorkManagerNotificationScheduler` needs to know which worker runs, and both
 * of those facts are handed **down** from here rather than imported **up** from
 * `data/`. That is the rule the architecture section of `CLAUDE.md` is protecting.
 *
 * ### `by lazy`
 * Each property is built on first access and then cached — Kotlin's `lazy` is
 * thread-safe by default. Nothing is constructed at app start, so opening the
 * database is deferred until something actually reads it, and the worker running
 * with no UI never builds the notifier it does not use.
 *
 * ### The two constructor seams
 * [databaseFactory], [schedulerFactory] and [opensOnTap] are parameters with
 * defaults so a test can substitute an in-memory database or a no-op scheduler
 * without a mocking framework. Production code constructs `AppContainer(this)` in
 * [TodoListApplication] and never mentions them.
 *
 * ### The threads
 * [databaseDispatcher] is a **single-threaded** executor. Room's calls in this app
 * are blocking, so everything that touches the database is launched on this
 * dispatcher — and because it is one thread, writes are automatically serialised
 * against each other and against reads.
 *
 * [writeScope] is a `CoroutineScope` that lives as long as the process, not as long
 * as a screen. `SupervisorJob` means one failed write does not cancel the others.
 * This is the scope deletes are launched on: a delete confirmed behind an undo slip
 * has to complete even if the reader rotates the device or walks off the page
 * before it lands, and a `viewModelScope` would be cancelled out from under it. See
 * `TodoListViewModel.deleteTodo`.
 */
class AppContainer(
    private val context: Context,
    private val databaseFactory: (Context) -> TodoDatabase = TodoDatabase::getInstance,
    private val schedulerFactory: (Context, ReminderTimeRepository) -> NotificationScheduler = { ctx, repo ->
        WorkManagerNotificationScheduler(ctx, DailyNotificationWork::class.java, repo)
    },
    private val opensOnTap: Class<*> = TodoListsActivity::class.java,
    val databaseDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
) {
    val writeScope: CoroutineScope = CoroutineScope(SupervisorJob() + databaseDispatcher)

    val clock: Clock = SystemClock()
    private val database by lazy { databaseFactory(context) }
    val todoListRepository: TodoListRepository by lazy { RoomTodoListRepository(database.todoListDao()) }
    val todoRepository: TodoRepository by lazy { RoomTodoRepository(database.todoItemDao(), clock) }
    val listNotifier: ListNotifier by lazy { AndroidListNotifier(context, opensOnTap) }
    val reminderTimeRepository: ReminderTimeRepository by lazy { SharedPreferencesReminderTimeRepository(context) }
    val notificationScheduler: NotificationScheduler by lazy { schedulerFactory(context, reminderTimeRepository) }
    val getReminderTimeUseCase: GetReminderTimeUseCase by lazy { GetReminderTimeUseCase(reminderTimeRepository) }
    val setReminderTimeUseCase: SetReminderTimeUseCase by lazy { SetReminderTimeUseCase(reminderTimeRepository) }
}
