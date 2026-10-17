package fr.mandarine.todolist.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import fr.mandarine.todolist.domain.AnimationEvent
import fr.mandarine.todolist.presentation.ReminderSettingsViewModel
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.ReminderNote
import fr.mandarine.todolist.ui.paper.ReminderSlip
import fr.mandarine.todolist.ui.paper.rememberReminderNotes
import fr.mandarine.todolist.ui.todolist.TodoListScreen
import fr.mandarine.todolist.ui.todolist.TodoListScreenState
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.ui.todolists.DateSelection
import fr.mandarine.todolist.ui.todolists.TodoListsScreen
import fr.mandarine.todolist.ui.todolists.TodoListsScreenState
import fr.mandarine.todolist.ui.todolists.reminderDateWritten
import java.time.LocalDate

private const val PAGE_RISE = 8
private const val ITEMS_VIEW_MODEL = "items-"
private const val FLAT_ON_THE_PAD = 0f
private const val LAID_OVER_THE_PAD = 1f

/**
 * The whole app in one window: a page of lists with, on top of it, the page of one
 * list. Opening a list lays its page over the one underneath rather than replacing
 * the window, which is what lets the tapped row travel into the head rule it
 * becomes and lets a drag from the edge peel the page back off by hand.
 */
@Composable
fun PageStack(
    backStack: NavBackStack<NavKey>,
    listsViewModel: TodoListsViewModel,
    listsScreenState: TodoListsScreenState,
    stage: NavStage,
    today: LocalDate,
    itemsViewModelFactory: (String) -> ViewModelProvider.Factory,
    onDueDateSet: () -> Unit,
    reminderSettingsViewModel: ReminderSettingsViewModel
) {
    val listsState by listsViewModel.state.collectAsStateWithLifecycle()
    val reminderTime by reminderSettingsViewModel.reminderTime.collectAsStateWithLifecycle()
    /**
     * A reminder announces itself from above both pages rather than from either
     * one: the day may be written on the page of lists or on a list's own page,
     * and the slip that says so is the same slip either way.
     */
    val notes = rememberReminderNotes()
    val written: (ReminderNote) -> Unit = { note ->
        notes.raise(note)
        onDueDateSet()
    }
    Box(modifier = Modifier.fillMaxSize()) {
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { stage.leave() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            sharedTransitionScope = this@SharedTransitionLayout,
            transitionSpec = { pageArrives() },
            popTransitionSpec = { pagePeels() },
            predictivePopTransitionSpec = { _ -> pagePeels() },
            entryProvider = entryProvider {
                entry<ListsRoute> {
                    Travelling {
                        ListsPage(
                            state = listsState,
                            viewModel = listsViewModel,
                            screenState = listsScreenState,
                            stage = stage,
                            today = today,
                            onDueDateSet = written,
                            reminderTime = reminderTime
                        )
                    }
                }
                entry<ItemsRoute> { route ->
                    Travelling {
                        ItemsPager(
                            openedId = route.listId,
                            listsState = listsState,
                            listsViewModel = listsViewModel,
                            stage = stage,
                            today = today,
                            itemsViewModelFactory = itemsViewModelFactory,
                            onDueDateSet = written,
                            appWideReminderTime = reminderTime
                        )
                    }
                }
            }
        )
    }
        ReminderSlip(
            notes = notes,
            animated = stage.animationsEnabled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = SLIP_MARGIN)
        )
    }
}

private val SLIP_MARGIN = 12.dp

/**
 * The page being opened rises a little as it lands, and the page underneath does not
 * go anywhere: it is held, whole and in full ink, until the arriving page and the
 * marks travelling onto it have both finished, so the rows around the tapped one
 * stay where they were for the length of the travel.
 *
 * Holding it has to be asked for. A fade to full ink is an animation with nothing to
 * animate, so it finishes on the frame it starts and the page beneath is let go of
 * immediately, leaving the opened page to dissolve up out of the bare window rather
 * than off the page it came from.
 *
 * The arriving page comes up on the same spring as the marks travelling onto it, so
 * paper and writing land together rather than the paper arriving first.
 */
private fun AnimatedContentTransitionScope<*>.pageArrives(): ContentTransform =
    (
        slideInVertically(PaperMotion.rowPlacement) { height -> height / PAGE_RISE } +
            fadeIn(PaperMotion.sheetSettle) togetherWith
            ExitTransition.KeepUntilTransitionsFinished
        ).apply { targetContentZIndex = LAID_OVER_THE_PAD }

/**
 * Back does not swap one window for another: the sheet on top slides off the pad,
 * uncovering the page of lists that was underneath it the whole time. Dragging from
 * the edge seeks this same movement frame by frame, so the sheet travels with the
 * finger and springs flat again if the finger lets go early.
 *
 * The page uncovered does not fade up, because it was never away: fading it made the
 * rows of the page beneath the sheet come in with the finger, greyed for the first
 * part of a gesture the reader had not committed to yet.
 */
private fun pagePeels(): ContentTransform =
    (
        EnterTransition.None togetherWith
            slideOutHorizontally(PaperMotion.rowPlacement) { width -> width }
        ).apply { targetContentZIndex = FLAT_ON_THE_PAD }

@Composable
private fun SharedTransitionScope.Travelling(content: @Composable () -> Unit) {
    val arrival = LocalNavAnimatedContentScope.current
    val travel = remember(this, arrival) { PageTravel(this, arrival) }
    CompositionLocalProvider(LocalPageTravel provides travel) { content() }
}

@Composable
private fun ListsPage(
    state: TodoListsState,
    viewModel: TodoListsViewModel,
    screenState: TodoListsScreenState,
    stage: NavStage,
    today: LocalDate,
    onDueDateSet: (ReminderNote) -> Unit,
    reminderTime: java.time.LocalTime
) {
    screenState.animationsEnabled = stage.animationsEnabled

    LaunchedEffect(viewModel) { viewModel.refresh() }

    TodoListsScreen(
        state = state,
        screenState = screenState,
        today = today,
        onOpenList = { list -> stage.open(list) },
        onCreateList = { name, targetDate, dueDate ->
            viewModel.createList(name, targetDate, dueDate)
        },
        onRenameList = { listId, name, targetDate, dueDate, colour ->
            viewModel.editList(listId, name, targetDate, dueDate, colour)
        },
        onDeleteList = { listId -> viewModel.deleteList(listId) },
        onDueDateSet = onDueDateSet,
        onReorder = { orderedActiveIds ->
            screenState.previewOrder = null
            viewModel.reorderLists(orderedActiveIds)
        },
        reminderTime = reminderTime,
        onSetListReminderTime = { listId, minuteOfDay ->
            viewModel.setListReminderTime(listId, minuteOfDay)
        }
    )
}

/**
 * The lists, bound one after another like the pages of the notebook they are: a
 * swipe sideways turns to the next list without going back to the page of lists
 * first. They are bound in the order the page of lists shows them.
 *
 * The binding is fixed while the reader is turning pages. A list that the reader
 * finishes moves to the foot of the page of lists, and a list given a day moves to
 * its horizon; re-binding on every such change would turn the page out from under
 * the finger. Lists that go are taken out, lists that arrive are bound at the back.
 */
@Composable
private fun ItemsPager(
    openedId: String,
    listsState: TodoListsState,
    listsViewModel: TodoListsViewModel,
    stage: NavStage,
    today: LocalDate,
    itemsViewModelFactory: (String) -> ViewModelProvider.Factory,
    onDueDateSet: (ReminderNote) -> Unit,
    appWideReminderTime: java.time.LocalTime
) {
    var bound by remember { mutableStateOf(rebind(emptyList(), pageOrder(listsState), openedId)) }
    LaunchedEffect(listsState) { bound = rebind(bound, pageOrder(listsState), openedId) }
    val pager = rememberPagerState(initialPage = bound.indexOf(openedId).coerceAtLeast(0)) { bound.size }
    var showing by remember { mutableStateOf(openedId) }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page -> bound.getOrNull(page)?.let { showing = it } }
    }
    LaunchedEffect(bound) {
        val page = bound.indexOf(showing)
        if (page >= 0 && page != pager.currentPage) pager.scrollToPage(page)
    }
    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxSize().peelingEdge(),
        key = { page -> bound[page] }
    ) { page ->
        val listId = bound[page]
        ItemsPage(
            listId = listId,
            listsState = listsState,
            listsViewModel = listsViewModel,
            stage = stage,
            today = today,
            itemsViewModelFactory = itemsViewModelFactory,
            onDueDateSet = onDueDateSet,
            appWideReminderTime = appWideReminderTime,
            onSetListReminderTime = { id, minuteOfDay ->
                listsViewModel.setListReminderTime(id, minuteOfDay)
            },
            showing = page == pager.settledPage,
            pagePosition = page,
            pageCount = bound.size
        )
    }
}

internal fun pageOrder(state: TodoListsState): List<String> =
    (state as? TodoListsState.Content)
        ?.let { it.activeSummaries + it.doneSummaries }
        ?.map { it.list.id }
        .orEmpty()

/**
 * The binding after the page of lists changed under it: what was bound stays in its
 * place unless it is gone, and what is new is bound at the back. Until the page of
 * lists has been read there is nothing to bind but the list that was opened.
 */
internal fun rebind(bound: List<String>, published: List<String>, openedId: String): List<String> {
    if (published.isEmpty()) return bound.ifEmpty { listOf(openedId) }
    if (bound.size <= 1) return published.takeIf { openedId in it } ?: listOf(openedId)
    val kept = bound.filter { it in published }
    return kept + published.filterNot { it in kept }
}

@Composable
private fun ItemsPage(
    listId: String,
    listsState: TodoListsState,
    listsViewModel: TodoListsViewModel,
    stage: NavStage,
    today: LocalDate,
    itemsViewModelFactory: (String) -> ViewModelProvider.Factory,
    onDueDateSet: (ReminderNote) -> Unit,
    appWideReminderTime: java.time.LocalTime,
    onSetListReminderTime: (String, Int?) -> Unit,
    showing: Boolean = true,
    pagePosition: Int = 0,
    pageCount: Int = 1
) {
    val viewModel: TodoListViewModel = viewModel(
        key = ITEMS_VIEW_MODEL + listId,
        factory = remember(listId) { itemsViewModelFactory(listId) }
    )
    val screenState = rememberSaveable(listId, saver = TodoListScreenState.Saver) {
        TodoListScreenState()
    }
    screenState.animationsEnabled = stage.animationsEnabled

    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = remember(listsState, listId) { summaryOf(listsState, listId) }

    LaunchedEffect(viewModel) { viewModel.refresh() }

    LaunchedEffect(state, showing) {
        if (showing && state is TodoListState.NotFound) stage.leave()
    }

    /**
     * The page is told which tick emptied the list and nothing else. It works out
     * where on the paper to celebrate from itself, because the view model has no
     * business knowing where a finger was.
     */
    LaunchedEffect(viewModel) {
        viewModel.animationEvents.collect { event ->
            if (event is AnimationEvent.ListCompleted) screenState.finishedOn = event.lastItemId
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TodoListScreen(
            summary = summary,
            today = today,
            state = state,
            screenState = screenState,
            onBack = { stage.leave() },
            onToggle = { todoId -> viewModel.toggleTodo(todoId) },
            onEdit = { todoId, newTitle -> viewModel.editTodo(todoId, newTitle) },
            onDelete = { todoId -> viewModel.deleteTodo(todoId) },
            onSubmitInline = { title -> viewModel.submitInlineInput(title) },
            onReorder = { orderedActiveIds ->
                screenState.previewOrder = null
                viewModel.reorderTodos(orderedActiveIds)
            },
            onRenameList = { name ->
                val list = summary?.list ?: return@TodoListScreen
                listsViewModel.editList(listId, name, list.targetDate, list.dueDate, list.colour)
            },
            onWriteDate = { written ->
                val list = summary?.list ?: return@TodoListScreen
                listsViewModel.editList(
                    listId,
                    list.name,
                    written.targetDate,
                    written.dueDate,
                    list.colour
                )
                val before = DateSelection.of(list.targetDate, list.dueDate)
                if (reminderDateWritten(before, written)) {
                    onDueDateSet(ReminderNote(list.name, written.date))
                }
            },
            appWideReminderTime = appWideReminderTime,
            onSetListReminderTime = onSetListReminderTime,
            pagePosition = pagePosition,
            pageCount = pageCount
        )
    }
}

private fun summaryOf(state: TodoListsState, listId: String) =
    (state as? TodoListsState.Content)
        ?.let { it.activeSummaries + it.doneSummaries }
        ?.firstOrNull { it.list.id == listId }
