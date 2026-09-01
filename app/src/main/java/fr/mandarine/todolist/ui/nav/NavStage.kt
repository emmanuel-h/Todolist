package fr.mandarine.todolist.ui.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import fr.mandarine.todolist.domain.TodoList

/**
 * One window means one stage. Which screen the reader is standing on is no longer
 * a question of which activity is resumed but of which page is on top of the back
 * stack.
 */
class NavStage(private val backStack: MutableList<NavKey>) {

    var animationsEnabled by mutableStateOf(true)

    val onItems: Boolean get() = backStack.lastOrNull() is ItemsRoute

    fun open(list: TodoList) {
        backStack.add(ItemsRoute(list.id))
    }

    fun leave() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
