package fr.mandarine.todolist.ui.nav

import androidx.navigation3.runtime.NavKey

/**
 * The two places the app can be. These are the *keys* on the Navigation 3 back
 * stack in `TodoListsActivity`, and `PageStack`'s `entryProvider` maps each one to
 * the screen that draws it.
 *
 * They must be serialisable values, because the stack is saved and restored across
 * process death — which is why a route carries a list **id** rather than a
 * `TodoList`. [ItemsRoute] is a `data class` so two entries for different lists are
 * distinct, and [ListsRoute] is a `data object` because there is only ever one page
 * of lists.
 *
 * Adding a screen means adding a key here and an `entry<...>` block in `PageStack`.
 */
data object ListsRoute : NavKey

data class ItemsRoute(val listId: String) : NavKey
