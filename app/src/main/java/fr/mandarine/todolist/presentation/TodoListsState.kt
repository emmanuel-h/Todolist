package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TodoListSummary

/**
 * Everything the page of lists can be, as a closed set.
 *
 * A `sealed class` here rather than one class with nullable fields: the screen does
 * a `when` over this and the compiler proves every case is handled, so adding a
 * third state (a loading state, say) makes every screen that reads it fail to
 * compile until it says what to draw. There is no `Loading` today because the read
 * is synchronous off a background dispatcher and the splash screen covers it — see
 * `BLANK_PAGE_MILLIS` in `TodoListsActivity`.
 *
 * `data object` is a singleton with a sensible `toString`/`equals` — the right shape
 * for a state that carries no data. [Empty] is also the *initial* value, so the very
 * first frame draws the empty page rather than nothing.
 *
 * The split into [Content.activeSummaries] and [Content.doneSummaries] is done once
 * in the ViewModel, not in the composable, so the screen never filters a list while
 * drawing.
 */
sealed class TodoListsState {
    /** No lists at all. Draws the invitation to write the first one. */
    data object Empty : TodoListsState()

    data class Content(
        val activeSummaries: List<TodoListSummary>,
        val doneSummaries: List<TodoListSummary>
    ) : TodoListsState()
}
