package fr.mandarine.todolist.domain

/**
 * How many items one list holds, split by whether they are done.
 *
 * This is the result of a single grouped `COUNT` query rather than of loading
 * every item: the page of lists needs a tally per row, and reading all items of
 * all lists to produce it would make the first frame cost the whole database.
 * See [TodoRepository.countsByList].
 *
 * A list with no items at all is simply absent from the result, so callers treat
 * a missing entry as zero-and-zero rather than expecting a row of zeroes.
 */
data class TodoCounts(
    val listId: String,
    val activeCount: Int,
    val completedCount: Int
)
