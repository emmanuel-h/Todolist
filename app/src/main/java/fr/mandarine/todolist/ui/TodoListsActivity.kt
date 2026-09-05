package fr.mandarine.todolist.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import fr.mandarine.todolist.AppContainer
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import fr.mandarine.todolist.presentation.ReminderSettingsViewModel
import fr.mandarine.todolist.presentation.TodoListViewModel
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel
import fr.mandarine.todolist.ui.nav.ItemsRoute
import fr.mandarine.todolist.ui.nav.ListsRoute
import fr.mandarine.todolist.ui.nav.NavStage
import fr.mandarine.todolist.ui.nav.PageStack
import fr.mandarine.todolist.ui.paper.PaperTheme
import fr.mandarine.todolist.ui.paper.drawEdgeToEdge
import fr.mandarine.todolist.ui.paper.openOnPaper
import fr.mandarine.todolist.ui.paper.preparePaperSheet
import fr.mandarine.todolist.ui.todolists.TodoListsScreenState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val BLANK_PAGE_MILLIS = 500L
private const val LIST_ID_EXTRA = "LIST_ID"
private const val OPEN_PAGE = "open-list-id"

/**
 * The one window the notebook is read in. Both pages live in it and the back stack
 * decides which of them is on top, so opening a list lays a sheet over the page of
 * lists instead of replacing the window — and back peels that sheet off again.
 */
class TodoListsActivity : ComponentActivity() {

    internal lateinit var viewModel: TodoListsViewModel
    internal lateinit var reminderSettingsViewModel: ReminderSettingsViewModel
    internal lateinit var stage: NavStage
    internal val screenState = TodoListsScreenState()
    internal val backStack = NavBackStack<NavKey>(ListsRoute)

    private lateinit var container: AppContainer
    private lateinit var clock: Clock
    private lateinit var notificationAsk: NotificationAsk
    private lateinit var notificationPermission: ActivityResultLauncher<String>
    private var pageWritten by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        openOnPaper { pageWritten }
        drawEdgeToEdge()
        super.onCreate(savedInstanceState)

        container = (application as TodoListApplication).container
        val todoListRepository = container.todoListRepository
        val todoRepository = container.todoRepository
        clock = container.clock
        val getTodoListsWithStatusUseCase =
            GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, clock)
        viewModel = ViewModelProvider(
            this,
            viewModelFactory {
                TodoListsViewModel(
                    CreateTodoListUseCase(todoListRepository),
                    DeleteTodoListUseCase(todoListRepository, todoRepository),
                    EditTodoListUseCase(todoListRepository),
                    getTodoListsWithStatusUseCase,
                    ReorderTodoListsUseCase(todoListRepository),
                    container.databaseDispatcher,
                    container.writeScope
                )
            }
        )[TodoListsViewModel::class.java]

        reminderSettingsViewModel = ViewModelProvider(
            this,
            viewModelFactory {
                ReminderSettingsViewModel(
                    container.getReminderTimeUseCase,
                    container.setReminderTimeUseCase,
                    container.notificationScheduler,
                    container.databaseDispatcher
                )
            }
        )[ReminderSettingsViewModel::class.java]

        stage = NavStage(backStack)
        stage.animationsEnabled = animationsAllowed()
        savedInstanceState?.let(screenState::restoreFrom)
        openedListId(savedInstanceState)?.let { backStack.add(ItemsRoute(it)) }

        notificationAsk = NotificationAsk(this)
        notificationPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { notificationAsk.markAsked() }

        container.notificationScheduler.scheduleDailyCheck()

        preparePaperSheet()

        /**
         * The launch window holds until the first read comes back, so the page
         * arrives already written on rather than blank with the rows landing a
         * frame later. A page that has nothing on it never reaches Content, so the
         * wait is bounded and the paper is handed over either way.
         */
        lifecycleScope.launch {
            withTimeoutOrNull(BLANK_PAGE_MILLIS) {
                viewModel.state.first { it is TodoListsState.Content }
            }
            pageWritten = true
        }

        setContent {
            ReportDrawnWhen { pageWritten }
            PaperTheme {
                PageStack(
                    backStack = backStack,
                    listsViewModel = viewModel,
                    listsScreenState = screenState,
                    stage = stage,
                    today = clock.today(),
                    itemsViewModelFactory = { listId -> itemsViewModelFactory(listId) },
                    onDueDateSet = { askForNotifications() },
                    reminderSettingsViewModel = reminderSettingsViewModel
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animationEvents.collect {
                    if (animationsAllowed()) screenState.noteListAdded()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (backStack.lastOrNull() as? ItemsRoute)?.let { outState.putString(OPEN_PAGE, it.listId) }
        screenState.saveTo(outState)
    }

    /**
     * A reminder is the first moment a notification could ever fire, which makes
     * it the first moment the request explains itself. Before that there is
     * nothing to be reminded of, so nothing is asked.
     *
     * Once the ask has been spent it is not put again — but a reader who writes
     * another reminder while notifications are off is asking for something the
     * app cannot deliver, and an app with no settings screen has nowhere else to
     * say so. That second time opens the system's own page for it, which is both
     * the explanation and the remedy.
     */
    internal fun askForNotifications() {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (shouldAskForNotifications(Build.VERSION.SDK_INT, granted, notificationAsk.alreadyAsked())) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        openNotificationSettings()
    }

    private fun openNotificationSettings() {
        val settings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        if (settings.resolveActivity(packageManager) != null) startActivity(settings)
    }

    /**
     * A tapped notification names the list it is about, and a window that is being
     * rebuilt after being thrown away remembers the page it was left open at. Both
     * arrive as one sheet already laid over the page of lists.
     *
     * They are asked separately. A saved window that recorded no open page was
     * left on the page of lists, and falling through to the notification's extra
     * would have re-opened the list on every rotation for the life of the task —
     * the extra is never cleared by anything else. Honouring it once takes it off
     * the intent.
     */
    private fun openedListId(savedInstanceState: Bundle?): String? {
        if (savedInstanceState != null) return savedInstanceState.getString(OPEN_PAGE)
        val named = intent.getStringExtra(LIST_ID_EXTRA) ?: return null
        intent.removeExtra(LIST_ID_EXTRA)
        return named
    }

    private fun itemsViewModelFactory(listId: String): ViewModelProvider.Factory =
        viewModelFactory {
            TodoListViewModel(
                AddTodoUseCase(container.todoRepository),
                GetTodosUseCase(container.todoRepository),
                ToggleTodoUseCase(container.todoRepository),
                DeleteTodoUseCase(container.todoRepository),
                EditTodoUseCase(container.todoRepository),
                ReorderTodosUseCase(container.todoRepository),
                GetTodoListsUseCase(container.todoListRepository),
                listId = listId,
                dispatcher = container.databaseDispatcher,
                writeScope = container.writeScope
            )
        }

    private fun animationsAllowed(): Boolean = !isReducedMotion()

    private fun isReducedMotion(): Boolean {
        val scale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return scale == 0f
    }
}
