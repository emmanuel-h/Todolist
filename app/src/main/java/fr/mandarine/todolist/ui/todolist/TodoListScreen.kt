package fr.mandarine.todolist.ui.todolist

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperInk
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.PaperSurface

private const val INLINE_ADD_KEY = "inline-add"
private const val DIVIDER_KEY = "completed-divider"
private val TOOLBAR_HEIGHT = 56.dp
private val TOOLBAR_TITLE_GAP = 4.dp
private val LIST_TOP_PADDING = 4.dp
private val LIST_BOTTOM_PADDING = 16.dp
private val AUTO_SCROLL_EDGE = 72.dp
private val AUTO_SCROLL_MAX_STEP = 12.dp

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
    val activeItems = orderActive(content?.activeItems.orEmpty(), screenState.previewOrder)
    val completedItems = content?.completedItems.orEmpty()
    val showDivider = activeItems.isNotEmpty() && completedItems.isNotEmpty()

    val listState = rememberLazyListState()
    val session = remember(screenState) {
        DragSession { order -> screenState.previewOrder = order }
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(screenState.hideKeyboardSignal) {
        if (screenState.hideKeyboardSignal > 0) keyboard?.hide()
    }

    val density = LocalDensity.current
    val edgeScroll = remember(density) {
        EdgeScroll(
            edge = with(density) { AUTO_SCROLL_EDGE.toPx() },
            maxStep = with(density) { AUTO_SCROLL_MAX_STEP.toPx() }
        )
    }

    AutoScrollWhileDragging(listState, session, edgeScroll)

    PaperSurface(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {
            PaperTopBar(title = listName, onBack = onBack)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = LIST_TOP_PADDING,
                    bottom = LIST_BOTTOM_PADDING
                )
            ) {
                itemsIndexed(activeItems, key = { _, item -> item.id }) { position, item ->
                    ActiveRow(
                        item = item,
                        position = position,
                        activeItems = activeItems,
                        listState = listState,
                        session = session,
                        edgeScroll = edgeScroll,
                        screenState = screenState,
                        onToggle = onToggle,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onReorder = onReorder
                    )
                }
                item(key = INLINE_ADD_KEY) {
                    InlineAddRow(
                        expanded = screenState.addRowExpanded,
                        text = screenState.addRowText,
                        onTextChange = { screenState.addRowText = it },
                        onExpand = { screenState.addRowExpanded = true },
                        onCollapse = { screenState.addRowExpanded = false },
                        onSubmit = {
                            onSubmitInline(screenState.addRowText)
                            screenState.addRowText = ""
                        },
                        modifier = animatedRow(screenState),
                        ghostModifier = Modifier.tutorialAnchor(
                            screenState,
                            TutorialAnchor.ItemGhostRow
                        ),
                        submitModifier = Modifier.tutorialAnchor(
                            screenState,
                            TutorialAnchor.SubmitItemButton
                        )
                    )
                }
                if (showDivider) {
                    item(key = DIVIDER_KEY) {
                        SectionDivider(
                            completedCount = completedItems.size,
                            modifier = animatedRow(screenState)
                        )
                    }
                }
                itemsIndexed(completedItems, key = { _, item -> item.id }) { position, item ->
                    TodoRow(
                        item = item,
                        editing = screenState.editingItemId == item.id,
                        onToggle = { onToggle(item.id) },
                        onEditRequested = { screenState.editingItemId = item.id },
                        onEditCommitted = { title -> onEdit(item.id, title) },
                        onEditDismissed = { screenState.editingItemId = null },
                        onDelete = { onDelete(item.id) },
                        modifier = animatedRow(screenState),
                        toggleModifier = Modifier.tutorialAnchor(
                            screenState,
                            TutorialAnchor.CompletedItemToggle(position)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.ActiveRow(
    item: TodoItem,
    position: Int,
    activeItems: List<TodoItem>,
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll,
    screenState: TodoListScreenState,
    onToggle: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val dragged = session.dragging && session.index == position
    val view = LocalView.current
    val latestItems = rememberUpdatedState(activeItems)

    val rowModifier = if (dragged) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = session.offset }
            .background(PaperInk.paperSheet)
    } else {
        animatedRow(screenState)
    }

    TodoRow(
        item = item,
        editing = screenState.editingItemId == item.id,
        onToggle = { onToggle(item.id) },
        onEditRequested = { screenState.editingItemId = item.id },
        onEditCommitted = { title -> onEdit(item.id, title) },
        onEditDismissed = { screenState.editingItemId = null },
        onDelete = { onDelete(item.id) },
        modifier = rowModifier
            .tutorialAnchor(screenState, TutorialAnchor.ActiveItemRow(position)),
        handleModifier = Modifier
            .tutorialAnchor(screenState, TutorialAnchor.ActiveItemDragHandle(position))
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = {
                        val items = latestItems.value
                        session.start(
                            from = items.indexOfFirst { it.id == item.id },
                            rowIds = items.map { it.id },
                            rowHeights = activeRowHeights(listState, items)
                        )
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        session.drag(amount.y)
                        session.edgeScrolling =
                            edgeScrollDelta(listState, session, edgeScroll) != 0f
                    },
                    onDragEnd = {
                        val reorder = session.end()
                        if (reorder == null) {
                            screenState.previewOrder = null
                        } else {
                            view.performDropFeedback()
                            onReorder(reorder.from, reorder.to)
                        }
                    },
                    onDragCancel = {
                        session.cancel()
                        screenState.previewOrder = null
                    }
                )
            },
        toggleModifier = Modifier
            .tutorialAnchor(screenState, TutorialAnchor.ActiveItemToggle(position))
    )
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
private fun PaperTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOOLBAR_HEIGHT)
            .padding(start = PaperDimens.toolbarInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InkIconButton(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.navigate_back),
            onClick = onBack,
            tint = PaperInk.ink
        )
        Text(
            text = title,
            modifier = Modifier.padding(start = TOOLBAR_TITLE_GAP),
            style = MaterialTheme.typography.titleLarge,
            color = PaperInk.ink
        )
    }
}

private class EdgeScroll(val edge: Float, val maxStep: Float)

private fun edgeScrollDelta(
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll
): Float {
    val info = listState.layoutInfo
    val dragged = info.visibleItemsInfo.firstOrNull { it.index == session.index } ?: return 0f
    val top = dragged.offset - info.viewportStartOffset + session.offset
    return autoScrollDelta(
        rowTop = top,
        rowBottom = top + dragged.size,
        viewportHeight = (info.viewportEndOffset - info.viewportStartOffset).toFloat(),
        edge = edgeScroll.edge,
        maxStep = edgeScroll.maxStep,
        canScrollUp = listState.canScrollBackward,
        canScrollDown = listState.canScrollForward
    )
}

/**
 * A finger held still inside the edge band must keep the list moving, so the
 * scroll is driven by frames rather than by drag events. It only spins while the
 * band is occupied — running it for the whole drag would keep the frame clock
 * busy for as long as a finger is down. Whatever it scrolls is fed back into the
 * session so the row stays under the finger and keeps swapping rows.
 */
@Composable
private fun AutoScrollWhileDragging(
    listState: LazyListState,
    session: DragSession,
    edgeScroll: EdgeScroll
) {
    LaunchedEffect(session.edgeScrolling) {
        while (session.edgeScrolling && session.dragging) {
            withFrameNanos { }
            val delta = edgeScrollDelta(listState, session, edgeScroll)
            if (delta == 0f) {
                session.edgeScrolling = false
            } else {
                listState.scrollBy(delta)
                session.drag(delta)
            }
        }
    }
}

private fun activeRowHeights(listState: LazyListState, items: List<TodoItem>): List<Int> {
    val sizes = listState.layoutInfo.visibleItemsInfo.associate { it.key to it.size }
    val fallback = sizes.values.firstOrNull() ?: 0
    return items.map { sizes[it.id] ?: fallback }
}

private fun View.performDropFeedback() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}
