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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.PaperSurface
import fr.mandarine.todolist.ui.paper.SectionSkip
import fr.mandarine.todolist.ui.paper.headMarginFade
import fr.mandarine.todolist.ui.paper.pageFrame
import fr.mandarine.todolist.ui.paper.pageVerticalInsets
import fr.mandarine.todolist.ui.paper.paperSheet
import fr.mandarine.todolist.ui.paper.performConfirmFeedback
import fr.mandarine.todolist.ui.paper.ruledPage
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.reorder.AutoScrollWhileDragging
import fr.mandarine.todolist.ui.reorder.DragSession
import fr.mandarine.todolist.ui.reorder.EdgeScroll
import fr.mandarine.todolist.ui.reorder.orderedBy
import fr.mandarine.todolist.ui.reorder.rememberEdgeScroll
import fr.mandarine.todolist.ui.reorder.reorderHandle
import fr.mandarine.todolist.ui.tutorial.tutorialAnchor

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
    val activeItems = orderedBy(content?.activeItems.orEmpty(), screenState.previewOrder) { it.id }
    val completedItems = content?.completedItems.orEmpty()
    val showSkip = activeItems.isNotEmpty() && completedItems.isNotEmpty()

    val pitch = LocalPagePitch.current
    val listState = rememberLazyListState()
    val session = remember(screenState) {
        DragSession { order -> screenState.previewOrder = order }
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val toggleWithFeedback: (String) -> Unit = { id ->
        view.performConfirmFeedback()
        onToggle(id)
    }

    LaunchedEffect(screenState.hideKeyboardSignal) {
        if (screenState.hideKeyboardSignal > 0) keyboard?.hide()
    }

    val edgeScroll = rememberEdgeScroll()

    AutoScrollWhileDragging(listState, session, edgeScroll)

    val insets = pageVerticalInsets()
    val headMargin = insets.calculateTopPadding() + pitch

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
                    .headMarginFade(listState, headMargin)
                    .ruledPage(
                        listState = listState,
                        pitch = pitch,
                        headMargin = headMargin,
                        color = LocalPaperPalette.current.rule
                    )
                    .consumeWindowInsets(WindowInsets.safeDrawing),
                contentPadding = PaddingValues(
                    top = insets.calculateTopPadding(),
                    bottom = insets.calculateBottomPadding() + pitch
                )
            ) {
                item(key = HEAD_KEY, contentType = HEAD_TYPE) {
                    HeadLine(name = listName)
                }
                itemsIndexed(
                    items = activeItems,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> ACTIVE_TYPE }
                ) { position, item ->
                    ActiveRow(
                        item = item,
                        position = position,
                        activeItems = activeItems,
                        listState = listState,
                        session = session,
                        edgeScroll = edgeScroll,
                        screenState = screenState,
                        onToggle = toggleWithFeedback,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onReorder = onReorder
                    )
                }
                item(key = INLINE_ADD_KEY, contentType = INLINE_ADD_TYPE) {
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
                if (showSkip) {
                    item(key = SKIP_KEY, contentType = SKIP_TYPE) {
                        SectionSkip(
                            completedCount = completedItems.size,
                            modifier = animatedRow(screenState)
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
                        editing = screenState.editingItemId == item.id,
                        onToggle = { toggleWithFeedback(item.id) },
                        onEditRequested = { screenState.editingItemId = item.id },
                        onEditCommitted = { title -> onEdit(item.id, title) },
                        onEditDismissed = { screenState.editingItemId = null },
                        onDelete = { onDelete(item.id) },
                        modifier = animatedRow(screenState),
                        toggleModifier = Modifier.tutorialAnchor(
                            screenState,
                            TutorialAnchor.CompletedItemToggle(position)
                        ),
                        animated = screenState.animationsEnabled
                    )
                }
            }
            BackGlyph(onBack = onBack)
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

    val rowModifier = if (dragged) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = session.offset }
            .paperSheet(tone = LocalPaperPalette.current.paperSheet)
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
            .reorderHandle(
                listState = listState,
                session = session,
                edgeScroll = edgeScroll,
                id = item.id,
                ids = activeItems.map { it.id },
                onDrop = { reorder ->
                    if (reorder == null) {
                        screenState.previewOrder = null
                    } else {
                        onReorder(reorder.from, reorder.to)
                    }
                }
            ),
        toggleModifier = Modifier
            .tutorialAnchor(screenState, TutorialAnchor.ActiveItemToggle(position)),
        animated = screenState.animationsEnabled
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
private fun HeadLine(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(LocalPagePitch.current),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = name,
            modifier = Modifier
                .padding(start = PaperDimens.gutter + PaperDimens.iconButton)
                .seatOnRule(MaterialTheme.typography.titleLarge),
            style = MaterialTheme.typography.titleLarge,
            color = LocalPaperPalette.current.ink
        )
    }
}

@Composable
private fun BackGlyph(onBack: () -> Unit) {
    InkIconButton(
        painter = painterResource(R.drawable.ic_arrow_back),
        contentDescription = stringResource(R.string.navigate_back),
        onClick = onBack,
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        ),
        tint = LocalPaperPalette.current.ink,
        seat = IconSeat.OnRule
    )
}
