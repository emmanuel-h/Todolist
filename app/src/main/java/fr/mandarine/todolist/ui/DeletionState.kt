package fr.mandarine.todolist.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

const val UNDO_SLIP_MILLIS = 9_000L

data class PendingDeletion(val id: String, val torn: Boolean)

/**
 * A delete lives here between the tear and the repository. The row is only
 * dropped from the page once it has finished tearing off, and it stays dropped
 * after the commit until the repository confirms it is gone — otherwise the row
 * flickers back for the frames the write takes.
 */
class DeletionState {

    var pending by mutableStateOf<PendingDeletion?>(null)
        private set

    private var committed by mutableStateOf(emptySet<String>())

    fun tearing(id: String): Boolean = pending?.let { it.id == id && !it.torn } == true

    fun hides(id: String): Boolean =
        id in committed || pending?.let { it.id == id && it.torn } == true

    /**
     * Starting a second delete finishes the first one: its slip is gone from the
     * screen, so its id is handed back to be written through immediately.
     *
     * Asking twice for the same row is not a second delete. Answering it as one
     * wrote the row through while its own slip was still on the paper, leaving an
     * undo that could not undo anything.
     */
    fun request(id: String): String? {
        val previous = pending?.id
        if (previous == id) return null
        pending = PendingDeletion(id, torn = false)
        if (previous != null) committed = committed + previous
        return previous
    }

    fun markTorn() {
        pending = pending?.copy(torn = true)
    }

    fun commit(): String? {
        val id = pending?.id ?: return null
        committed = committed + id
        pending = null
        return id
    }

    fun undo(): String? {
        val id = pending?.id ?: return null
        pending = null
        return id
    }

    fun forget(liveIds: Set<String>) {
        if (committed.isEmpty()) return
        val remaining = committed.filterTo(mutableSetOf()) { it in liveIds }
        if (remaining.size != committed.size) committed = remaining
    }
}
