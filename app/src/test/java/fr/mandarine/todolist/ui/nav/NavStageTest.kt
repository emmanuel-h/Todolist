package fr.mandarine.todolist.ui.nav

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import fr.mandarine.todolist.domain.TodoList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavStageTest {

    private val backStack = NavBackStack<NavKey>(ListsRoute)
    private val stage = NavStage(backStack)

    @Test
    fun `should stand on the page of lists while nothing is laid over it`() {
        assertFalse(stage.onItems)
    }

    @Test
    fun `should stand on the page of items once a list is opened`() {
        stage.open(TodoList("list-1", "Groceries"))

        assertTrue(stage.onItems)
        assertEquals(listOf(ListsRoute, ItemsRoute("list-1")), backStack.toList())
    }

    @Test
    fun `should never peel the last page off the pad`() {
        stage.leave()

        assertEquals(listOf(ListsRoute), backStack.toList())
    }

    @Test
    fun `should peel only the page on top off the pad`() {
        stage.open(TodoList("list-1", "Groceries"))

        stage.leave()

        assertEquals(listOf(ListsRoute), backStack.toList())
        assertFalse(stage.onItems)
    }
}
