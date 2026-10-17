package fr.mandarine.todolist.ui.nav

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListsState
import org.junit.Assert.assertEquals
import org.junit.Test

class PageBindingTest {

    private fun summary(id: String, allDone: Boolean = false) =
        TodoListSummary(TodoList(id, id), allDone = allDone)

    @Test
    fun `should bind the lists in the order the page of lists shows them, finished last`() {
        val state = TodoListsState.Content(
            activeSummaries = listOf(summary("a"), summary("b")),
            doneSummaries = listOf(summary("c", allDone = true))
        )

        assertEquals(listOf("a", "b", "c"), pageOrder(state))
    }

    @Test
    fun `should bind nothing while the page of lists is empty`() {
        assertEquals(emptyList<String>(), pageOrder(TodoListsState.Empty))
    }

    @Test
    fun `should bind only the opened list before the page of lists has been read`() {
        assertEquals(listOf("b"), rebind(emptyList(), emptyList(), "b"))
    }

    @Test
    fun `should bind every list once the page of lists arrives`() {
        assertEquals(listOf("a", "b", "c"), rebind(listOf("b"), listOf("a", "b", "c"), "b"))
    }

    @Test
    fun `should bind only the opened list when the page of lists does not hold it`() {
        assertEquals(listOf("z"), rebind(emptyList(), listOf("a", "b"), "z"))
    }

    @Test
    fun `should keep the binding when a list moves on the page of lists`() {
        assertEquals(listOf("a", "b", "c"), rebind(listOf("a", "b", "c"), listOf("b", "c", "a"), "a"))
    }

    @Test
    fun `should take out a list that is gone and bind a new one at the back`() {
        assertEquals(listOf("a", "c", "d"), rebind(listOf("a", "b", "c"), listOf("d", "a", "c"), "a"))
    }

    @Test
    fun `should keep the binding when the page of lists is briefly empty`() {
        assertEquals(listOf("a", "b"), rebind(listOf("a", "b"), emptyList(), "a"))
    }
}
