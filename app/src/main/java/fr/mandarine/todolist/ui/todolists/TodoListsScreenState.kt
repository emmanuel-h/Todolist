package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost
import fr.mandarine.todolist.ui.tutorial.TutorialAnchors
import java.time.LocalDate

enum class DateTarget { ADD_ROW, RENAME }

data class DatePickerRequest(
    val target: DateTarget,
    val kind: DateKind,
    val initial: LocalDate?
)

data class RenameState(
    val listId: String,
    val name: String,
    val selection: DateSelection
) {
    companion object {
        fun of(list: TodoList): RenameState = RenameState(
            listId = list.id,
            name = list.name,
            selection = DateSelection.of(list.targetDate, list.dueDate)
        )
    }
}

class TodoListsScreenState : TutorialAnchorHost by TutorialAnchors() {

    var addRowExpanded by mutableStateOf(false)

    var addRowText by mutableStateOf("")

    var addRowSelection by mutableStateOf(DateSelection.None)

    var datePickerRequest by mutableStateOf<DatePickerRequest?>(null)

    var confirmingDeleteListId by mutableStateOf<String?>(null)

    var rename by mutableStateOf<RenameState?>(null)

    var previewOrder by mutableStateOf<List<String>?>(null)

    var animationsEnabled by mutableStateOf(true)

    var hideKeyboardSignal by mutableStateOf(0)
        private set

    private var knownListIds: Set<String> = emptySet()
    private var pendingDropIn = false

    fun requestHideKeyboard() {
        hideKeyboardSignal += 1
    }

    fun openAddRow() {
        addRowExpanded = true
        confirmingDeleteListId = null
    }

    fun closeAddRow() {
        addRowExpanded = false
        addRowText = ""
        addRowSelection = DateSelection.None
    }

    fun noteListAdded() {
        pendingDropIn = true
    }

    /**
     * The row that drops in is the one that appeared on the same publish as the
     * create event. Rows arriving without a pending create — a first read, a
     * rename, a reorder — are already on the page and must not fall onto it.
     */
    fun dropInFor(ids: List<String>): String? {
        val fresh = ids.firstOrNull { it !in knownListIds }
        knownListIds = ids.toSet()
        if (!pendingDropIn || fresh == null) return null
        pendingDropIn = false
        return fresh
    }
}
