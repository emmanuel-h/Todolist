package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.ListHorizon
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
 *
 * [Content.activeSummaries] arrive in the **reader's own order** — the position
 * order the repository returns them in — so the board preserves whatever the reader
 * has dragged into place. [Content.agenda] is that same set of dated unfinished
 * lists re-sorted by horizon then day for the Calendar tab. [Content.sections] is
 * derived from [Content.agenda] (for dated horizons) and [Content.activeSummaries]
 * (for the undated horizon), so the UI can head each section without regrouping.
 */
sealed class TodoListsState {
    /** No lists at all. Draws the invitation to write the first one. */
    data object Empty : TodoListsState()

    data class Content(
        val activeSummaries: List<TodoListSummary>,
        val doneSummaries: List<TodoListSummary>
    ) : TodoListsState() {

        /**
         * Dated unfinished lists sorted by horizon, then by day (`targetDate ?: dueDate`),
         * stable so same-day lists keep the reader's own order. Undated lists are excluded.
         */
        val agenda: List<TodoListSummary> = activeSummaries
            .filter { it.horizon != ListHorizon.UNDATED }
            .sortedWith(compareBy({ it.horizon }, { it.list.targetDate ?: it.list.dueDate }))

        /**
         * The subset of [agenda] whose horizon is [ListHorizon.OVERDUE],
         * [ListHorizon.TODAY], or [ListHorizon.THIS_WEEK] — the lists that are
         * relevant this week.
         */
        val thisWeek: List<TodoListSummary> = agenda.filter {
            it.horizon == ListHorizon.OVERDUE ||
                it.horizon == ListHorizon.TODAY ||
                it.horizon == ListHorizon.THIS_WEEK
        }

        /** How many unfinished lists carry no date. */
        val undatedCount: Int = activeSummaries.filter { it.horizon == ListHorizon.UNDATED }.size

        /**
         * Active lists cut at each change of horizon, with dated horizons drawn
         * from [agenda] (in date order) and the undated horizon drawn from
         * [activeSummaries] (in reader order). The UI draws one heading per section.
         */
        val sections: List<ListSection> = ListHorizon.entries.mapNotNull { horizon ->
            val items = if (horizon == ListHorizon.UNDATED) {
                activeSummaries.filter { it.horizon == ListHorizon.UNDATED }
            } else {
                agenda.filter { it.horizon == horizon }
            }
            items.takeIf { it.isNotEmpty() }?.let { ListSection(horizon, it) }
        }
    }
}

/** One heading's worth of the page of lists: every unfinished list on one horizon. */
data class ListSection(
    val horizon: ListHorizon,
    val summaries: List<TodoListSummary>
)
