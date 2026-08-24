package fr.mandarine.todolist.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
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
import fr.mandarine.todolist.ui.paper.preparePaperSheet
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.DatePickerRequest
import fr.mandarine.todolist.ui.todolists.DateSelection
import fr.mandarine.todolist.ui.todolists.DateTarget
import fr.mandarine.todolist.ui.todolists.TodoListsScreen
import fr.mandarine.todolist.ui.todolists.TodoListsScreenState
import fr.mandarine.todolist.ui.tutorial.TutorialOverlay
import fr.mandarine.todolist.ui.tutorial.TutorialOverlayController
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TYPE_CHAR_MILLIS = 80L

class TodoListsActivity : ComponentActivity(), TutorialStage {

    internal lateinit var viewModel: TodoListsViewModel
    internal val screenState = TodoListsScreenState()

    private lateinit var clock: Clock
    private var notificationPermissionRequested = false

    private lateinit var tutorialViewModel: TutorialViewModel
    private lateinit var tutorialController: TutorialOverlayController
    internal val tutorialBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            tutorialController.onSkipRequested()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        container.notificationScheduler.scheduleDailyCheck()

        preparePaperSheet()

        setContent {
            val state by viewModel.state.collectAsState()
            PaperTheme {
                Box(Modifier.fillMaxSize()) {
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
                        onReorder = { from, to ->
                            screenState.previewOrder = null
                            viewModel.reorderLists(from, to)
                        },
                        onReplayTutorial = { tutorialViewModel.replay() }
                    )
                    TutorialOverlay(
                        state = tutorialController.overlayState,
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

        val requestNotificationPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tutorialViewModel.uiState.collect { state ->
                    tutorialBackCallback.isEnabled = state is TutorialUiState.ReadyToStart ||
                        state is TutorialUiState.Active
                    screenState.animationsEnabled = animationsAllowed()
                    tutorialController.handleState(state, this@TodoListsActivity)
                    if (state is TutorialUiState.Dismissed && !notificationPermissionRequested) {
                        notificationPermissionRequested = true
                        maybeRequestNotificationPermission(requestNotificationPermission)
                    }
                }
            }
        }

        tutorialViewModel.initialize()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun maybeRequestNotificationPermission(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
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

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = screenState.boundsOf(anchor)

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

    override suspend fun awaitDemoListId(): String? {
        val content = viewModel.state.first { s ->
            s is TodoListsState.Content && s.activeSummaries.isNotEmpty()
        } as TodoListsState.Content
        return content.activeSummaries.firstOrNull()?.list?.id
    }

    override fun bannerContent(): TutorialBannerContent? {
        val summary = firstActiveSummary() ?: return null
        return TutorialBannerContent(summary.list.name, summary.list.dueDate)
    }

    private fun openCreateRow(): Boolean {
        if (screenState.addRowExpanded) return false
        screenState.openAddRow()
        return true
    }

    private suspend fun typeListName(text: String): Boolean {
        if (!screenState.addRowExpanded) return false
        for (character in text) {
            delay(TYPE_CHAR_MILLIS)
            screenState.addRowText += character
        }
        screenState.requestHideKeyboard()
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
        openList(summary.list)
        return true
    }

    private fun armDeleteOnFirstList(): Boolean {
        val summary = firstSummary() ?: return false
        screenState.confirmingDeleteListId = summary.list.id
        return true
    }

    private fun confirmDeleteOnFirstList(): Boolean {
        val listId = screenState.confirmingDeleteListId ?: return false
        screenState.confirmingDeleteListId = null
        viewModel.deleteList(listId)
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
