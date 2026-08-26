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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.UNDO_SLIP_MILLIS
import fr.mandarine.todolist.ui.paper.InkAddLine
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperGutter
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.PaperSurface
import fr.mandarine.todolist.ui.paper.RuledRow
import fr.mandarine.todolist.ui.paper.SectionSkip
import fr.mandarine.todolist.ui.paper.StickyNotePad
import fr.mandarine.todolist.ui.paper.StickyNotePutBack
import fr.mandarine.todolist.ui.paper.UndoSlip
import fr.mandarine.todolist.ui.paper.headMarginFade
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.keyboardSeam
import fr.mandarine.todolist.ui.paper.pageFrame
import fr.mandarine.todolist.ui.paper.pageVerticalInsets
import fr.mandarine.todolist.ui.paper.rememberPageBend
import fr.mandarine.todolist.ui.paper.rememberPageTail
import fr.mandarine.todolist.ui.paper.ruledPage
import fr.mandarine.todolist.ui.reorder.AutoScrollWhileDragging
import fr.mandarine.todolist.ui.reorder.DragSession
import fr.mandarine.todolist.ui.reorder.EdgeScroll
import fr.mandarine.todolist.ui.reorder.LiftHold
import fr.mandarine.todolist.ui.reorder.liftToReorder
import fr.mandarine.todolist.ui.reorder.liftedSlip
import fr.mandarine.todolist.ui.reorder.moved
import fr.mandarine.todolist.ui.reorder.orderedBy
import fr.mandarine.todolist.ui.reorder.rememberEdgeScroll
import fr.mandarine.todolist.ui.tutorial.tutorialAnchor
import java.time.LocalDate
import kotlinx.coroutines.delay

private const val HEAD_KEY = "head"
private const val ADD_KEY = "list-add"
private const val SKIP_KEY = "done-skip"
private const val HEAD_TYPE = "head"
private const val ADD_TYPE = "list-add"
private const val ACTIVE_TYPE = "active"
private const val SKIP_TYPE = "skip"
private const val DONE_TYPE = "done"
private const val REPLAY_ALPHA = 0.8f
private val CORNER_MARGIN = 8.dp
private val DROP_IN_TRAVEL = 16.dp

@Composable
fun TodoListsScreen(
    state: TodoListsState,
    screenState: TodoListsScreenState,
    today: LocalDate,
    onOpenList: (TodoList) -> Unit,
    onCreateList: (String, LocalDate?, LocalDate?) -> Unit,
    onRenameList: (String, String, LocalDate?, LocalDate?) -> Unit,
    onDeleteList: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onReplayTutorial: () -> Unit,
    onDueDateSet: () -> Unit = {}
) {
    val content = state as? TodoListsState.Content
    val deletion = screenState.deletion
    /**
     * Hidden first, then staged. A staged order names the rows the reader can
     * see, so measuring it against a set that still holds a torn-off row made it
     * the wrong length and threw the whole preview away on every frame.
     */
    val publishedSummaries =
        content?.activeSummaries.orEmpty().filterNot { deletion.hides(it.list.id) }
    val activeSummaries = orderedBy(publishedSummaries, screenState.previewOrder) { it.list.id }
    val doneSummaries = content?.doneSummaries.orEmpty().filterNot { deletion.hides(it.list.id) }
    val activeIds = activeSummaries.map { it.list.id }
    val publishedIds = publishedSummaries.map { it.list.id }
    LaunchedEffect(publishedIds) { screenState.releaseOrder(publishedIds) }
    val dropInListId = remember(activeIds) { screenState.dropInFor(activeIds) }
    val firstRowId = (activeSummaries.firstOrNull() ?: doneSummaries.firstOrNull())?.list?.id
    val allIds = (content?.activeSummaries.orEmpty() + content?.doneSummaries.orEmpty())
        .map { it.list.id }

    val pageEmpty = activeSummaries.isEmpty() && doneSummaries.isEmpty()
    val pitch = LocalPagePitch.current
    val listState = rememberLazyListState()
    val tail = rememberPageTail(listState, pitch)
    val session = remember(screenState) {
        DragSession { order -> screenState.previewOrder = order }
    }
    val edgeScroll = rememberEdgeScroll()
    val focusManager = LocalFocusManager.current
    val insets = pageVerticalInsets()
    val topInset = insets.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()
    val headMargin = topInset + pitch
    val palette = LocalPaperPalette.current
    val gutter = LocalPaperGutter.current
    val headRuleSeat = headRuleSeat(headMargin, gutter)
    val seam = keyboardSeam(deletion.pending == null)
    val bend = rememberPageBend(screenState.animationsEnabled)
    /**
     * The pad keeps its footprint for as long as it is standing there, which is
     * always: a sheet taken off it leaves the rest of the pad in the corner. Only
     * the resting inset under it is given up while the line is open, because the
     * page has already made room for the keyboard by then.
     */
    val padFootprint = padFootMargin(if (screenState.addRowExpanded) 0.dp else bottomInset)
    val restingSpace = (bottomInset - padFootprint).coerceAtLeast(0.dp)

    val requestDelete: (String) -> Unit = { id ->
        if (!session.dragging) deletion.request(id)?.let(onDeleteList)
    }
    /**
     * A tap on a row while the pen is still on the add line finishes that line
     * rather than leaving the screen with the words on it: on a page full of lists
     * there may be no blank paper left to tap, and a tap that loses what was
     * written reads as a mistake the app made.
     */
    val openList: (TodoList) -> Unit = { list ->
        when {
            session.dragging -> Unit
            screenState.addRowExpanded -> {
                submitAddRow(screenState, onCreateList, onDueDateSet)
                screenState.closeAddRow()
            }
            else -> onOpenList(list)
        }
    }
    val holdTorn: (String) -> Unit = { id ->
        listState.holdPage {
            tail.absorb(id)
            deletion.markTorn()
        }
    }
    /**
     * The jot in a row's margin is the same mark as the jot on that list's own head
     * rule, so it opens the same calendar on the same day rather than counting as a
     * tap on the row it is written beside.
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

    LaunchedEffect(deletion.pending?.id) {
        if (deletion.pending == null) return@LaunchedEffect
        delay(UNDO_SLIP_MILLIS)
        deletion.commit()?.let(onDeleteList)
    }

    /**
     * The slip counts down on the page's own coroutine, so a page that leaves
     * takes the countdown with it. Leaving commits instead of forgetting: the
     * tear was the decision, and the reader watched the row come off.
     */
    val commitOnLeaving = rememberUpdatedState(onDeleteList)
    DisposableEffect(deletion) {
        onDispose { deletion.commit()?.let(commitOnLeaving.value) }
    }

    LaunchedEffect(allIds) {
        deletion.forget(allIds.toSet())
    }

    AutoScrollWhileDragging(listState, session, edgeScroll)

    PaperSurface(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) {
        Column(modifier = Modifier.pageFrame(bend).align(Alignment.TopCenter)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .ruledPage(
                        listState = listState,
                        pitch = pitch,
                        headMargin = headMargin,
                        color = palette.rule,
                        seamColor = palette.keyboardSeam,
                        seam = seam,
                        gutter = gutter
                    )
                    .headMarginFade(listState, headMargin)
                    .consumeWindowInsets(WindowInsets.safeDrawing)
                    .padding(bottom = padFootprint),
                contentPadding = PaddingValues(
                    top = topInset,
                    bottom = restingSpace + pitch + tail.height
                ),
                overscrollEffect = bend
            ) {
                item(key = HEAD_KEY, contentType = HEAD_TYPE) {
                    Spacer(Modifier.height(pitch))
                }
                item(key = ADD_KEY, contentType = ADD_TYPE) {
                    AnimatedVisibility(
                        visible = screenState.addRowExpanded,
                        enter = unfoldFromHeadRule(screenState.animationsEnabled),
                        exit = foldAwayFromPage(screenState.animationsEnabled)
                    ) {
                        Column {
                            InkAddLine(
                                spoken = stringResource(R.string.add_list),
                                text = screenState.addRowText,
                                onTextChange = { screenState.addRowText = it },
                                onCommit = { _ ->
                                    submitAddRow(screenState, onCreateList, onDueDateSet)
                                },
                                armed = screenState.addRowExpanded,
                                onPenUp = { screenState.openAddRow() },
                                onPenDown = { screenState.closeAddRow() },
                                modifier = Modifier
                                    .tutorialAnchor(screenState, TutorialAnchor.ListCreateRow)
                                    .tutorialAnchor(screenState, TutorialAnchor.SubmitListButton),
                                fieldModifier = Modifier
                                    .tutorialAnchor(screenState, TutorialAnchor.ListNameField),
                                style = MaterialTheme.typography.titleMedium,
                                animated = screenState.animationsEnabled
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
                itemsIndexed(
                    items = activeSummaries,
                    key = { _, it -> it.list.id },
                    contentType = { _, _ -> ACTIVE_TYPE }
                ) { position, summary ->
                    ActiveListRow(
                        summary = summary,
                        position = position,
                        rowIds = activeIds,
                        dropIn = dropInListId == summary.list.id,
                        firstRow = firstRowId == summary.list.id,
                        listState = listState,
                        session = session,
                        edgeScroll = edgeScroll,
                        screenState = screenState,
                        onOpenList = openList,
                        onDeleteRequested = requestDelete,
                        onTorn = holdTorn,
                        onReorder = onReorder,
                        onRewriteDate = rewriteDate(summary.list.id)
                    )
                }
                if (activeSummaries.isNotEmpty() && doneSummaries.isNotEmpty()) {
                    item(key = SKIP_KEY, contentType = SKIP_TYPE) {
                        SectionSkip(
                            completedCount = doneSummaries.size,
                            spoken = pluralStringResource(
                                R.plurals.done_lists,
                                doneSummaries.size,
                                doneSummaries.size
                            ),
                            modifier = animatedRow(screenState),
                            animated = screenState.animationsEnabled
                        )
                    }
                }
                itemsIndexed(
                    items = doneSummaries,
                    key = { _, it -> it.list.id },
                    contentType = { _, _ -> DONE_TYPE }
                ) { _, summary ->
                    val firstRow = firstRowId == summary.list.id
                    TodoListRow(
                        summary = summary,
                        animated = screenState.animationsEnabled,
                        onOpen = { openList(summary.list) },
                        onDeleteRequested = { requestDelete(summary.list.id) },
                        modifier = animatedRow(screenState)
                            .then(rowAnchor(screenState, firstRow, TutorialAnchor.FirstListRow))
                            .then(
                                rowAnchor(screenState, firstRow, TutorialAnchor.DeleteListButton)
                            ),
                        tearing = deletion.tearing(summary.list.id),
                        onTorn = { holdTorn(summary.list.id) },
                        onRenameRequested = {
                            screenState.rename = RenameState.of(summary.list)
                        },
                        onRewriteDate = rewriteDate(summary.list.id)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !screenState.addRowExpanded,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(CORNER_MARGIN),
            enter = fadeIn(PaperMotion.rowEnter),
            exit = fadeOut(PaperMotion.rowExit)
        ) {
            InkIconButton(
                painter = painterResource(R.drawable.ic_help),
                contentDescription = stringResource(R.string.replay_tutorial),
                onClick = onReplayTutorial,
                modifier = Modifier.alpha(REPLAY_ALPHA),
                tint = LocalPaperPalette.current.inked(InkTone.Margin)
            )
        }
        StickyNotePad(
            onTake = { screenState.openAddRow() },
            contentDescription = stringResource(R.string.add_list_fab_description),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(CORNER_MARGIN)
                .tutorialAnchor(screenState, TutorialAnchor.CreateListButton),
            taken = screenState.addRowExpanded,
            reducedMotion = !screenState.animationsEnabled,
            beckons = pageEmpty,
            landing = headRuleSeat,
            putBack = StickyNotePutBack(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = stringResource(R.string.discard_list),
                onPress = {
                    focusManager.clearFocus()
                    screenState.abandonAddRow()
                }
            )
        )
        UndoSlip(
            pending = deletion.pending?.id,
            window = UNDO_SLIP_MILLIS,
            onUndo = { listState.holdPage { deletion.undo()?.let(tail::release) } },
            modifier = Modifier.align(Alignment.BottomCenter),
            animated = screenState.animationsEnabled
        )
    }

    val rename = screenState.rename
    if (rename != null) {
        RenameListDialog(
            state = rename,
            onNameChange = { screenState.rename = rename.copy(name = it) },
            onKindChange = { kind ->
                if (writeRenameSelection(screenState, rename.selection.withKind(kind))) {
                    onDueDateSet()
                }
            },
            onPickDate = {
                screenState.datePickerRequest = DatePickerRequest(
                    target = DateTarget.Rename,
                    kind = rename.selection.kind,
                    initial = rename.selection.date
                )
            },
            onClearDate = {
                screenState.rename = rename.copy(selection = rename.selection.cleared())
            },
            onDismiss = { screenState.rename = null },
            onConfirm = {
                if (rename.name.isNotBlank()) {
                    onRenameList(
                        rename.listId,
                        rename.name,
                        rename.selection.targetDate,
                        rename.selection.dueDate
                    )
                    screenState.rename = null
                }
            }
        )
    }

    val request = screenState.datePickerRequest
    if (request != null) {
        ListDatePickerDialog(
            initial = request.initial,
            today = today,
            animated = screenState.animationsEnabled,
            onDismiss = { screenState.datePickerRequest = null },
            onPicked = { date ->
                val owed = applyPickedDate(screenState, request, date) { listId, written ->
                    writeListDate(state, listId, written, onRenameList)
                }
                screenState.datePickerRequest = null
                if (owed) onDueDateSet()
            }
        )
    }
}

/**
 * The line being written wears its date marks on the rule beneath it, the way the
 * edit sheet does — but only once there are words for the date to belong to, so
 * the sheet taken off the pad still reads as one bare rule until it is written on.
 */
@Composable
private fun AddLineDateRule(screenState: TodoListsScreenState) {
    val selection = screenState.addRowSelection
    RuledRow {
        DateMarks(
            selection = selection,
            onKindChange = { kind ->
                writeAddRowSelection(screenState, selection.withKind(kind))
            },
            onPickDate = {
                screenState.datePickerRequest = DatePickerRequest(
                    target = DateTarget.AddRow,
                    kind = selection.kind,
                    initial = selection.date
                )
            },
            onClearDate = { screenState.addRowSelection = selection.cleared() },
            targetModifier = Modifier
                .tutorialAnchor(screenState, TutorialAnchor.TargetDateButton),
            dueModifier = Modifier
                .tutorialAnchor(screenState, TutorialAnchor.DueDateButton)
        )
    }
}

internal fun dateMarksOwed(screenState: TodoListsScreenState): Boolean =
    screenState.addRowText.isNotBlank() || screenState.addRowSelection.date != null

/**
 * Where on the page the sheet taken off the pad is being put down: the start of
 * the head rule, which is the line the reader is about to write on. The page is
 * centred in the window and its rows are indented by the gutter, so the seat is
 * arithmetic rather than a measurement — a measured one would be dragged around by
 * the page's own overscroll bend while the sheet was still in the air.
 */
@Composable
private fun headRuleSeat(headMargin: Dp, gutter: Dp): () -> Offset {
    val density = LocalDensity.current
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    val pageLeft = (windowWidth - minOf(windowWidth, PaperDimens.pageWidth)) / 2
    val seat = with(density) { Offset((pageLeft + gutter).toPx(), headMargin.toPx()) }
    return remember(seat) { { seat } }
}

/**
 * The pad lies on the page itself only when there is no desk around the page to
 * lie on. Where it does, the page keeps a foot margin the width of the pad's
 * footprint, so no row is ever written underneath it at any font scale — and
 * where the page is narrow enough to leave a margin beside it, the page keeps its
 * whole height.
 */
@Composable
private fun padFootMargin(bottomInset: Dp): Dp {
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    val reach = PaperDimens.stickyPad + CORNER_MARGIN
    return if (padLiesOnPage(windowWidth, PaperDimens.pageWidth, reach)) {
        bottomInset + reach
    } else {
        0.dp
    }
}

internal fun padLiesOnPage(windowWidth: Dp, pageWidth: Dp, reach: Dp): Boolean =
    (windowWidth - minOf(windowWidth, pageWidth)) / 2f < reach

/**
 * The line does not go away once a list is written on it: the sheet stays on the
 * page with a fresh caret waiting, so several lists can be written one after the
 * other. Putting the pen down is what ends the sheet.
 */
internal fun submitAddRow(
    screenState: TodoListsScreenState,
    onCreateList: (String, LocalDate?, LocalDate?) -> Unit,
    onReminderWritten: () -> Unit = {}
): Boolean {
    val name = screenState.addRowText
    if (name.isBlank()) return false
    val selection = screenState.addRowSelection
    onCreateList(name, selection.targetDate, selection.dueDate)
    if (selection.date != null) onReminderWritten()
    screenState.clearAddRow()
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
): Boolean {
    val picked = DateSelection(request.kind, date)
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
    onRenameList: (String, String, LocalDate?, LocalDate?) -> Unit
): Boolean {
    val list = listOnPage(state, listId) ?: return false
    onRenameList(list.id, list.name, written.targetDate, written.dueDate)
    return reminderDateWritten(DateSelection.of(list.targetDate, list.dueDate), written)
}

private fun listOnPage(state: TodoListsState, listId: String): TodoList? =
    (state as? TodoListsState.Content)
        ?.let { it.activeSummaries + it.doneSummaries }
        ?.firstOrNull { it.list.id == listId }
        ?.list

internal fun writeAddRowSelection(
    screenState: TodoListsScreenState,
    written: DateSelection
): Boolean {
    val before = screenState.addRowSelection
    screenState.addRowSelection = written
    return reminderDateWritten(before, written)
}

internal fun writeRenameSelection(
    screenState: TodoListsScreenState,
    written: DateSelection
): Boolean {
    val rename = screenState.rename ?: return false
    screenState.rename = rename.copy(selection = written)
    return reminderDateWritten(rename.selection, written)
}

@Composable
private fun LazyItemScope.ActiveListRow(
    summary: TodoListSummary,
    position: Int,
    rowIds: List<String>,
    dropIn: Boolean,
    firstRow: Boolean,
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll,
    screenState: TodoListsScreenState,
    onOpenList: (TodoList) -> Unit,
    onDeleteRequested: (String) -> Unit,
    onTorn: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onRewriteDate: (DateSelection) -> Unit
) {
    val lifted = session.dragging && session.index == position
    val deletion = screenState.deletion
    val resting = animatedRow(screenState, lifted)
        .dropIn(dropIn && screenState.animationsEnabled)
    val move: (Int) -> (() -> Unit)? = { step ->
        val destination = position + step
        if (destination in rowIds.indices) {
            {
                val ordered = rowIds.moved(position, destination)
                listState.holdPage {
                    screenState.stageOrder(ordered)
                    onReorder(ordered)
                }
            }
        } else {
            null
        }
    }

    LiftHold {
        TodoListRow(
            summary = summary,
            animated = screenState.animationsEnabled,
            onOpen = { onOpenList(summary.list) },
            onDeleteRequested = { onDeleteRequested(summary.list.id) },
            modifier = resting
                .liftedSlip(session, lifted, screenState.animationsEnabled)
                .liftToReorder(
                    listState = listState,
                    session = session,
                    edgeScroll = edgeScroll,
                    id = summary.list.id,
                    ids = rowIds,
                    onDrop = { reorder ->
                        if (reorder == null) {
                            screenState.previewOrder = null
                        } else {
                            screenState.stageOrder(reorder.orderedIds)
                            onReorder(reorder.orderedIds)
                        }
                    }
                )
                .then(rowAnchor(screenState, firstRow, TutorialAnchor.FirstListRow))
                .then(rowAnchor(screenState, firstRow, TutorialAnchor.DeleteListButton)),
            tearing = deletion.tearing(summary.list.id),
            onTorn = { onTorn(summary.list.id) },
            onRenameRequested = { screenState.rename = RenameState.of(summary.list) },
            onRewriteDate = onRewriteDate,
            onMoveUp = move(-1),
            onMoveDown = move(+1)
        )
    }
}

private inline fun LazyListState.holdPage(change: () -> Unit) {
    requestScrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    change()
}

/**
 * The tutorial only ever points at the top of the page, so only the first row
 * registers bounds — every other row would overwrite them. By the last scene the
 * demo list is finished, so that row can be below the divider.
 */
@Composable
private fun rowAnchor(
    screenState: TodoListsScreenState,
    firstRow: Boolean,
    anchor: TutorialAnchor
): Modifier = if (firstRow) {
    Modifier.tutorialAnchor(screenState, anchor)
} else {
    Modifier
}

/**
 * A lifted row is carried by the finger, not by the list, so the list must not
 * animate it — but it must not stop tracking it either. Taking the animation off
 * a row for the length of a drag detached the only node that remembered where
 * that row had been, so the drop handed the list a row it had no history for and
 * the list played it in as new. Snapping keeps the row where the finger leaves it
 * and keeps the page's memory of it intact.
 */
@Composable
private fun LazyItemScope.animatedRow(
    screenState: TodoListsScreenState,
    lifted: Boolean = false
): Modifier = when {
    lifted -> Modifier.animateItem(
        fadeInSpec = snap(),
        placementSpec = snap(),
        fadeOutSpec = snap()
    )

    screenState.animationsEnabled -> Modifier.animateItem(
        fadeInSpec = PaperMotion.rowEnter,
        placementSpec = PaperMotion.rowPlacement,
        fadeOutSpec = PaperMotion.rowExit
    )

    else -> Modifier.animateItem(fadeInSpec = snap(), placementSpec = snap(), fadeOutSpec = snap())
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
