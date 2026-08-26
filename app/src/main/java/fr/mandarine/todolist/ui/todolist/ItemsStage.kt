package fr.mandarine.todolist.ui.todolist

import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.ui.nav.PageStage
import fr.mandarine.todolist.ui.reorder.moved
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost
import kotlinx.coroutines.delay

private const val TYPE_CHAR_MILLIS = 80L

/**
 * The items of one list as the demo's hand finds them. The page it drives is the
 * one on top of the back stack, so it lives and dies with that page rather than
 * with the window.
 */
class ItemsStage(
    val viewModel: TodoListViewModel,
    val screenState: TodoListScreenState,
    private val aim: (TutorialAnchor, TutorialBounds?) -> TutorialBounds?,
    private val onLeave: () -> Unit
) : PageStage {

    override val screen: TutorialScreen = TutorialScreen.ITEMS

    override val anchors: TutorialAnchorHost get() = screenState

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? =
        aim(anchor, screenState.boundsOf(anchor))

    override suspend fun perform(action: TutorialAction): Boolean = when (action) {
        TutorialAction.OpenItemAddRow -> openAddRow()
        is TutorialAction.TypeItemTitle -> typeItemTitle(action.text)
        TutorialAction.SubmitItem -> submitItem()
        is TutorialAction.ToggleActiveItem -> toggle(activeItems(), action.index)
        is TutorialAction.ToggleCompletedItem -> toggle(completedItems(), action.index)
        is TutorialAction.MoveActiveItem -> previewMove(action.from, action.to)
        is TutorialAction.CommitReorder -> commitReorder(action.from, action.to)
        TutorialAction.NavigateBack -> {
            onLeave()
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
        for (character in text) {
            delay(TYPE_CHAR_MILLIS)
            screenState.addRowText += character
        }
        screenState.requestHideKeyboard()
        return true
    }

    private fun submitItem(): Boolean {
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
        val staged = screenState.previewOrder
        val ordered = if (staged != null) {
            staged
        } else {
            val ids = activeItems().map { it.id }
            if (from !in ids.indices || to !in ids.indices) return false
            ids.moved(from, to)
        }
        screenState.previewOrder = null
        viewModel.reorderTodos(ordered)
        return true
    }

    private fun activeItems(): List<TodoItem> =
        (viewModel.state.value as? TodoListState.Content)?.activeItems.orEmpty()

    private fun completedItems(): List<TodoItem> =
        (viewModel.state.value as? TodoListState.Content)?.completedItems.orEmpty()
}
