package fr.mandarine.todolist.ui

import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.MainThreadDatabaseRule
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.nav.ItemsRoute
import fr.mandarine.todolist.ui.nav.ListsRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The lists are bound like the pages of a notebook: on a list's own page, a swipe
 * sideways turns to the next list without going back to the page of lists, and
 * back still lands on the page of lists rather than on the list the reader started
 * from.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListPagingTest {

    @get:Rule(order = 0)
    val databaseRule = MainThreadDatabaseRule()

    @get:Rule(order = 1)
    val stillPage = object : ExternalResource() {
        override fun before() {
            Settings.Global.putFloat(
                ApplicationProvider.getApplicationContext<TodoListApplication>().contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                0f
            )
        }
    }

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<TodoListsActivity>()

    private fun openFirstOf(vararg names: String): List<String> {
        names.forEach { name -> composeRule.runOnUiThread { composeRule.activity.viewModel.createList(name) } }
        composeRule.waitForIdle()
        val shown = (composeRule.activity.viewModel.state.value as TodoListsState.Content)
            .activeSummaries.map { it.list }
        composeRule.runOnUiThread { composeRule.activity.stage.open(shown.first()) }
        composeRule.waitForIdle()
        return shown.map { it.id }
    }

    @Test
    fun `should turn to the next list with a swipe from right to left`() {
        openFirstOf("Groceries", "Weekend")

        composeRule.onNodeWithContentDescription("List 1 of 2").assertExists()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("List 2 of 2").assertExists()
    }

    @Test
    fun `should turn back to the list before with a swipe from left to right`() {
        openFirstOf("Groceries", "Weekend")

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("List 1 of 2").assertExists()
    }

    @Test
    fun `should leave to the page of lists from whichever list was turned to`() {
        val ids = openFirstOf("Groceries", "Weekend")

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertEquals(listOf(ListsRoute, ItemsRoute(ids.first())), composeRule.activity.backStack.toList())

        composeRule.runOnUiThread { composeRule.activity.stage.leave() }
        composeRule.waitForIdle()

        assertEquals(listOf(ListsRoute), composeRule.activity.backStack.toList())
    }
}
