package fr.mandarine.todolist.ui.todolist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.ui.DeletionState
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.DateSelection
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost
import fr.mandarine.todolist.ui.tutorial.TutorialAnchors
import java.time.LocalDate

private const val ADD_OPEN = "add-open"
private const val ADD_TEXT = "add-text"
private const val EDITING = "editing-item"
private const val RENAMING = "renaming-list"
private const val SHEET_KIND = "sheet-kind"
private const val SHEET_DAY = "sheet-day"

class TodoListScreenState : TutorialAnchorHost by TutorialAnchors() {

    val deletion = DeletionState()

    var addRowExpanded by mutableStateOf(false)

    var addRowText by mutableStateOf("")

    var editingItemId by mutableStateOf<String?>(null)

    var renamingList by mutableStateOf(false)

    var dateSheet by mutableStateOf<DateSelection?>(null)

    /**
     * Every row whose tick is still being drawn, not just the latest one. A
     * single slot meant a second tap inside the stroke replaced the first, and
     * the effect carrying it was cancelled mid-stroke — so ticking a list
     * quickly left most of it unticked.
     */
    var pendingToggles by mutableStateOf<Set<String>>(emptySet())
        private set

    var previewOrder by mutableStateOf<List<String>?>(null)

    var animationsEnabled by mutableStateOf(true)

    var hideKeyboardSignal by mutableStateOf(0)
        private set

    fun inked(item: TodoItem): Boolean = item.isCompleted != (item.id in pendingToggles)

    fun startToggle(id: String) {
        pendingToggles = pendingToggles + id
    }

    fun finishToggle(id: String) {
        pendingToggles = pendingToggles - id
    }

    fun requestHideKeyboard() {
        hideKeyboardSignal += 1
    }

    companion object {
        /**
         * What the reader was in the middle of writing, and nothing else. A tear
         * mid-slip, a staged drag order and a half-drawn tick all belong to a
         * gesture that the rotation ended anyway; a half-typed item does not.
         */
        val Saver: Saver<TodoListScreenState, Any> = mapSaver(
            save = { state ->
                mapOf(
                    ADD_OPEN to state.addRowExpanded,
                    ADD_TEXT to state.addRowText,
                    EDITING to state.editingItemId,
                    RENAMING to state.renamingList,
                    SHEET_KIND to state.dateSheet?.kind?.name,
                    SHEET_DAY to state.dateSheet?.date?.toEpochDay()
                )
            },
            restore = { saved ->
                TodoListScreenState().apply {
                    addRowExpanded = saved[ADD_OPEN] as Boolean
                    addRowText = saved[ADD_TEXT] as String
                    editingItemId = saved[EDITING] as String?
                    renamingList = saved[RENAMING] as Boolean
                    dateSheet = (saved[SHEET_KIND] as String?)?.let { kind ->
                        DateSelection(
                            DateKind.valueOf(kind),
                            (saved[SHEET_DAY] as Long?)?.let(LocalDate::ofEpochDay)
                        )
                    }
                }
            }
        )
    }
}
