package fr.mandarine.todolist.ui.todolist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.ui.ConfirmDeleteRequest
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.DateSelection
import java.time.LocalDate

private const val ADD_OPEN = "add-open"
private const val ADD_TEXT = "add-text"
private const val EDITING = "editing-item"
private const val RENAMING = "renaming-list"
private const val SHEET_KIND = "sheet-kind"
private const val SHEET_DAY = "sheet-day"

/**
 * Everything the page of one list is *doing* that is not worth storing.
 *
 * This is the counterpart to `TodoListViewModel`: the ViewModel holds what is in
 * the database, and this holds what the reader's hands are in the middle of — a
 * half-typed line, which row is being torn off, which tick is still being drawn,
 * where the last finger went. None of it is a fact about the list, so none of it
 * belongs in the domain.
 *
 * ### `by mutableStateOf`
 * Every property here is a Compose *snapshot state*. `by` is Kotlin property
 * delegation: reading `addRowText` calls the delegate's getter, writing it calls the
 * setter. What Compose adds is that a read inside a composable is **recorded**, so
 * writing the property later re-runs exactly the composables that read it. That is
 * the whole recomposition mechanism — there is no `invalidate()` and no observer to
 * register.
 *
 * A property whose setter must not be public uses `private set` plus a method
 * ([startToggle], [requestHideKeyboard]) so the invariant stays in one place.
 *
 * ### Lifetime
 * Created by `rememberSaveable(listId, saver = Saver)` in `PageStack`, so it lives
 * as long as the page and survives a rotation. [Saver] decides what survives; see
 * its own note for why that is deliberately only the half-typed text.
 */
class TodoListScreenState {

    var confirmDelete by mutableStateOf<ConfirmDeleteRequest?>(null)

    var tearingId by mutableStateOf<String?>(null)

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

    /**
     * Where the reader last put a finger on the page, so a flourish can be thrown
     * from there rather than from the middle of the sheet. Read off the pointer
     * itself rather than off a row's bounds: by the time a finishing tick is
     * reported the row it was written on has already travelled into the completed
     * section, and the place worth celebrating from is where the hand was.
     */
    var lastTouch by mutableStateOf(Offset.Zero)

    /**
     * The item a finishing tick was written on, handed over by the view model and
     * taken back by the page once the flourish has run. Not saved: a tour of the
     * confetti is not something to restore on a rotation.
     */
    var finishedOn by mutableStateOf<String?>(null)

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
