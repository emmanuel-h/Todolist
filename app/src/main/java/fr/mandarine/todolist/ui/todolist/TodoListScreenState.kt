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
private const val TEARING = "tearing-item"
private const val CONFIRM_ID = "confirm-id"
private const val CONFIRM_NAME = "confirm-name"
private const val CONFIRM_CASCADE = "confirm-cascade"
private const val TICKING = "ticking"
private const val UNTICKING = "unticking"
private const val ISSUED = "issued-toggles"

@Suppress("UNCHECKED_CAST")
private fun savedIds(saved: Any?): List<String> = (saved as? List<String>).orEmpty()

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
    /**
     * The rows whose ink the page is drawing ahead of the store, and what it is
     * drawing on each of them.
     *
     * Held until the store agrees. Dropping the row the moment the write is *issued*
     * left a frame in which the ring read the stored value again — the tick erased
     * itself, buzzed for the erasing, and then drew itself back when the write
     * landed. What the reader felt was two buzzes half a second apart on one tap.
     */
    var pendingToggles by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set

    /**
     * The rows whose write has already gone to the store. A restored page re-arms
     * the effect that writes, and a toggle written twice flips back — so a row that
     * has already been written keeps its ink and waits, rather than writing again.
     */
    var issuedToggles by mutableStateOf<Set<String>>(emptySet())
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

    fun inked(item: TodoItem): Boolean = pendingToggles[item.id] ?: item.isCompleted

    fun startToggle(id: String, drawing: Boolean) {
        pendingToggles = pendingToggles + (id to drawing)
    }

    fun markToggleIssued(id: String) {
        issuedToggles = issuedToggles + id
    }

    fun finishToggle(id: String) {
        pendingToggles = pendingToggles - id
        issuedToggles = issuedToggles - id
    }

    /**
     * Lets go of every row the store has caught up with. The counterpart of
     * [releaseOrder], and the same idea: the page keeps drawing ahead of storage
     * only until storage says the same thing.
     */
    fun releaseToggles(published: List<TodoItem>) {
        if (pendingToggles.isEmpty()) return
        val settled = published.filter { pendingToggles[it.id] == it.isCompleted }
        if (settled.isNotEmpty()) {
            val ids = settled.map { it.id }.toSet()
            pendingToggles = pendingToggles - ids
            issuedToggles = issuedToggles - ids
        }
    }

    fun requestHideKeyboard() {
        hideKeyboardSignal += 1
    }

    companion object {
        /**
         * What the reader was in the middle of, and what they have already answered
         * for. A staged drag order belongs to a gesture the rotation ended; a
         * half-typed item does not, and neither does a delete already confirmed on
         * the slip or a tick already drawn — both of those used to be dropped, so a
         * confirmed delete simply did not happen and the row came back.
         */
        val Saver: Saver<TodoListScreenState, Any> = mapSaver(
            save = { state ->
                mapOf(
                    ADD_OPEN to state.addRowExpanded,
                    ADD_TEXT to state.addRowText,
                    EDITING to state.editingItemId,
                    RENAMING to state.renamingList,
                    SHEET_KIND to state.dateSheet?.kind?.name,
                    SHEET_DAY to state.dateSheet?.date?.toEpochDay(),
                    TEARING to state.tearingId,
                    CONFIRM_ID to state.confirmDelete?.id,
                    CONFIRM_NAME to state.confirmDelete?.name,
                    CONFIRM_CASCADE to state.confirmDelete?.cascadeCount,
                    TICKING to ArrayList(state.pendingToggles.filterValues { it }.keys),
                    UNTICKING to ArrayList(state.pendingToggles.filterValues { !it }.keys),
                    ISSUED to ArrayList(state.issuedToggles)
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
                    tearingId = saved[TEARING] as String?
                    (saved[CONFIRM_ID] as String?)?.let { id ->
                        confirmDelete = ConfirmDeleteRequest(
                            id = id,
                            name = saved[CONFIRM_NAME] as String? ?: "",
                            cascadeCount = saved[CONFIRM_CASCADE] as Int?
                        )
                    }
                    pendingToggles = savedIds(saved[TICKING]).associateWith { true } +
                        savedIds(saved[UNTICKING]).associateWith { false }
                    issuedToggles = savedIds(saved[ISSUED]).toSet()
                }
            }
        )
    }
}
