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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
import fr.mandarine.todolist.domain.SaveDemoListIdUseCase
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import fr.mandarine.todolist.presentation.TodoListViewModel
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import fr.mandarine.todolist.ui.nav.ItemsRoute
import fr.mandarine.todolist.ui.nav.ListsRoute
import fr.mandarine.todolist.ui.nav.NavStage
import fr.mandarine.todolist.ui.nav.PageStack
import fr.mandarine.todolist.ui.paper.PaperTheme
import fr.mandarine.todolist.ui.paper.drawEdgeToEdge
import fr.mandarine.todolist.ui.paper.openOnPaper
import fr.mandarine.todolist.ui.paper.preparePaperSheet
import fr.mandarine.todolist.ui.todolists.ListsStage
import fr.mandarine.todolist.ui.todolists.TodoListsScreenState
import fr.mandarine.todolist.ui.tutorial.NonShiftingTodoListRepository
import fr.mandarine.todolist.ui.tutorial.TutorialOverlay
import fr.mandarine.todolist.ui.tutorial.TutorialOverlayController
import fr.mandarine.todolist.ui.tutorial.behindTutorial
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    internal lateinit var stage: NavStage
    internal val screenState = TodoListsScreenState()
    internal val backStack = NavBackStack<NavKey>(ListsRoute)

    private lateinit var container: AppContainer
    private lateinit var clock: Clock
    private lateinit var notificationAsk: NotificationAsk
    private lateinit var notificationPermission: ActivityResultLauncher<String>
    private var pageWritten by mutableStateOf(false)

    private lateinit var tutorialViewModel: TutorialViewModel
    private lateinit var tutorialController: TutorialOverlayController

    /**
     * Held on the window rather than inside the collector: the collector is torn
     * down and rebuilt every time the window stops and starts, and a reader
     * coming back to an open page must not be read as a demo that has just ended.
     */
    private var lastTutorialState: TutorialUiState? = null
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

        tutorialViewModel = container.tutorialViewModel
        tutorialController = TutorialOverlayController(tutorialViewModel, lifecycleScope)
        stage = NavStage(
            backStack,
            ListsStage(
                viewModel = viewModel,
                screenState = screenState,
                aim = tutorialController.overlayState::aimAt,
                writeDemoList = { name, targetDate, dueDate ->
                    writeDemoList(name, targetDate, dueDate)
                },
                onOpen = { list -> stage.open(list) }
            )
        )
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
            val overlayState = tutorialController.overlayState
            ReportDrawnWhen { pageWritten }
            PaperTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().behindTutorial(overlayState.visible)) {
                        PageStack(
                            backStack = backStack,
                            listsViewModel = viewModel,
                            listsScreenState = screenState,
                            stage = stage,
                            today = clock.today(),
                            itemsViewModelFactory = { listId -> itemsViewModelFactory(listId) },
                            aim = overlayState::aimAt,
                            onDueDateSet = { askForNotifications() },
                            onReplayTutorial = { tutorialViewModel.replay() }
                        )
                    }
                    TutorialOverlay(
                        state = overlayState,
                        anchors = stage.anchors,
                        onSkip = { tutorialController.onSkipRequested() }
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animationEvents.collect {
                    if (animationsAllowed()) screenState.noteListAdded()
                }
            }
        }

        /**
         * A scene belongs to a step and to the page that step is played on, and
         * with one window the page can now change without the step changing — the
         * demo walking back out of a list is exactly that. So the beat the
         * controller is asked to play is both together.
         */
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(tutorialViewModel.uiState, snapshotFlow { stage.screen }) { state, _ ->
                    state
                }.collect { state ->
                    val running = state is TutorialUiState.ReadyToStart ||
                        state is TutorialUiState.Active
                    tutorialController.overlayState.running = running
                    stage.recordingAnchors = running
                    stage.animationsEnabled = animationsAllowed()
                    if (demoJustEnded(state)) {
                        closeDemoPage()
                        viewModel.refresh()
                    }
                    lastTutorialState = state
                    tutorialController.handleState(state, stage)
                }
            }
        }

        tutorialViewModel.initialize()
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

    /**
     * The demo ending is a moment, not a condition.
     *
     * The beat is read from two flows at once, and the page is one of them, so
     * the resting state is offered again every single time the reader turns a
     * page. Acting on the state alone tore off whatever page they had just
     * opened — which, once the demo has been seen, is every page there is.
     *
     * Only a demo that was actually on the paper can end, so the tear-off is
     * asked for by the transition out of a running tour and by nothing else.
     * A page restored from a notification or from a saved window arrives with
     * the tour long over and is left where it is.
     */
    private fun demoJustEnded(state: TutorialUiState): Boolean =
        state is TutorialUiState.Dismissed &&
            (
                lastTutorialState is TutorialUiState.ReadyToStart ||
                    lastTutorialState is TutorialUiState.Active
                )

    /**
     * The demo is torn off with its list, so a page of it left open would outlive
     * the list it reads and leave the reader holding a sheet with nothing written
     * on it. Ending the demo closes whatever it opened.
     */
    private fun closeDemoPage() {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /**
     * The demo's list is claimed as the demo's before a single row of it is
     * written, so an abort at any beat — or a window torn down mid-demonstration —
     * finds an id to tear off rather than a stray list nobody owns. It is then
     * written through a page that lays it above the reader's rows without
     * renumbering them, which is what lets the tear-off restore the page exactly.
     */
    private suspend fun writeDemoList(name: String, targetDate: LocalDate?, dueDate: LocalDate?) {
        withContext(container.databaseDispatcher) {
            val demoListId = UUID.randomUUID().toString()
            SaveDemoListIdUseCase(container.tutorialStateRepository)(demoListId)
            CreateTodoListUseCase(
                NonShiftingTodoListRepository(container.todoListRepository)
            ) { demoListId }(name, targetDate, dueDate)
        }
        viewModel.refresh()
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

    private fun animationsAllowed(): Boolean =
        !tutorialViewModel.animationsSuppressed && !isReducedMotion()

    private fun isReducedMotion(): Boolean {
        val scale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return scale == 0f
    }
}
