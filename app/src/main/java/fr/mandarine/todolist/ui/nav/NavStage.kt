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

    /**
     * Laying a sheet over the page of lists, once. The arriving page fades in from
     * an eighth of the window down, and alpha does not stop a tap reaching what is
     * under it, so the row that opened the list is still pressable for those first
     * frames — a double tap used to push two sheets and the first back press then
     * appeared to do nothing.
     */
    fun open(list: TodoList) {
        if (onItems) return
        backStack.add(ItemsRoute(list.id))
    }

    fun leave() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
