package fr.mandarine.todolist.presentation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long the demonstration rests between beats, and the reader's right to say
 * they have seen enough.
 *
 * A hurried scene keeps every beat and drops every rest: the page is driven
 * through exactly the actions it would have been driven through, so it lands
 * where the unhurried scene would have left it and the next scene starts from
 * solid ground. The rest that is already being taken when the reader asks ends
 * there rather than running itself out.
 */
class TutorialPace {

    private var seenEnough = CompletableDeferred<Unit>()

    var hurrying: Boolean = false
        private set

    fun hurry() {
        hurrying = true
        seenEnough.complete(Unit)
    }

    fun settle() {
        hurrying = false
        seenEnough = CompletableDeferred()
    }

    suspend fun beat(millis: Long) {
        if (hurrying) return
        withTimeoutOrNull(millis) { seenEnough.await() }
    }
}
