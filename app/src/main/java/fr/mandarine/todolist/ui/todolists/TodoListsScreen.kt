package fr.mandarine.todolist.ui.todolists

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.PaperInk
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.PaperSurface
import fr.mandarine.todolist.ui.paper.SectionDivider
import fr.mandarine.todolist.ui.paper.StickyNotePad
import fr.mandarine.todolist.ui.reorder.AutoScrollWhileDragging
import fr.mandarine.todolist.ui.reorder.DragSession
import fr.mandarine.todolist.ui.reorder.EdgeScroll
import fr.mandarine.todolist.ui.reorder.orderedBy
import fr.mandarine.todolist.ui.reorder.rememberEdgeScroll
import fr.mandarine.todolist.ui.reorder.reorderHandle
import fr.mandarine.todolist.ui.tutorial.tutorialAnchor
import java.time.LocalDate

private const val DIVIDER_KEY = "done-divider"
private const val REPLAY_ALPHA = 0.45f
private val LIST_TOP_PADDING = 64.dp
private val LIST_BOTTOM_PADDING = 88.dp
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
    onReorder: (Int, Int) -> Unit,
    onReplayTutorial: () -> Unit
) {
    val content = state as? TodoListsState.Content
    val activeSummaries =
        orderedBy(content?.activeSummaries.orEmpty(), screenState.previewOrder) { it.list.id }
    val doneSummaries = content?.doneSummaries.orEmpty()
    val activeIds = activeSummaries.map { it.list.id }
    val dropInListId = remember(activeIds) { screenState.dropInFor(activeIds) }
    val firstRowId = (activeSummaries.firstOrNull() ?: doneSummaries.firstOrNull())?.list?.id

    val listState = rememberLazyListState()
    val session = remember(screenState) {
        DragSession { order -> screenState.previewOrder = order }
    }
    val edgeScroll = rememberEdgeScroll()
    val keyboard = LocalSoftwareKeyboardController.current
    val horizontalInset = dimensionResource(R.dimen.list_horizontal_inset)

    LaunchedEffect(screenState.hideKeyboardSignal) {
        if (screenState.hideKeyboardSignal > 0) keyboard?.hide()
    }

    AutoScrollWhileDragging(listState, session, edgeScroll)

    PaperSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {
            if (screenState.addRowExpanded) {
                ListInlineAddRow(
                    text = screenState.addRowText,
                    selection = screenState.addRowSelection,
                    onTextChange = { screenState.addRowText = it },
                    onCancel = { screenState.closeAddRow() },
                    onPickTargetDate = {
                        screenState.datePickerRequest = DatePickerRequest(
                            target = DateTarget.ADD_ROW,
                            kind = DateKind.TARGET,
                            initial = screenState.addRowSelection.targetDate
                        )
                    },
                    onPickDueDate = {
                        screenState.datePickerRequest = DatePickerRequest(
                            target = DateTarget.ADD_ROW,
                            kind = DateKind.DUE,
                            initial = screenState.addRowSelection.dueDate
                        )
                    },
                    onSubmit = { submitAddRow(screenState, onCreateList) },
                    modifier = Modifier.tutorialAnchor(screenState, TutorialAnchor.ListCreateRow),
                    nameFieldModifier = Modifier
                        .tutorialAnchor(screenState, TutorialAnchor.ListNameField),
                    targetDateModifier = Modifier
                        .tutorialAnchor(screenState, TutorialAnchor.TargetDateButton),
                    dueDateModifier = Modifier
                        .tutorialAnchor(screenState, TutorialAnchor.DueDateButton),
                    submitModifier = Modifier
                        .tutorialAnchor(screenState, TutorialAnchor.SubmitListButton)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalInset,
                    end = horizontalInset,
                    top = LIST_TOP_PADDING,
                    bottom = LIST_BOTTOM_PADDING
                )
            ) {
                itemsIndexed(activeSummaries, key = { _, it -> it.list.id }) { position, summary ->
                    ActiveListRow(
                        summary = summary,
                        position = position,
                        activeIds = activeIds,
                        dropIn = dropInListId == summary.list.id,
                        firstRow = firstRowId == summary.list.id,
                        listState = listState,
                        session = session,
                        edgeScroll = edgeScroll,
                        screenState = screenState,
                        onOpenList = onOpenList,
                        onDeleteList = onDeleteList,
                        onReorder = onReorder
                    )
                }
                if (activeSummaries.isNotEmpty() && doneSummaries.isNotEmpty()) {
                    item(key = DIVIDER_KEY) {
                        SectionDivider(
                            completedCount = doneSummaries.size,
                            modifier = animatedRow(screenState)
                        )
                    }
                }
                itemsIndexed(doneSummaries, key = { _, it -> it.list.id }) { _, summary ->
                    val firstRow = firstRowId == summary.list.id
                    TodoListRow(
                        summary = summary,
                        confirmingDelete = screenState.confirmingDeleteListId == summary.list.id,
                        animated = screenState.animationsEnabled,
                        onOpen = { onOpenList(summary.list) },
                        onRename = { screenState.rename = RenameState.of(summary.list) },
                        onDeleteRequested = {
                            screenState.confirmingDeleteListId = summary.list.id
                        },
                        onDeleteCancelled = { screenState.confirmingDeleteListId = null },
                        onDeleteConfirmed = {
                            screenState.confirmingDeleteListId = null
                            onDeleteList(summary.list.id)
                        },
                        modifier = animatedRow(screenState)
                            .then(rowAnchor(screenState, firstRow, TutorialAnchor.FirstListRow)),
                        deleteModifier =
                            rowAnchor(screenState, firstRow, TutorialAnchor.DeleteListButton),
                        confirmModifier =
                            rowAnchor(screenState, firstRow, TutorialAnchor.ConfirmDeleteButton)
                    )
                }
            }
        }

        if (!screenState.addRowExpanded) {
            InkIconButton(
                painter = painterResource(R.drawable.ic_replay),
                contentDescription = stringResource(R.string.replay_tutorial),
                onClick = onReplayTutorial,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(CORNER_MARGIN)
                    .alpha(REPLAY_ALPHA),
                tint = PaperInk.pencil
            )
        }
        StickyNotePad(
            onTake = { screenState.openAddRow() },
            contentDescription = stringResource(R.string.add_list_fab_description),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(CORNER_MARGIN)
                .tutorialAnchor(screenState, TutorialAnchor.CreateListButton),
            taken = screenState.addRowExpanded,
            reducedMotion = !screenState.animationsEnabled
        )
    }

    val rename = screenState.rename
    if (rename != null) {
        RenameListDialog(
            state = rename,
            onNameChange = { screenState.rename = rename.copy(name = it) },
            onKindChange = {
                screenState.rename = rename.copy(selection = rename.selection.withKind(it))
            },
            onPickDate = {
                screenState.datePickerRequest = DatePickerRequest(
                    target = DateTarget.RENAME,
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
            onDismiss = { screenState.datePickerRequest = null },
            onPicked = { date ->
                applyPickedDate(screenState, request, date)
                screenState.datePickerRequest = null
            }
        )
    }
}

internal fun submitAddRow(
    screenState: TodoListsScreenState,
    onCreateList: (String, LocalDate?, LocalDate?) -> Unit
): Boolean {
    val name = screenState.addRowText
    if (name.isBlank()) return false
    val selection = screenState.addRowSelection
    onCreateList(name, selection.targetDate, selection.dueDate)
    screenState.closeAddRow()
    return true
}

internal fun applyPickedDate(
    screenState: TodoListsScreenState,
    request: DatePickerRequest,
    date: LocalDate
) {
    when (request.target) {
        DateTarget.ADD_ROW -> screenState.addRowSelection = DateSelection(request.kind, date)
        DateTarget.RENAME -> {
            val rename = screenState.rename ?: return
            screenState.rename = rename.copy(
                selection = DateSelection(request.kind, date)
            )
        }
    }
}

@Composable
private fun LazyItemScope.ActiveListRow(
    summary: TodoListSummary,
    position: Int,
    activeIds: List<String>,
    dropIn: Boolean,
    firstRow: Boolean,
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll,
    screenState: TodoListsScreenState,
    onOpenList: (TodoList) -> Unit,
    onDeleteList: (String) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val dragged = session.dragging && session.index == position
    val rowModifier = if (dragged) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = session.offset }
            .background(PaperInk.paperSheet)
    } else {
        animatedRow(screenState).dropIn(dropIn && screenState.animationsEnabled)
    }

    TodoListRow(
        summary = summary,
        confirmingDelete = screenState.confirmingDeleteListId == summary.list.id,
        animated = screenState.animationsEnabled,
        onOpen = { onOpenList(summary.list) },
        onRename = { screenState.rename = RenameState.of(summary.list) },
        onDeleteRequested = { screenState.confirmingDeleteListId = summary.list.id },
        onDeleteCancelled = { screenState.confirmingDeleteListId = null },
        onDeleteConfirmed = {
            screenState.confirmingDeleteListId = null
            onDeleteList(summary.list.id)
        },
        modifier = rowModifier
            .then(rowAnchor(screenState, firstRow, TutorialAnchor.FirstListRow)),
        handleModifier = Modifier.reorderHandle(
            listState = listState,
            session = session,
            edgeScroll = edgeScroll,
            id = summary.list.id,
            ids = activeIds,
            onDrop = { reorder ->
                if (reorder == null) {
                    screenState.previewOrder = null
                } else {
                    onReorder(reorder.from, reorder.to)
                }
            }
        ),
        deleteModifier = rowAnchor(screenState, firstRow, TutorialAnchor.DeleteListButton),
        confirmModifier = rowAnchor(screenState, firstRow, TutorialAnchor.ConfirmDeleteButton)
    )
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

@Composable
private fun LazyItemScope.animatedRow(screenState: TodoListsScreenState): Modifier =
    if (screenState.animationsEnabled) {
        Modifier.animateItem(
            fadeInSpec = PaperMotion.rowEnter,
            placementSpec = PaperMotion.rowPlacement,
            fadeOutSpec = PaperMotion.rowExit
        )
    } else {
        Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
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
