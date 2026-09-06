package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TodoItem

/**
 * Everything the page of one list can be. See [TodoListsState] for why this is a
 * sealed class.
 *
 * [NotFound] is the state that matters: the items page lives on an entry of the
 * back stack and holds only a list *id*, so the list can be deleted from underneath
 * it. When that happens the screen pops itself — see the `LaunchedEffect` on `state`
 * in `ui/nav/PageStack.kt`.
 *
 * The completed items arrive already sorted most-recently-finished first; the
 * active ones keep their stored `position` order.
 */
sealed class TodoListState {
    /** The list this page was opened on no longer exists. The page leaves. */
    data object NotFound : TodoListState()

    /** The list exists but has nothing written on it yet. */
    data object Empty : TodoListState()

    data class Content(
        val activeItems: List<TodoItem>,
        val completedItems: List<TodoItem>
    ) : TodoListState()
}
