package fr.mandarine.todolist.ui.todolists

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.ui.DeletionState
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost
import fr.mandarine.todolist.ui.tutorial.TutorialAnchors
import java.time.LocalDate

/**
 * Whose date the open calendar is going to write. The line being written and the
 * edit surface both hold their answer in screen state until they are put down; a
 * row on the page has nothing to hold it in, so it names the list instead and the
 * pick is written straight through.
 */
sealed interface DateTarget {

    data object AddRow : DateTarget

    data object Rename : DateTarget

    data class Row(val listId: String) : DateTarget
}

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

    val deletion = DeletionState()

    var addRowExpanded by mutableStateOf(false)

    var addRowText by mutableStateOf("")

    var addRowSelection by mutableStateOf(DateSelection.None)

    var datePickerRequest by mutableStateOf<DatePickerRequest?>(null)

    var rename by mutableStateOf<RenameState?>(null)

    var previewOrder by mutableStateOf<List<String>?>(null)

    var animationsEnabled by mutableStateOf(true)

    private var knownListIds: Set<String> = emptySet()
    private var pendingDropIn = false

    fun openAddRow() {
        addRowExpanded = true
    }

    fun closeAddRow() {
        addRowExpanded = false
        clearAddRow()
    }

    fun clearAddRow() {
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

    /**
     * This state belongs to the window rather than to a composition, because the
     * demo's stage holds it before there is a composition to hold it in — so it
     * is saved and restored by hand alongside the window.
     *
     * What is kept is what the reader was in the middle of writing. A tear
     * mid-slip and a staged drag order belong to a gesture the rotation ended
     * anyway; a half-typed list name and the day circled next to it do not.
     */
    fun saveTo(outState: Bundle) {
        outState.putBoolean(ADD_OPEN, addRowExpanded)
        outState.putString(ADD_TEXT, addRowText)
        outState.putString(ADD_KIND, addRowSelection.kind.name)
        addRowSelection.date?.let { outState.putLong(ADD_DAY, it.toEpochDay()) }
        rename?.let { open ->
            outState.putString(RENAME_ID, open.listId)
            outState.putString(RENAME_NAME, open.name)
            outState.putString(RENAME_KIND, open.selection.kind.name)
            open.selection.date?.let { outState.putLong(RENAME_DAY, it.toEpochDay()) }
        }
    }

    fun restoreFrom(savedInstanceState: Bundle) {
        addRowExpanded = savedInstanceState.getBoolean(ADD_OPEN)
        addRowText = savedInstanceState.getString(ADD_TEXT).orEmpty()
        addRowSelection = DateSelection(
            savedInstanceState.getString(ADD_KIND)?.let(DateKind::valueOf) ?: DateKind.TARGET,
            savedInstanceState.dayOrNull(ADD_DAY)
        )
        val renamedId = savedInstanceState.getString(RENAME_ID) ?: return
        rename = RenameState(
            listId = renamedId,
            name = savedInstanceState.getString(RENAME_NAME).orEmpty(),
            selection = DateSelection(
                savedInstanceState.getString(RENAME_KIND)?.let(DateKind::valueOf)
                    ?: DateKind.TARGET,
                savedInstanceState.dayOrNull(RENAME_DAY)
            )
        )
    }
}

private fun Bundle.dayOrNull(key: String): LocalDate? =
    if (containsKey(key)) LocalDate.ofEpochDay(getLong(key)) else null

private const val ADD_OPEN = "lists-add-open"
private const val ADD_TEXT = "lists-add-text"
private const val ADD_KIND = "lists-add-kind"
private const val ADD_DAY = "lists-add-day"
private const val RENAME_ID = "lists-rename-id"
private const val RENAME_NAME = "lists-rename-name"
private const val RENAME_KIND = "lists-rename-kind"
private const val RENAME_DAY = "lists-rename-day"
