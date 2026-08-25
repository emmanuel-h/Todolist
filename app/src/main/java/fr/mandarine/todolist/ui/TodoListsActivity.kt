package fr.mandarine.todolist.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.Clock
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialStage
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import fr.mandarine.todolist.ui.paper.PaperTheme
import fr.mandarine.todolist.ui.paper.drawEdgeToEdge
import fr.mandarine.todolist.ui.paper.openOnPaper
import fr.mandarine.todolist.ui.paper.preparePaperSheet
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.DatePickerRequest
import fr.mandarine.todolist.ui.todolists.DateSelection
import fr.mandarine.todolist.ui.todolists.DateTarget
import fr.mandarine.todolist.ui.todolists.TodoListsScreen
import fr.mandarine.todolist.ui.todolists.TodoListsScreenState
import fr.mandarine.todolist.ui.tutorial.TutorialOverlay
import fr.mandarine.todolist.ui.tutorial.TutorialOverlayController
import fr.mandarine.todolist.ui.tutorial.behindTutorial
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TYPE_CHAR_MILLIS = 80L
private const val BLANK_PAGE_MILLIS = 500L

class TodoListsActivity : ComponentActivity(), TutorialStage {

    internal lateinit var viewModel: TodoListsViewModel
    internal val screenState = TodoListsScreenState()

    private lateinit var clock: Clock
    private lateinit var notificationAsk: NotificationAsk
    private lateinit var notificationPermission: ActivityResultLauncher<String>
    private var demoListId: String? = null
    private var listsBeforeDemo: Set<String> = emptySet()
    private var pageWritten by mutableStateOf(false)

    private lateinit var tutorialViewModel: TutorialViewModel
    private lateinit var tutorialController: TutorialOverlayController
    internal val tutorialBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            tutorialController.onSkipRequested()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        openOnPaper { pageWritten }
        drawEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as TodoListApplication).container
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
                    ReorderTodoListsUseCase(todoListRepository, getTodoListsWithStatusUseCase),
                    container.databaseDispatcher
                )
            }
        )[TodoListsViewModel::class.java]

        tutorialViewModel = container.tutorialViewModel
        tutorialController = TutorialOverlayController(tutorialViewModel, lifecycleScope)
        screenState.animationsEnabled = animationsAllowed()
        notificationAsk = NotificationAsk(this)
        notificationPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

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
            val state by viewModel.state.collectAsStateWithLifecycle()
            val overlayState = tutorialController.overlayState
            ReportDrawnWhen { pageWritten }
            PaperTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().behindTutorial(overlayState.visible)) {
                        TodoListsScreen(
                            state = state,
                            screenState = screenState,
                            today = clock.today(),
                            onOpenList = { list -> openList(list) },
                            onCreateList = { name, targetDate, dueDate ->
                                viewModel.createList(name, targetDate, dueDate)
                            },
                            onRenameList = { listId, name, targetDate, dueDate ->
                                viewModel.editList(listId, name, targetDate, dueDate)
                            },
                            onDeleteList = { listId -> viewModel.deleteList(listId) },
                            onDueDateSet = { askForNotifications() },
                            onReorder = { from, to ->
                                screenState.previewOrder = null
                                viewModel.reorderLists(from, to)
                            },
                            onReplayTutorial = { tutorialViewModel.replay() }
                        )
                    }
                    TutorialOverlay(
                        state = overlayState,
                        anchors = screenState,
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

        onBackPressedDispatcher.addCallback(this, tutorialBackCallback)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tutorialViewModel.uiState.collect { state ->
                    val running = state is TutorialUiState.ReadyToStart ||
                        state is TutorialUiState.Active
                    tutorialBackCallback.isEnabled = running
                    screenState.recordingAnchors = running
                    screenState.animationsEnabled = animationsAllowed()
                    tutorialController.handleState(state, this@TodoListsActivity)
                }
            }
        }

        tutorialViewModel.initialize()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    /**
     * A due date is the first moment a notification could ever fire, which makes
     * it the first moment the request explains itself. Before that there is
     * nothing to be reminded of, so nothing is asked.
     */
    internal fun askForNotifications() {
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val asked = notificationAsk.alreadyAsked()
        if (!shouldAskForNotifications(Build.VERSION.SDK_INT, granted, asked)) return
        notificationAsk.markAsked()
        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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

    private fun openList(list: TodoList) {
        val intent = Intent(this, TodoListActivity::class.java)
        intent.putExtra("LIST_ID", list.id)
        intent.putExtra("LIST_NAME", list.name)
        startActivity(intent)
    }

    // ── TutorialStage ──

    override val screen: TutorialScreen = TutorialScreen.LISTS

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? =
        tutorialController.overlayState.aimAt(anchor, screenState.boundsOf(anchor))

    override suspend fun perform(action: TutorialAction): Boolean = when (action) {
        TutorialAction.OpenListCreateRow -> openCreateRow()
        is TutorialAction.TypeListName -> typeListName(action.text)
        TutorialAction.OpenDueDatePicker -> openDueDatePicker()
        is TutorialAction.PickDueDate -> pickDueDate(action.date)
        TutorialAction.SubmitList -> submitList()
        TutorialAction.OpenFirstList -> openFirstList()
        TutorialAction.RequestDeleteFirstList -> armDeleteOnFirstList()
        TutorialAction.ConfirmDeleteFirstList -> confirmDeleteOnFirstList()
        else -> false
    }

    /**
     * On a page that already holds lists the demo's own list is not simply the
     * first row, so the id is found by waiting for a row that was not on the page
     * before the demo started writing. Taking the first row instead hands the
     * cleanup the reader's list and deletes it when the demo is abandoned.
     */
    override suspend fun awaitDemoListId(): String? {
        val written = listsBeforeDemo
        val content = viewModel.state.first { s ->
            s is TodoListsState.Content && s.activeSummaries.any { it.list.id !in written }
        } as TodoListsState.Content
        return content.activeSummaries.firstOrNull { it.list.id !in written }?.list?.id
    }

    private fun listIdsOnPage(): Set<String> {
        val content = viewModel.state.value as? TodoListsState.Content ?: return emptySet()
        return (content.activeSummaries + content.doneSummaries).mapTo(mutableSetOf()) { it.list.id }
    }

    override fun bannerContent(): TutorialBannerContent? {
        val summary = firstActiveSummary() ?: return null
        return TutorialBannerContent(summary.list.name, summary.list.dueDate)
    }

    private fun openCreateRow(): Boolean {
        if (screenState.addRowExpanded) return false
        listsBeforeDemo = listIdsOnPage()
        screenState.openAddRow()
        return true
    }

    private suspend fun typeListName(text: String): Boolean {
        if (!screenState.addRowExpanded) return false
        for (character in text) {
            delay(TYPE_CHAR_MILLIS)
            screenState.addRowText += character
        }
        return true
    }

    private fun openDueDatePicker(): Boolean {
        if (!screenState.addRowExpanded) return false
        screenState.datePickerRequest = DatePickerRequest(
            target = DateTarget.ADD_ROW,
            kind = DateKind.DUE,
            initial = screenState.addRowSelection.dueDate
        )
        return true
    }

    private fun pickDueDate(date: LocalDate): Boolean {
        screenState.addRowSelection = DateSelection(DateKind.DUE, date)
        screenState.datePickerRequest = null
        return true
    }

    private fun submitList(): Boolean {
        val name = screenState.addRowText
        if (name.isBlank()) return false
        val selection = screenState.addRowSelection
        viewModel.createList(name, selection.targetDate, selection.dueDate)
        screenState.closeAddRow()
        return true
    }

    private fun openFirstList(): Boolean {
        val summary = firstSummary() ?: return false
        demoListId = summary.list.id
        openList(summary.list)
        return true
    }

    /**
     * The demo tears the row off exactly the way a swipe does, so the beat that
     * used to arm a confirm strip now starts the pending deletion and the beat
     * that used to confirm it writes that deletion through early instead of
     * waiting for the undo slip to settle.
     */
    private fun armDeleteOnFirstList(): Boolean {
        val listId = demoListId ?: firstSummary()?.list?.id ?: return false
        screenState.deletion.request(listId)?.let { viewModel.deleteList(it) }
        return true
    }

    private fun confirmDeleteOnFirstList(): Boolean {
        val listId = screenState.deletion.commit() ?: return false
        viewModel.deleteList(listId)
        demoListId = null
        return true
    }

    /**
     * By the last scene the demo list is finished and has moved below the
     * divider, so "the first list" has to mean the first row on the page rather
     * than the first unfinished one.
     */
    private fun firstSummary(): TodoListSummary? {
        val content = viewModel.state.value as? TodoListsState.Content ?: return null
        return content.activeSummaries.firstOrNull() ?: content.doneSummaries.firstOrNull()
    }

    private fun firstActiveSummary(): TodoListSummary? =
        (viewModel.state.value as? TodoListsState.Content)?.activeSummaries?.firstOrNull()
}
