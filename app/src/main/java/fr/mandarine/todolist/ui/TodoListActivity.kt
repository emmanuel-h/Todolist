package fr.mandarine.todolist.ui

import android.os.Bundle
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.mandarine.todolist.R
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialStage
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import fr.mandarine.todolist.ui.paper.PaperTheme
import fr.mandarine.todolist.ui.todolist.TodoListScreen
import fr.mandarine.todolist.ui.todolist.TodoListScreenState
import fr.mandarine.todolist.ui.reorder.moved
import fr.mandarine.todolist.ui.tutorial.TutorialOverlay
import fr.mandarine.todolist.ui.tutorial.TutorialOverlayController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TYPE_CHAR_MILLIS = 80L

class TodoListActivity : AppCompatActivity(), TutorialStage {

    internal lateinit var viewModel: TodoListViewModel
    internal val screenState = TodoListScreenState()

    private lateinit var tutorialViewModel: TutorialViewModel
    private lateinit var tutorialController: TutorialOverlayController
    internal val tutorialBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            tutorialController.onSkipRequested()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val listId = requireNotNull(intent.getStringExtra("LIST_ID")) {
            "TodoListActivity requires LIST_ID intent extra"
        }
        val listName = intent.getStringExtra("LIST_NAME") ?: getString(R.string.app_name)

        val container = (application as TodoListApplication).container
        val todoRepository = container.todoRepository
        viewModel = ViewModelProvider(
            this,
            viewModelFactory {
                TodoListViewModel(
                    AddTodoUseCase(todoRepository),
                    GetTodosUseCase(todoRepository),
                    ToggleTodoUseCase(todoRepository),
                    DeleteTodoUseCase(todoRepository),
                    EditTodoUseCase(todoRepository),
                    ReorderTodosUseCase(todoRepository),
                    GetTodoListsUseCase(container.todoListRepository),
                    listId = listId,
                    dispatcher = container.databaseDispatcher
                )
            }
        )[TodoListViewModel::class.java]

        tutorialViewModel = container.tutorialViewModel
        tutorialController = TutorialOverlayController(tutorialViewModel, lifecycleScope)
        screenState.animationsEnabled = animationsAllowed()

        setContent {
            val state by viewModel.state.collectAsState()
            PaperTheme {
                Box(Modifier.fillMaxSize()) {
                    TodoListScreen(
                        listName = listName,
                        state = state,
                        screenState = screenState,
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        onToggle = { todoId -> viewModel.toggleTodo(todoId) },
                        onEdit = { todoId, newTitle -> viewModel.editTodo(todoId, newTitle) },
                        onDelete = { todoId -> viewModel.deleteTodo(todoId) },
                        onSubmitInline = { title -> viewModel.submitInlineInput(title) },
                        onReorder = { from, to ->
                            screenState.previewOrder = null
                            viewModel.reorderTodos(from, to)
                        }
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
                viewModel.state.collect { state ->
                    if (state is TodoListState.NotFound) finish()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, tutorialBackCallback)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tutorialViewModel.uiState.collect { state ->
                    tutorialBackCallback.isEnabled = state is TutorialUiState.Active
                    screenState.animationsEnabled = animationsAllowed()
                    tutorialController.handleState(state, this@TodoListActivity)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
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

    // ── TutorialStage ──

    override val screen: TutorialScreen = TutorialScreen.ITEMS

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = screenState.boundsOf(anchor)

    override suspend fun perform(action: TutorialAction): Boolean = when (action) {
        TutorialAction.OpenItemAddRow -> openAddRow()
        is TutorialAction.TypeItemTitle -> typeItemTitle(action.text)
        TutorialAction.SubmitItem -> submitItem()
        is TutorialAction.ToggleActiveItem -> toggle(activeItems(), action.index)
        is TutorialAction.ToggleCompletedItem -> toggle(completedItems(), action.index)
        is TutorialAction.MoveActiveItem -> previewMove(action.from, action.to)
        is TutorialAction.CommitReorder -> commitReorder(action.from, action.to)
        TutorialAction.NavigateBack -> {
            tutorialBackCallback.isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            true
        }
        else -> false
    }

    override suspend fun awaitDemoListId(): String? = null

    override fun bannerContent(): TutorialBannerContent? = null

    private fun openAddRow(): Boolean {
        if (screenState.addRowExpanded) return false
        screenState.addRowExpanded = true
        return true
    }

    private suspend fun typeItemTitle(text: String): Boolean {
        if (!screenState.addRowExpanded) return false
        for (character in text) {
            delay(TYPE_CHAR_MILLIS)
            screenState.addRowText += character
        }
        screenState.requestHideKeyboard()
        return true
    }

    private fun submitItem(): Boolean {
        if (!screenState.addRowExpanded) return false
        val submitted = viewModel.submitInlineInput(screenState.addRowText)
        if (submitted) screenState.addRowText = ""
        return submitted
    }

    private fun toggle(items: List<TodoItem>, index: Int): Boolean {
        val item = items.getOrNull(index) ?: return false
        viewModel.toggleTodo(item.id)
        return true
    }

    private fun previewMove(from: Int, to: Int): Boolean {
        val ids = screenState.previewOrder ?: activeItems().map { it.id }
        if (from !in ids.indices || to !in ids.indices) return false
        screenState.previewOrder = ids.moved(from, to)
        return true
    }

    private fun commitReorder(from: Int, to: Int): Boolean {
        screenState.previewOrder = null
        viewModel.reorderTodos(from, to)
        return true
    }

    private fun activeItems(): List<TodoItem> =
        (viewModel.state.value as? TodoListState.Content)?.activeItems.orEmpty()

    private fun completedItems(): List<TodoItem> =
        (viewModel.state.value as? TodoListState.Content)?.completedItems.orEmpty()
}
