package fr.mandarine.todolist.ui.todolists

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.ui.ConfirmDeleteRequest
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
    val selection: DateSelection,
    val colour: ListColour = ListColour.None
) {
    companion object {
        fun of(list: TodoList): RenameState = RenameState(
            listId = list.id,
            name = list.name,
            selection = DateSelection.of(list.targetDate, list.dueDate),
            colour = list.colour
        )
    }
}

class TodoListsScreenState {

    var settingsOpen by mutableStateOf(false)

    var confirmDelete by mutableStateOf<ConfirmDeleteRequest?>(null)

    var tearingId by mutableStateOf<String?>(null)

    var addRowExpanded by mutableStateOf(false)

    var addRowText by mutableStateOf("")

    var addRowSelection by mutableStateOf(DateSelection.None)

    var datePickerRequest by mutableStateOf<DatePickerRequest?>(null)

    var rename by mutableStateOf<RenameState?>(null)

    var previewOrder by mutableStateOf<List<String>?>(null)

    /**
     * The order the reader left the rows in outlives the drag that made it. The
     * repository is written to on another dispatcher, so between the drop and the
     * read that answers it the page would be handed the old order once more and
     * would glide every row back before gliding it forward again — which is what
     * read as the dropped row arriving from somewhere else entirely. The staged
     * order is held until the page is handed exactly it, and let go of then.
     */
    fun stageOrder(order: List<String>) {
        previewOrder = order
    }

    fun releaseOrder(published: List<String>) {
        if (previewOrder == published) previewOrder = null
    }

    var animationsEnabled by mutableStateOf(true)

    private var knownListIds: Set<String> = emptySet()
    private var pendingDropIn = false

    fun openAddRow() {
        addRowExpanded = true
    }

    /**
     * Putting the pen down discards the line — tapping away is how the reader
     * throws it away, and finding the words again later was the surprise. The pad
     * always opens on blank paper. Only a commit keeps what was written.
     */
    fun closeAddRow() {
        addRowExpanded = false
        clearAddRow()
    }

    fun abandonAddRow() {
        closeAddRow()
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
     * What is kept is what the reader was in the middle of, and what they have
     * already answered for. A staged drag order belongs to a gesture the rotation
     * ended; a half-typed list name and the day circled next to it do not, and
     * neither does a delete already confirmed on the slip — that one used to be
     * dropped, so the delete simply did not happen and the row came back.
     */
    fun saveTo(outState: Bundle) {
        outState.putBoolean(ADD_OPEN, addRowExpanded)
        outState.putString(ADD_TEXT, addRowText)
        outState.putString(ADD_KIND, addRowSelection.kind.name)
        addRowSelection.date?.let { outState.putLong(ADD_DAY, it.toEpochDay()) }
        outState.putBoolean(SETTINGS_OPEN, settingsOpen)
        tearingId?.let { outState.putString(TEARING, it) }
        confirmDelete?.let { confirm ->
            outState.putString(CONFIRM_ID, confirm.id)
            outState.putString(CONFIRM_NAME, confirm.name)
            confirm.cascadeCount?.let { outState.putInt(CONFIRM_CASCADE, it) }
        }
        datePickerRequest?.let { request ->
            outState.putString(PICKER_TARGET, request.target.saved())
            outState.putString(PICKER_KIND, request.kind.name)
            request.initial?.let { outState.putLong(PICKER_DAY, it.toEpochDay()) }
        }
        rename?.let { open ->
            outState.putString(RENAME_ID, open.listId)
            outState.putString(RENAME_NAME, open.name)
            outState.putString(RENAME_KIND, open.selection.kind.name)
            open.selection.date?.let { outState.putLong(RENAME_DAY, it.toEpochDay()) }
            outState.putString(RENAME_COLOUR, open.colour.name)
        }
    }

    fun restoreFrom(savedInstanceState: Bundle) {
        addRowExpanded = savedInstanceState.getBoolean(ADD_OPEN)
        addRowText = savedInstanceState.getString(ADD_TEXT).orEmpty()
        addRowSelection = DateSelection(
            savedInstanceState.getString(ADD_KIND)?.let(DateKind::valueOf) ?: DateKind.TARGET,
            savedInstanceState.dayOrNull(ADD_DAY)
        )
        settingsOpen = savedInstanceState.getBoolean(SETTINGS_OPEN)
        tearingId = savedInstanceState.getString(TEARING)
        savedInstanceState.getString(CONFIRM_ID)?.let { id ->
            confirmDelete = ConfirmDeleteRequest(
                id = id,
                name = savedInstanceState.getString(CONFIRM_NAME).orEmpty(),
                cascadeCount = if (savedInstanceState.containsKey(CONFIRM_CASCADE)) {
                    savedInstanceState.getInt(CONFIRM_CASCADE)
                } else {
                    null
                }
            )
        }
        savedInstanceState.getString(PICKER_TARGET)?.let { target ->
            datePickerRequest = DatePickerRequest(
                target = restoredTarget(target),
                kind = savedInstanceState.getString(PICKER_KIND)?.let(DateKind::valueOf)
                    ?: DateKind.TARGET,
                initial = savedInstanceState.dayOrNull(PICKER_DAY)
            )
        }
        val renamedId = savedInstanceState.getString(RENAME_ID) ?: return
        rename = RenameState(
            listId = renamedId,
            name = savedInstanceState.getString(RENAME_NAME).orEmpty(),
            selection = DateSelection(
                savedInstanceState.getString(RENAME_KIND)?.let(DateKind::valueOf)
                    ?: DateKind.TARGET,
                savedInstanceState.dayOrNull(RENAME_DAY)
            ),
            colour = savedInstanceState.getString(RENAME_COLOUR)
                ?.let(ListColour::valueOf) ?: ListColour.None
        )
    }
}

private fun Bundle.dayOrNull(key: String): LocalDate? =
    if (containsKey(key)) LocalDate.ofEpochDay(getLong(key)) else null

/**
 * A [DateTarget] is a sealed interface, and only one of its shapes carries
 * anything: the row names the list it is about.
 */
private fun DateTarget.saved(): String = when (this) {
    DateTarget.AddRow -> TARGET_ADD_ROW
    DateTarget.Rename -> TARGET_RENAME
    is DateTarget.Row -> TARGET_ROW + listId
}

private fun restoredTarget(saved: String): DateTarget = when {
    saved == TARGET_ADD_ROW -> DateTarget.AddRow
    saved == TARGET_RENAME -> DateTarget.Rename
    else -> DateTarget.Row(saved.removePrefix(TARGET_ROW))
}

private const val TARGET_ADD_ROW = "add-row"
private const val TARGET_RENAME = "rename"
private const val TARGET_ROW = "row:"
private const val SETTINGS_OPEN = "lists-settings-open"
private const val TEARING = "lists-tearing"
private const val CONFIRM_ID = "lists-confirm-id"
private const val CONFIRM_NAME = "lists-confirm-name"
private const val CONFIRM_CASCADE = "lists-confirm-cascade"
private const val PICKER_TARGET = "lists-picker-target"
private const val PICKER_KIND = "lists-picker-kind"
private const val PICKER_DAY = "lists-picker-day"
private const val ADD_OPEN = "lists-add-open"
private const val ADD_TEXT = "lists-add-text"
private const val ADD_KIND = "lists-add-kind"
private const val ADD_DAY = "lists-add-day"
private const val RENAME_ID = "lists-rename-id"
private const val RENAME_NAME = "lists-rename-name"
private const val RENAME_KIND = "lists-rename-kind"
private const val RENAME_DAY = "lists-rename-day"
private const val RENAME_COLOUR = "lists-rename-colour"

