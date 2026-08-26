package fr.mandarine.todolist.ui.todolists

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.ui.nav.PageStage
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val TYPE_CHAR_MILLIS = 80L

/**
 * The page of lists as the demo's hand finds it. Every beat the script plays on
 * this screen is one method here, driving the same state a finger would.
 */
class ListsStage(
    private val viewModel: TodoListsViewModel,
    val screenState: TodoListsScreenState,
    private val aim: (TutorialAnchor, TutorialBounds?) -> TutorialBounds?,
    private val writeDemoList: suspend (String, LocalDate?, LocalDate?) -> Unit,
    private val onOpen: (TodoList) -> Unit
) : PageStage {

    private var demoListId: String? = null
    private var listsBeforeDemo: Set<String> = emptySet()

    override val screen: TutorialScreen = TutorialScreen.LISTS

    override val anchors: TutorialAnchorHost get() = screenState

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? =
        aim(anchor, screenState.boundsOf(anchor))

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

    /**
     * On a page that already holds lists the demo's own list is not simply the
     * first row, so the id is found by waiting for a row that was not on the page
     * before the demo started writing. Taking the first row instead hands the
     * cleanup the reader's list and deletes it when the demo is abandoned.
     */
    override suspend fun awaitDemoListId(): String? {
        val written = listsBeforeDemo
        val content = viewModel.state.first { s ->
            s is TodoListsState.Content && s.activeSummaries.any { it.list.id !in written }
        } as TodoListsState.Content
        return content.activeSummaries.firstOrNull { it.list.id !in written }?.list?.id
    }

    override fun bannerContent(): TutorialBannerContent? {
        val summary = firstActiveSummary() ?: return null
        return TutorialBannerContent(summary.list.name, summary.list.dueDate)
    }

    private fun openCreateRow(): Boolean {
        if (screenState.addRowExpanded) return false
        listsBeforeDemo = listIdsOnPage()
        screenState.openAddRow()
        return true
    }

    private suspend fun typeListName(text: String): Boolean {
        if (!screenState.addRowExpanded) return false
        for (character in text) {
            delay(TYPE_CHAR_MILLIS)
            screenState.addRowText += character
        }
        return true
    }

    private fun openDueDatePicker(): Boolean {
        if (!screenState.addRowExpanded) return false
        screenState.datePickerRequest = DatePickerRequest(
            target = DateTarget.AddRow,
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

    /**
     * The reader's own tap makes room for a new list by pushing every row down a
     * rule; a demonstration may not. So the demo's list is written through a page
     * that lays it above the others without renumbering them, and it is claimed as
     * the demo's before it exists — an abort a frame later still knows what to
     * tear off.
     */
    private suspend fun submitList(): Boolean {
        val name = screenState.addRowText
        if (name.isBlank()) return false
        val selection = screenState.addRowSelection
        screenState.abandonAddRow()
        if (screenState.animationsEnabled) screenState.noteListAdded()
        writeDemoList(name, selection.targetDate, selection.dueDate)
        return true
    }

    private fun openFirstList(): Boolean {
        val summary = firstSummary() ?: return false
        demoListId = summary.list.id
        onOpen(summary.list)
        return true
    }

    /**
     * The demo tears the row off exactly the way a swipe does, so the beat that
     * used to arm a confirm strip now starts the pending deletion and the beat
     * that used to confirm it writes that deletion through early instead of
     * waiting for the undo slip to settle.
     */
    private fun armDeleteOnFirstList(): Boolean {
        val listId = demoListId ?: firstSummary()?.list?.id ?: return false
        screenState.deletion.request(listId)?.let { viewModel.deleteList(it) }
        return true
    }

    private fun confirmDeleteOnFirstList(): Boolean {
        val listId = screenState.deletion.commit() ?: return false
        viewModel.deleteList(listId)
        demoListId = null
        return true
    }

    private fun listIdsOnPage(): Set<String> {
        val content = viewModel.state.value as? TodoListsState.Content ?: return emptySet()
        return (content.activeSummaries + content.doneSummaries)
            .mapTo(mutableSetOf()) { it.list.id }
    }

    /**
     * By the last scene the demo list is finished and has moved below the
     * divider, so "the first list" has to mean the first row on the page rather
     * than the first unfinished one.
     */
    internal fun firstSummary(): TodoListSummary? {
        val content = viewModel.state.value as? TodoListsState.Content ?: return null
        return content.activeSummaries.firstOrNull() ?: content.doneSummaries.firstOrNull()
    }

    private fun firstActiveSummary(): TodoListSummary? =
        (viewModel.state.value as? TodoListsState.Content)?.activeSummaries?.firstOrNull()
}
