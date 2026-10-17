package fr.mandarine.todolist.ui.todolists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.ConfirmDeleteRequest
import fr.mandarine.todolist.ui.calendar.CalendarPage
import fr.mandarine.todolist.ui.paper.CorkDimens
import fr.mandarine.todolist.ui.paper.CorkSurface
import fr.mandarine.todolist.ui.paper.DeleteConfirmDialog
import fr.mandarine.todolist.ui.paper.InkAddLine
import fr.mandarine.todolist.ui.paper.LocalPaperGutter
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.ReminderClockPickerDialog
import fr.mandarine.todolist.ui.paper.ReminderNote
import fr.mandarine.todolist.ui.paper.RemoveDateConfirmDialog
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.SectionHeading
import fr.mandarine.todolist.ui.paper.StickyNotePad
import fr.mandarine.todolist.ui.paper.StickyNotePutBack
import fr.mandarine.todolist.ui.paper.pinnedPaper
import fr.mandarine.todolist.ui.reorder.BoardDrag
import fr.mandarine.todolist.ui.reorder.LiftHold
import fr.mandarine.todolist.ui.reorder.ScrollBoardWhileDragging
import fr.mandarine.todolist.ui.reorder.boardCard
import fr.mandarine.todolist.ui.reorder.moved
import fr.mandarine.todolist.ui.reorder.orderedBy
import fr.mandarine.todolist.ui.reorder.rememberEdgeScroll
import java.time.LocalDate
import java.time.LocalTime

private const val HEAD_KEY = "head"
private const val ADD_KEY = "list-add"
private const val WEEK_KEY = "this-week"
private const val FINISHED_KEY = "finished"
private const val HEAD_TYPE = "head"
private const val ADD_TYPE = "list-add"
private const val WEEK_TYPE = "this-week"
private const val CARD_TYPE = "card"
private const val HEADING_TYPE = "heading"
private const val DONE_TYPE = "done"
private val CORNER_MARGIN = 8.dp
private val DROP_IN_TRAVEL = 16.dp
private val HEAD_GUTTER = 18.dp
private val BOARD_TOP = 12.dp
private val BOARD_FOOT = 24.dp
private val HEAD_STRIP_BOTTOM = 6.dp
private val LINE_STRIP_PADDING = 4.dp

/**
 * Screen 1: the board the lists are pinned to, and — one tab over — the calendar
 * the dated ones fall on.
 *
 * The entry point for everything the reader sees at rest. It receives a
 * [TodoListsState] and a bag of `on…` callbacks and owns no data of its own — the
 * stored data is in `TodoListsViewModel`, the in-progress gesture state is in
 * [TodoListsScreenState], and this function only arranges and reports.
 *
 * ### What it puts on the board
 * A `LazyVerticalStaggeredGrid` — cards of different heights in columns, composing
 * only what is on screen — carrying, in order: the head, the inline "write a new
 * list" line, the slip naming what is due this week, every unfinished list as a
 * card in the reader's own order, and the finished cards under a heading that
 * folds them away. Days do not order the board: most lists have none, and the
 * reader's own arrangement is the one that holds. The calendar is where days lead.
 * The sticky-note pad and the tab bar sit *outside* the grid, laid over it.
 *
 * ### Keys
 * Every `items(...)` call passes a stable key (the list id), so cards keep their
 * own state and animate to their new places when the board re-flows.
 */
@Composable
fun TodoListsScreen(
    state: TodoListsState,
    screenState: TodoListsScreenState,
    today: LocalDate,
    onOpenList: (TodoList) -> Unit,
    onCreateList: (String, LocalDate?, LocalDate?) -> Unit,
    onRenameList: (String, String, LocalDate?, LocalDate?, ListColour) -> Unit,
    onDeleteList: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onDueDateSet: (ReminderNote) -> Unit = {},
    reminderTime: LocalTime = LocalTime.of(8, 0),
    onSetListReminderTime: (String, Int?) -> Unit = { _, _ -> }
) {
    val content = state as? TodoListsState.Content
    val published = content?.activeSummaries.orEmpty()
    val activeSummaries = orderedBy(published, screenState.previewOrder) { it.list.id }
    val doneSummaries = content?.doneSummaries.orEmpty()
    val publishedIds = published.map { it.list.id }
    LaunchedEffect(publishedIds) { screenState.releaseOrder(publishedIds) }

    val gridState = rememberLazyStaggeredGridState()
    val drag = remember(screenState) { BoardDrag { order -> screenState.previewOrder = order } }
    val focusManager = LocalFocusManager.current
    val pageEmpty = activeSummaries.isEmpty() && doneSummaries.isEmpty()

    val requestActions: (TodoListSummary) -> Unit = { summary ->
        if (!drag.dragging) screenState.actionsFor = summary.list.id
    }
    val requestDelete: (TodoListSummary) -> Unit = { summary ->
        if (!drag.dragging) {
            val cascade = summary.activeCount + summary.completedCount
            screenState.confirmDelete = ConfirmDeleteRequest(
                id = summary.list.id,
                name = summary.list.name,
                cascadeCount = cascade
            )
        }
    }
    /**
     * A tap on a card while the pen is still on the add line finishes that line
     * rather than leaving the screen with the words on it: on a board full of cards
     * there may be no bare cork left to tap, and a tap that loses what was written
     * reads as a mistake the app made.
     */
    val openList: (TodoList) -> Unit = { list ->
        when {
            drag.dragging -> Unit
            screenState.addRowExpanded -> {
                submitAddRow(screenState, onCreateList, onDueDateSet)
                screenState.closeAddRow()
            }
            else -> onOpenList(list)
        }
    }
    val holdTorn: (String) -> Unit = { id ->
        gridState.holdBoard { onDeleteList(id) }
        screenState.tearingId = null
    }
    /**
     * The page torn off a card's corner is the same mark as the jot on that list's
     * own head, so it opens the same calendar on the same day rather than counting
     * as a tap on the card it is clipped to.
     */
    val rewriteDate: (String) -> (DateSelection) -> Unit = { listId ->
        { jotted ->
            screenState.datePickerRequest = DatePickerRequest(
                target = DateTarget.Row(listId),
                kind = jotted.kind,
                initial = jotted.date
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        when (screenState.tab) {
            BoardTab.Lists -> Board(
                state = state,
                screenState = screenState,
                gridState = gridState,
                drag = drag,
                activeSummaries = activeSummaries,
                doneSummaries = doneSummaries,
                today = today,
                onOpenList = openList,
                onCreateList = onCreateList,
                onDueDateSet = onDueDateSet,
                onReorder = onReorder,
                onActionsRequested = requestActions,
                onTorn = holdTorn,
                onRewriteDate = rewriteDate
            )
            BoardTab.Calendar -> CalendarPage(
                state = state,
                today = today,
                month = screenState.calendarMonth,
                onMonthTurned = { screenState.calendarMonth = it },
                onOpenList = openList,
                animated = screenState.animationsEnabled
            )
        }

        BoardTabBar(
            selected = screenState.tab,
            onSelect = { tab ->
                if (screenState.addRowExpanded) screenState.closeAddRow()
                screenState.tab = tab
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (screenState.tab == BoardTab.Lists) {
            StickyNotePad(
                onTake = { screenState.openAddRow() },
                contentDescription = stringResource(R.string.add_list_fab_description),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = BoardBarHeight)
                    .padding(CORNER_MARGIN),
                taken = screenState.addRowExpanded,
                reducedMotion = !screenState.animationsEnabled,
                beckons = pageEmpty,
                landing = addLineSeat(),
                putBack = StickyNotePutBack(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.commit_list),
                    onPress = {
                        submitAddRow(screenState, onCreateList, onDueDateSet)
                        focusManager.clearFocus()
                        screenState.closeAddRow()
                    }
                )
            )
        }
    }

    val actionsSummary = screenState.actionsFor?.let { summaryOnPage(state, it) }
    if (actionsSummary != null) {
        val list = actionsSummary.list
        ListActionsSheet(
            summary = actionsSummary,
            onDismiss = { screenState.actionsFor = null },
            onRename = {
                screenState.actionsFor = null
                screenState.rename = RenameState.of(list)
            },
            onPickDate = {
                screenState.actionsFor = null
                val written = DateSelection.of(list.targetDate, list.dueDate)
                screenState.datePickerRequest = DatePickerRequest(
                    target = DateTarget.Row(list.id),
                    kind = written.kind,
                    initial = written.date
                )
            },
            onDelete = {
                screenState.actionsFor = null
                requestDelete(actionsSummary)
            }
        )
    }

    val confirm = screenState.confirmDelete
    if (confirm != null) {
        DeleteConfirmDialog(
            name = confirm.name,
            cascadeCount = confirm.cascadeCount,
            onCancel = { screenState.confirmDelete = null },
            onDelete = {
                screenState.confirmDelete = null
                screenState.tearingId = confirm.id
            }
        )
    }

    val rename = screenState.rename
    if (rename != null) {
        RenameListDialog(
            state = rename,
            onNameChange = { screenState.rename = rename.copy(name = it) },
            onKindChange = { kind ->
                writeRenameSelection(screenState, rename.selection.withKind(kind))
            },
            onPickDate = { kind ->
                screenState.datePickerRequest = DatePickerRequest(
                    target = DateTarget.Rename,
                    kind = kind,
                    initial = rename.selection.date
                )
            },
            onClearDate = {
                screenState.rename = rename.copy(selection = rename.selection.cleared())
            },
            onColourChange = { colour ->
                screenState.rename = rename.copy(colour = colour)
            },
            animated = screenState.animationsEnabled,
            onDismiss = { screenState.rename = null },
            onConfirm = {
                if (rename.name.isNotBlank()) {
                    /**
                     * The ask waits for the sheet to be put down, because that is
                     * when the reminder exists. Raised when the mark was pressed
                     * instead, a reader who blanked the name and dismissed had a
                     * slip promising a reminder that was never written and an ask
                     * they only get once already spent.
                     */
                    val before = listOnPage(state, rename.listId)
                        ?.let { DateSelection.of(it.targetDate, it.dueDate) }
                        ?: DateSelection.None
                    onRenameList(
                        rename.listId,
                        rename.name,
                        rename.selection.targetDate,
                        rename.selection.dueDate,
                        rename.colour
                    )
                    screenState.rename = null
                    if (reminderDateWritten(before, rename.selection)) {
                        onDueDateSet(ReminderNote(rename.name, rename.selection.date))
                    }
                }
            }
        )
    }

    val request = screenState.datePickerRequest
    if (request != null) {
        val requestListName = when (val target = request.target) {
            DateTarget.AddRow -> screenState.addRowText
            DateTarget.Rename -> screenState.rename?.name.orEmpty()
            is DateTarget.Row -> listOnPage(state, target.listId)?.name.orEmpty()
        }
        ListDatePickerDialog(
            listName = requestListName,
            initial = request.initial,
            today = today,
            kind = request.kind,
            animated = screenState.animationsEnabled,
            onDismiss = { screenState.datePickerRequest = null },
            onPicked = { date ->
                val owed = applyPickedDate(screenState, request, date) { listId, written ->
                    writeListDate(state, listId, written, onRenameList)
                }
                screenState.datePickerRequest = null
                if (owed) {
                    reminderNoteFor(screenState, state, request, date)?.let(onDueDateSet)
                }
                val rowTarget = request.target as? DateTarget.Row
                if (rowTarget != null) {
                    val list = listOnPage(state, rowTarget.listId)
                    val initMinute = list?.reminderTime?.let { t -> t.hour * 60 + t.minute }
                        ?: (reminderTime.hour * 60 + reminderTime.minute)
                    screenState.clockPickerRequest = ClockPickerRequest(rowTarget.listId, initMinute)
                }
            },
            onKindAsked = { kind -> screenState.datePickerRequest = request.copy(kind = kind) },
            onKindChange = { kind ->
                val moved = DateSelection(kind, request.initial)
                val owed = applyDateSelection(screenState, request, moved) { listId, written ->
                    writeListDate(state, listId, written, onRenameList)
                }
                screenState.datePickerRequest = request.copy(kind = kind)
                if (owed) {
                    reminderNoteFor(screenState, state, request, moved.date)?.let(onDueDateSet)
                }
            },
            onCleared = {
                applyDateSelection(screenState, request, DateSelection(request.kind, null)) {
                    listId, written ->
                    writeListDate(state, listId, written, onRenameList)
                }
                screenState.datePickerRequest = null
            }
        )
    }

    val clockReq = screenState.clockPickerRequest
    if (clockReq != null) {
        val clockList = listOnPage(state, clockReq.listId)
        ReminderClockPickerDialog(
            currentMinuteOfDay = clockReq.initialMinuteOfDay,
            onMinuteOfDayPicked = { minuteOfDay ->
                onSetListReminderTime(clockReq.listId, minuteOfDay)
                screenState.clockPickerRequest = null
            },
            onDismiss = { screenState.clockPickerRequest = null },
            animated = screenState.animationsEnabled,
            onRubOut = if (clockList?.reminderTime != null) {
                {
                    onSetListReminderTime(clockReq.listId, null)
                    screenState.clockPickerRequest = null
                }
            } else null
        )
    }
}

/**
 * The cork and everything pinned to it. The board is never wider than a desk's
 * worth of cards: on a wide window it stands in the middle of the cork, and the
 * columns are as many as fit.
 */
@Composable
private fun Board(
    state: TodoListsState,
    screenState: TodoListsScreenState,
    gridState: LazyStaggeredGridState,
    drag: BoardDrag,
    activeSummaries: List<TodoListSummary>,
    doneSummaries: List<TodoListSummary>,
    today: LocalDate,
    onOpenList: (TodoList) -> Unit,
    onCreateList: (String, LocalDate?, LocalDate?) -> Unit,
    onDueDateSet: (ReminderNote) -> Unit,
    onReorder: (List<String>) -> Unit,
    onActionsRequested: (TodoListSummary) -> Unit,
    onTorn: (String) -> Unit,
    onRewriteDate: (String) -> (DateSelection) -> Unit
) {
    val thisWeek = (state as? TodoListsState.Content)?.thisWeek.orEmpty()
    val activeIds = activeSummaries.map { it.list.id }
    val dropInListId = remember(activeIds) { screenState.dropInFor(activeIds) }
    val bars = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical).asPaddingValues()
    val edgeScroll = rememberEdgeScroll()
    val animated = screenState.animationsEnabled
    val drop: (List<String>?) -> Unit = { order ->
        if (order == null) {
            screenState.previewOrder = null
        } else {
            screenState.stageOrder(order)
            onReorder(order)
        }
    }
    val move: (Int, Int) -> (() -> Unit)? = { position, step ->
        val destination = position + step
        if (destination in activeIds.indices) {
            {
                val ordered = activeIds.moved(position, destination)
                gridState.holdBoard {
                    screenState.stageOrder(ordered)
                    onReorder(ordered)
                }
            }
        } else {
            null
        }
    }

    ScrollBoardWhileDragging(gridState, drag, edgeScroll)

    CorkSurface {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(CorkDimens.cardMinWidth),
            state = gridState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = CorkDimens.boardWidth)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .imePadding()
                .consumeWindowInsets(WindowInsets.safeDrawing)
                .onPlaced { drag.viewport = it.boundsInRoot() },
            contentPadding = PaddingValues(
                start = CorkDimens.boardMargin,
                end = CorkDimens.boardMargin,
                top = bars.calculateTopPadding() + BOARD_TOP,
                bottom = bars.calculateBottomPadding() + BoardBarHeight + PaperDimens.stickyPad + BOARD_FOOT
            ),
            horizontalArrangement = Arrangement.spacedBy(CorkDimens.cardGap),
            verticalItemSpacing = CorkDimens.cardGap
        ) {
            item(key = HEAD_KEY, contentType = HEAD_TYPE, span = StaggeredGridItemSpan.FullLine) {
                BoardHead(state = state, today = today)
            }
            item(key = ADD_KEY, contentType = ADD_TYPE, span = StaggeredGridItemSpan.FullLine) {
                AddLineCard(screenState = screenState, onCreateList = onCreateList, onDueDateSet = onDueDateSet)
            }
            if (thisWeek.isNotEmpty()) {
                item(key = WEEK_KEY, contentType = WEEK_TYPE, span = StaggeredGridItemSpan.FullLine) {
                    ThisWeekStrip(
                        thisWeek = thisWeek,
                        onOpenCalendar = {
                            if (!drag.dragging) screenState.tab = BoardTab.Calendar
                        },
                        modifier = animatedCard(animated)
                    )
                }
            }
            items(
                items = activeSummaries,
                key = { it.list.id },
                contentType = { CARD_TYPE }
            ) { summary ->
                val position = activeIds.indexOf(summary.list.id)
                val lifted = drag.draggedId == summary.list.id
                LiftHold {
                    ListCard(
                        summary = summary,
                        animated = animated,
                        onOpen = { onOpenList(summary.list) },
                        modifier = animatedCard(animated, lifted)
                            .dropIn(dropInListId == summary.list.id && animated)
                            .boardCard(
                                drag = drag,
                                id = summary.list.id,
                                ids = activeIds,
                                animated = animated,
                                onDrop = drop
                            ),
                        tearing = screenState.tearingId == summary.list.id,
                        onTorn = { onTorn(summary.list.id) },
                        onActionsRequested = { onActionsRequested(summary) },
                        onRewriteDate = onRewriteDate(summary.list.id),
                        onMoveUp = move(position, -1),
                        onMoveDown = move(position, +1)
                    )
                }
            }
            if (doneSummaries.isNotEmpty()) {
                item(key = FINISHED_KEY, contentType = HEADING_TYPE, span = StaggeredGridItemSpan.FullLine) {
                    FinishedHeading(
                        count = doneSummaries.size,
                        screenState = screenState,
                        modifier = animatedCard(animated)
                    )
                }
            }
            if (!screenState.finishedFolded) {
                items(
                    items = doneSummaries,
                    key = { it.list.id },
                    contentType = { DONE_TYPE }
                ) { summary ->
                    ListCard(
                        summary = summary,
                        animated = animated,
                        onOpen = { onOpenList(summary.list) },
                        modifier = animatedCard(animated),
                        tearing = screenState.tearingId == summary.list.id,
                        onTorn = { onTorn(summary.list.id) },
                        onActionsRequested = { onActionsRequested(summary) },
                        onRewriteDate = onRewriteDate(summary.list.id)
                    )
                }
            }
        }
    }
}

/**
 * The head of the board: a sheet pinned across the top carrying the cover — the
 * day, the notebook's name and how things stand. It is the notebook speaking, so it
 * is set in print.
 */
@Composable
private fun BoardHead(state: TodoListsState, today: LocalDate) {
    Box(modifier = Modifier.fillMaxWidth().pinnedPaper().padding(bottom = HEAD_STRIP_BOTTOM)) {
        CompositionLocalProvider(LocalPaperGutter provides HEAD_GUTTER) {
            ListsCover(state = state, today = today)
        }
    }
}

/**
 * The line a new list is written on, on a fresh card pinned under the head while
 * the pen is out. It is ruled like every line the reader writes on.
 */
@Composable
private fun AddLineCard(
    screenState: TodoListsScreenState,
    onCreateList: (String, LocalDate?, LocalDate?) -> Unit,
    onDueDateSet: (ReminderNote) -> Unit
) {
    AnimatedVisibility(
        visible = screenState.addRowExpanded,
        enter = unfoldFromHeadRule(screenState.animationsEnabled),
        exit = foldAwayFromPage(screenState.animationsEnabled)
    ) {
        Column(modifier = Modifier.fillMaxWidth().pinnedPaper().padding(vertical = LINE_STRIP_PADDING)) {
            InkAddLine(
                spoken = stringResource(R.string.add_list),
                commitSpoken = stringResource(R.string.commit_list),
                text = screenState.addRowText,
                onTextChange = { screenState.addRowText = it },
                onCommit = { _ ->
                    submitAddRow(screenState, onCreateList, onDueDateSet)
                },
                armed = screenState.addRowExpanded,
                onPenUp = { screenState.openAddRow() },
                onPenDown = { screenState.closeAddRow() },
                style = MaterialTheme.typography.titleMedium,
                animated = screenState.animationsEnabled,
                keepsPenAfterCommit = false
            )
            AnimatedVisibility(
                visible = dateMarksOwed(screenState),
                enter = unfoldFromHeadRule(screenState.animationsEnabled),
                exit = foldAwayFromPage(screenState.animationsEnabled)
            ) {
                AddLineDateRule(screenState)
            }
        }
    }
}

/**
 * The finished lists are headed on a label of their own, pinned to the cork, and a
 * tap anywhere along it folds them away and back.
 */
@Composable
private fun FinishedHeading(count: Int, screenState: TodoListsScreenState, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.section_finished_lists)
    Box(modifier = modifier.fillMaxWidth().pinnedPaper()) {
        CompositionLocalProvider(LocalPaperGutter provides HEAD_GUTTER) {
            SectionHeading(
                title = title,
                count = count,
                spoken = pluralStringResource(R.plurals.section_lists_spoken, count, title, count),
                animated = screenState.animationsEnabled,
                folded = screenState.finishedFolded,
                onFold = { screenState.finishedFolded = !screenState.finishedFolded },
                foldedSpoken = stringResource(R.string.section_folded),
                openSpoken = stringResource(R.string.section_open)
            )
        }
    }
}

/**
 * Where the sheet taken off the pad is put down: the start of the line a new list
 * is written on, just under the board's head. The seat is arithmetic rather than a
 * measurement, so the sheet in the air is not dragged about by the board moving.
 */
@Composable
private fun addLineSeat(): () -> Offset {
    val density = LocalDensity.current
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    val top = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    val boardLeft = (windowWidth - minOf(windowWidth, CorkDimens.boardWidth)) / 2
    val pitch = fr.mandarine.todolist.ui.paper.LocalPagePitch.current
    val seat = with(density) {
        Offset(
            (boardLeft + CorkDimens.boardMargin + LocalPaperGutter.current).toPx(),
            (top + BOARD_TOP + coverHeight(pitch) + CorkDimens.cardGap).toPx()
        )
    }
    return remember(seat) { { seat } }
}

/**
 * The line being written wears its date marks on the rule beneath it, the way the
 * edit sheet does — but only once there are words for the date to belong to, so
 * the sheet taken off the pad still reads as one bare rule until it is written on.
 */
@Composable
private fun AddLineDateRule(screenState: TodoListsScreenState) {
    val selection = screenState.addRowSelection
    val said = rememberDateKindSaid()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    Column {
    RuledRow {
        DateMarks(
            selection = selection,
            said = said,
            onKindChange = { kind ->
                writeAddRowSelection(screenState, selection.withKind(kind))
            },
            onPickDate = { kind ->
                screenState.datePickerRequest = DatePickerRequest(
                    target = DateTarget.AddRow,
                    kind = kind,
                    initial = selection.date
                )
            },
            onClearDate = { confirmClear = true }
        )
    }
    DateKindCaption(said = said, animated = screenState.animationsEnabled)
    }
    /**
     * The line has no list yet, so the slip names what is being written rather
     * than a list that exists. It still asks: this is the same gesture as on a
     * committed row, and the spec asks on both routes.
     */
    if (confirmClear) {
        RemoveDateConfirmDialog(
            listName = screenState.addRowText,
            onCancel = { confirmClear = false },
            onRemove = {
                confirmClear = false
                screenState.addRowSelection = selection.cleared()
            }
        )
    }
}

internal fun dateMarksOwed(screenState: TodoListsScreenState): Boolean =
    screenState.addRowText.isNotBlank() || screenState.addRowSelection.date != null

/**
 * Writing a list puts the sheet down: nobody writes several lists in a row, so a
 * sheet left waiting was only one more thing to dismiss.
 */
internal fun submitAddRow(
    screenState: TodoListsScreenState,
    onCreateList: (String, LocalDate?, LocalDate?) -> Unit,
    onReminderWritten: (ReminderNote) -> Unit = {}
): Boolean {
    val name = screenState.addRowText
    if (name.isBlank()) return false
    val selection = screenState.addRowSelection
    onCreateList(name, selection.targetDate, selection.dueDate)
    if (selection.date != null) onReminderWritten(ReminderNote(name, selection.date))
    screenState.closeAddRow()
    return true
}

/**
 * Every route to a date goes through one of these two, and each answers the only
 * question the page asks of a write: did a due date just come into existence, and
 * is the notification ask therefore owed?
 */
internal fun applyPickedDate(
    screenState: TodoListsScreenState,
    request: DatePickerRequest,
    date: LocalDate,
    writeRowDate: (String, DateSelection) -> Boolean
): Boolean = applyDateSelection(screenState, request, DateSelection(request.kind, date), writeRowDate)

/**
 * Whatever the sheet has settled on — a day circled, a day moved to the other
 * mark, or a day rubbed out — written back to whoever asked for the sheet.
 */
internal fun applyDateSelection(
    screenState: TodoListsScreenState,
    request: DatePickerRequest,
    picked: DateSelection,
    writeRowDate: (String, DateSelection) -> Boolean
): Boolean {
    return when (val target = request.target) {
        /**
         * A day circled on a line that has not been committed has not created a
         * reminder yet, and may never — backing out of the line clears it. The ask
         * waits for the list to exist rather than being spent on a list that does
         * not.
         */
        DateTarget.AddRow -> {
            writeAddRowSelection(screenState, picked)
            false
        }
        DateTarget.Rename -> writeRenameSelection(screenState, picked)
        is DateTarget.Row -> writeRowDate(target.listId, picked)
    }
}

/**
 * A day circled for a row already on the page is written through at once, the way
 * the same jot on that list's own head rule writes it: the name is left as it is
 * and the one date the list carries is traded for the day just picked.
 */
internal fun writeListDate(
    state: TodoListsState,
    listId: String,
    written: DateSelection,
    onRenameList: (String, String, LocalDate?, LocalDate?, ListColour) -> Unit
): Boolean {
    val list = listOnPage(state, listId) ?: return false
    onRenameList(list.id, list.name, written.targetDate, written.dueDate, list.colour)
    return reminderDateWritten(DateSelection.of(list.targetDate, list.dueDate), written)
}

/**
 * Whose reminder was just written, so the slip that drops can carry the name the
 * reader gave it rather than the fact that something somewhere now rings.
 */
internal fun reminderNoteFor(
    screenState: TodoListsScreenState,
    state: TodoListsState,
    request: DatePickerRequest,
    day: LocalDate?
): ReminderNote? {
    val name = when (val target = request.target) {
        DateTarget.AddRow -> screenState.addRowText
        DateTarget.Rename -> screenState.rename?.name
        is DateTarget.Row -> listOnPage(state, target.listId)?.name
    }
    return name?.takeIf { it.isNotBlank() }?.let { ReminderNote(it, day) }
}

internal fun listOnPage(state: TodoListsState, listId: String): TodoList? =
    summaryOnPage(state, listId)?.list

internal fun summaryOnPage(state: TodoListsState, listId: String): TodoListSummary? =
    (state as? TodoListsState.Content)
        ?.let { it.activeSummaries + it.doneSummaries }
        ?.firstOrNull { it.list.id == listId }

internal fun writeAddRowSelection(
    screenState: TodoListsScreenState,
    written: DateSelection
): Boolean {
    val before = screenState.addRowSelection
    screenState.addRowSelection = written
    return reminderDateWritten(before, written)
}

/**
 * A day circled on the edit sheet is held on the sheet, not written through — the
 * sheet is put down to commit it and blanking the name throws it away. So nothing
 * is owed here: the ask belongs to the moment the reminder is persisted, which is
 * where the sheet's own confirm raises it.
 */
internal fun writeRenameSelection(
    screenState: TodoListsScreenState,
    written: DateSelection
): Boolean {
    val rename = screenState.rename ?: return false
    screenState.rename = rename.copy(selection = written)
    return false
}


/**
 * Keeps the board where it is while a change lands under it, so a card leaving or
 * moving does not carry the reader's place with it.
 */
private inline fun LazyStaggeredGridState.holdBoard(change: () -> Unit) {
    requestScrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    change()
}

/**
 * A card carried by the finger is not animated into place by the board — the
 * finger has it — but the board must not lose track of it either, so it snaps.
 */
@Composable
private fun LazyStaggeredGridItemScope.animatedCard(animated: Boolean, lifted: Boolean = false): Modifier =
    if (animated && !lifted) {
        Modifier.animateItem(
            fadeInSpec = PaperMotion.rowEnter,
            placementSpec = PaperMotion.rowPlacement,
            fadeOutSpec = PaperMotion.rowExit
        )
    } else {
        Modifier.animateItem(fadeInSpec = snap(), placementSpec = snap(), fadeOutSpec = snap())
    }

/**
 * The sheet taken off the pad lands as line one and unfolds down from the head
 * rule, pushing the page's own lines ahead of it; putting the pen down folds it
 * away again in one stroke.
 */
private fun unfoldFromHeadRule(animated: Boolean): EnterTransition =
    if (!animated) {
        EnterTransition.None
    } else {
        expandVertically(
            animationSpec = PaperMotion.rowUnfold,
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = PaperMotion.rowEnter)
    }

private fun foldAwayFromPage(animated: Boolean): ExitTransition =
    if (!animated) {
        ExitTransition.None
    } else {
        shrinkVertically(
            animationSpec = PaperMotion.rowFold,
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = PaperMotion.rowExit)
    }

/**
 * A created list falls onto the page from just above it, so the sheet peeled off
 * the pad and the row that follows read as one movement. Whether it plays is
 * fixed when the row first composes — the staged flag is cleared on the very next
 * frame, and a row must not stop mid-fall because of it.
 */
@Composable
private fun Modifier.dropIn(active: Boolean): Modifier {
    val plays = remember { active }
    val progress = remember { Animatable(if (plays) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (plays) progress.animateTo(1f, PaperMotion.rowEnter)
    }
    if (!plays) return this
    return graphicsLayer {
        alpha = progress.value
        translationY = -DROP_IN_TRAVEL.toPx() * (1f - progress.value)
    }
}
