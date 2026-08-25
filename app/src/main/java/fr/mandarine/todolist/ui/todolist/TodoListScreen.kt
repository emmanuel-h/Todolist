package fr.mandarine.todolist.ui.todolist

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.ui.UNDO_SLIP_MILLIS
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkAddLine
import fr.mandarine.todolist.ui.paper.InkBudget
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.PaperSurface
import fr.mandarine.todolist.ui.paper.SectionSkip
import fr.mandarine.todolist.ui.paper.UndoSlip
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.headMarginFade
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.keyboardSeam
import fr.mandarine.todolist.ui.paper.pageFrame
import fr.mandarine.todolist.ui.paper.pageVerticalInsets
import fr.mandarine.todolist.ui.paper.penStrike
import fr.mandarine.todolist.ui.paper.rememberPageTail
import fr.mandarine.todolist.ui.paper.rememberPaperHaptics
import fr.mandarine.todolist.ui.paper.rememberPenStrike
import fr.mandarine.todolist.ui.paper.ruledPage
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.paper.settleOnRule
import fr.mandarine.todolist.ui.reorder.AutoScrollWhileDragging
import fr.mandarine.todolist.ui.reorder.DragSession
import fr.mandarine.todolist.ui.reorder.EdgeScroll
import fr.mandarine.todolist.ui.reorder.LiftHold
import fr.mandarine.todolist.ui.reorder.liftToReorder
import fr.mandarine.todolist.ui.reorder.liftedSlip
import fr.mandarine.todolist.ui.reorder.orderedBy
import fr.mandarine.todolist.ui.reorder.rememberEdgeScroll
import fr.mandarine.todolist.ui.tutorial.tutorialAnchor
import kotlinx.coroutines.delay

internal const val INK_TICK_MILLIS = 220L
internal const val INK_STRIKE_MILLIS = 220L

private const val INLINE_ADD_KEY = "inline-add"
private const val HEAD_KEY = "head"
private const val SKIP_KEY = "completed-skip"
private const val HEAD_TYPE = "head"
private const val ACTIVE_TYPE = "active"
private const val SKIP_TYPE = "skip"
private const val INLINE_ADD_TYPE = "inline-add"
private const val COMPLETED_TYPE = "completed"

@Composable
fun TodoListScreen(
    listName: String,
    state: TodoListState,
    screenState: TodoListScreenState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSubmitInline: (String) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val content = state as? TodoListState.Content
    val deletion = screenState.deletion
    val activeItems = orderedBy(content?.activeItems.orEmpty(), screenState.previewOrder) { it.id }
        .filterNot { deletion.hides(it.id) }
    val completedItems = content?.completedItems.orEmpty().filterNot { deletion.hides(it.id) }
    val showSkip = activeItems.isNotEmpty() && completedItems.isNotEmpty()
    val allDone = activeItems.isEmpty() && completedItems.isNotEmpty()

    val pitch = LocalPagePitch.current
    val listState = rememberLazyListState()
    val tail = rememberPageTail(listState, pitch)
    val session = remember(screenState) {
        DragSession { order -> screenState.previewOrder = order }
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = rememberPaperHaptics()
    val liveIds = rememberUpdatedState(
        activeItems.map { it.id } + completedItems.map { it.id }
    )
    val allIds = (content?.activeItems.orEmpty() + content?.completedItems.orEmpty()).map { it.id }
    val requestToggle: (TodoItem) -> Unit = { item ->
        when {
            session.dragging -> Unit
            !screenState.animationsEnabled -> {
                if (item.isCompleted) haptics.untick() else haptics.tick()
                listState.holdPage { onToggle(item.id) }
            }
            screenState.pendingToggle == item.id -> screenState.pendingToggle = null
            else -> screenState.pendingToggle = item.id
        }
    }
    val requestDelete: (String) -> Unit = { id ->
        if (!session.dragging) {
            screenState.editingItemId = null
            deletion.request(id)?.let(onDelete)
        }
    }
    val holdTorn: (String) -> Unit = { id ->
        listState.holdPage {
            tail.absorb(id)
            deletion.markTorn()
        }
    }

    LaunchedEffect(screenState.pendingToggle) {
        val id = screenState.pendingToggle ?: return@LaunchedEffect
        delay(INK_TICK_MILLIS + INK_STRIKE_MILLIS)
        screenState.pendingToggle = null
        if (liveIds.value.contains(id)) listState.holdPage { onToggle(id) }
    }

    LaunchedEffect(deletion.pending?.id) {
        if (deletion.pending == null) return@LaunchedEffect
        delay(UNDO_SLIP_MILLIS)
        deletion.commit()?.let(onDelete)
    }

    LaunchedEffect(allIds) {
        deletion.forget(allIds.toSet())
    }

    LaunchedEffect(screenState.hideKeyboardSignal) {
        if (screenState.hideKeyboardSignal > 0) keyboard?.hide()
    }

    val edgeScroll = rememberEdgeScroll()

    AutoScrollWhileDragging(listState, session, edgeScroll)

    val insets = pageVerticalInsets()
    val headMargin = insets.calculateTopPadding() + pitch
    val palette = LocalPaperPalette.current
    val seam = keyboardSeam(deletion.pending == null)

    PaperSurface(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) {
        Box(modifier = Modifier.pageFrame().align(Alignment.TopCenter)) {
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
                        seam = seam
                    )
                    .headMarginFade(listState, headMargin)
                    .consumeWindowInsets(WindowInsets.safeDrawing),
                contentPadding = PaddingValues(
                    top = insets.calculateTopPadding(),
                    bottom = insets.calculateBottomPadding() + pitch + tail.height
                )
            ) {
                item(key = HEAD_KEY, contentType = HEAD_TYPE) {
                    HeadLine(
                        name = listName,
                        allDone = allDone,
                        animated = screenState.animationsEnabled
                    )
                }
                itemsIndexed(
                    items = activeItems,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> ACTIVE_TYPE }
                ) { position, item ->
                    ActiveRow(
                        item = item,
                        position = position,
                        rowIds = activeItems.map { it.id },
                        listState = listState,
                        session = session,
                        edgeScroll = edgeScroll,
                        screenState = screenState,
                        onToggle = requestToggle,
                        onEdit = onEdit,
                        onDeleteRequested = requestDelete,
                        onTorn = holdTorn,
                        onReorder = onReorder
                    )
                }
                item(key = INLINE_ADD_KEY, contentType = INLINE_ADD_TYPE) {
                    InkAddLine(
                        text = screenState.addRowText,
                        onTextChange = { screenState.addRowText = it },
                        onCommit = { title ->
                            onSubmitInline(title)
                            screenState.addRowText = ""
                        },
                        armed = screenState.addRowExpanded,
                        onPenUp = { screenState.addRowExpanded = true },
                        onPenDown = { screenState.addRowExpanded = false },
                        modifier = animatedRow(screenState)
                            .tutorialAnchor(screenState, TutorialAnchor.ItemGhostRow)
                            .tutorialAnchor(screenState, TutorialAnchor.SubmitItemButton),
                        style = MaterialTheme.typography.bodyLarge,
                        breathing = activeItems.isEmpty() && completedItems.isEmpty(),
                        animated = screenState.animationsEnabled
                    )
                }
                if (showSkip) {
                    item(key = SKIP_KEY, contentType = SKIP_TYPE) {
                        SectionSkip(
                            completedCount = completedItems.size,
                            modifier = animatedRow(screenState),
                            animated = screenState.animationsEnabled
                        )
                    }
                }
                itemsIndexed(
                    items = completedItems,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> COMPLETED_TYPE }
                ) { position, item ->
                    TodoRow(
                        item = item,
                        checked = screenState.inked(item),
                        editing = screenState.editingItemId == item.id,
                        onToggle = { requestToggle(item) },
                        onEditRequested = {
                            if (!session.dragging) screenState.editingItemId = item.id
                        },
                        onEditCommitted = { title -> onEdit(item.id, title) },
                        onEditDismissed = { screenState.editingItemId = null },
                        onDeleteRequested = { requestDelete(item.id) },
                        modifier = animatedRow(screenState),
                        toggleModifier = Modifier.tutorialAnchor(
                            screenState,
                            TutorialAnchor.CompletedItemToggle(position)
                        ),
                        animated = screenState.animationsEnabled,
                        tearing = deletion.tearing(item.id),
                        onTorn = { holdTorn(item.id) }
                    )
                }
            }
            BackGlyph(
                onBack = onBack,
                listState = listState,
                headMargin = headMargin,
                pitch = pitch
            )
        }
        UndoSlip(
            pending = deletion.pending?.id,
            window = UNDO_SLIP_MILLIS,
            onUndo = { listState.holdPage { deletion.undo()?.let(tail::release) } },
            modifier = Modifier.align(Alignment.BottomCenter),
            animated = screenState.animationsEnabled
        )
    }
}

@Composable
private fun LazyItemScope.ActiveRow(
    item: TodoItem,
    position: Int,
    rowIds: List<String>,
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll,
    screenState: TodoListScreenState,
    onToggle: (TodoItem) -> Unit,
    onEdit: (String, String) -> Unit,
    onDeleteRequested: (String) -> Unit,
    onTorn: (String) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val lifted = session.dragging && session.index == position
    val deletion = screenState.deletion
    val move: (Int) -> (() -> Unit)? = { step ->
        val destination = position + step
        if (destination in rowIds.indices) {
            {
                listState.holdPage {
                    screenState.previewOrder = null
                    onReorder(position, destination)
                }
            }
        } else {
            null
        }
    }

    LiftHold {
        TodoRow(
            item = item,
            checked = screenState.inked(item),
            editing = screenState.editingItemId == item.id,
            onToggle = { onToggle(item) },
            onEditRequested = { if (!session.dragging) screenState.editingItemId = item.id },
            onEditCommitted = { title -> onEdit(item.id, title) },
            onEditDismissed = { screenState.editingItemId = null },
            onDeleteRequested = { onDeleteRequested(item.id) },
            modifier = Modifier
                .then(if (lifted) Modifier else animatedRow(screenState))
                .liftedSlip(session, lifted, screenState.animationsEnabled)
                .liftToReorder(
                    listState = listState,
                    session = session,
                    edgeScroll = edgeScroll,
                    id = item.id,
                    ids = rowIds,
                    onDrop = { reorder ->
                        screenState.previewOrder = null
                        if (reorder != null) onReorder(reorder.from, reorder.to)
                    }
                )
                .tutorialAnchor(screenState, TutorialAnchor.ActiveItemRow(position))
                .tutorialAnchor(screenState, TutorialAnchor.ActiveItemDragHandle(position)),
            toggleModifier = Modifier
                .tutorialAnchor(screenState, TutorialAnchor.ActiveItemToggle(position)),
            animated = screenState.animationsEnabled,
            tearing = deletion.tearing(item.id),
            onTorn = { onTorn(item.id) },
            onMoveUp = move(-1),
            onMoveDown = move(+1)
        )
    }
}

private inline fun LazyListState.holdPage(change: () -> Unit) {
    requestScrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    change()
}

@Composable
private fun LazyItemScope.animatedRow(screenState: TodoListScreenState): Modifier =
    if (screenState.animationsEnabled) {
        Modifier.animateItem(
            fadeInSpec = PaperMotion.rowEnter,
            placementSpec = PaperMotion.rowPlacement,
            fadeOutSpec = PaperMotion.rowExit
        )
    } else {
        Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
    }

@Composable
private fun HeadLine(name: String, allDone: Boolean, animated: Boolean) {
    val palette = LocalPaperPalette.current
    val style = MaterialTheme.typography.titleLarge
    val strike = rememberPenStrike(name, allDone, animated)
    val ink = palette.inked(InkBudget.words(allDone))
    Row(
        modifier = Modifier.fillMaxWidth().height(LocalPagePitch.current),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = remember(name) { handwritten(name) },
            modifier = Modifier
                .padding(start = PaperDimens.gutter + PaperDimens.iconButton)
                .seatOnRule()
                .penStrike(strike, ink),
            style = style,
            color = ink,
            onTextLayout = strike::onTextLayout
        )
    }
}

@Composable
private fun BackGlyph(
    onBack: () -> Unit,
    listState: LazyListState,
    headMargin: Dp,
    pitch: Dp
) {
    InkIconButton(
        painter = painterResource(R.drawable.ic_arrow_back),
        contentDescription = stringResource(R.string.navigate_back),
        onClick = onBack,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .settleOnRule(listState, headMargin, pitch),
        tint = LocalPaperPalette.current.inked(InkTone.Words),
        seat = IconSeat.OnRule
    )
}
