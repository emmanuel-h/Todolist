package fr.mandarine.todolist.ui.nav

import androidx.navigation3.runtime.NavKey

data object ListsRoute : NavKey

data class ItemsRoute(val listId: String) : NavKey
