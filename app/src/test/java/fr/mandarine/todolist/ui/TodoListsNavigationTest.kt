package fr.mandarine.todolist.ui

import android.content.Intent
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.MainThreadDatabaseRule
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.data.SharedPreferencesTutorialStateRepository
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.nav.ItemsRoute
import fr.mandarine.todolist.ui.nav.ListsRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The page of items is a sheet laid over the page of lists rather than a window
 * of its own, and the tutorial watches the same back stack in order to tear its
 * own page off. These pin the boundary between the two: a reader turning a page
 * must never be mistaken for a demo ending.
 *
 * Every assertion is made after the main looper has drained, because the beat is
 * delivered by a collector and not by the tap that caused it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsNavigationTest {

    @get:Rule
    val databaseRule = MainThreadDatabaseRule()

    @Before
    fun stillPage() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<TodoListApplication>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f
        )
    }

    @Before
    fun markTutorialSeen() {
        val app = ApplicationProvider.getApplicationContext<TodoListApplication>()
        SharedPreferencesTutorialStateRepository(app).markTutorialSeen()
    }

    @Test
    fun `should keep the opened page on the stack once the beat has been delivered`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.viewModel.createList("Groceries")
                assertTrue(open(activity))
            }

            idle()

            scenario.onActivity { activity ->
                assertEquals(
                    listOf(ListsRoute, ItemsRoute(requireNotNull(activity.firstListId()))),
                    activity.backStack.toList()
                )
            }
        }
    }

    @Test
    fun `should keep the opened page when the window stops and starts again`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.viewModel.createList("Groceries")
                open(activity)
            }
            idle()

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            idle()

            scenario.onActivity { activity ->
                assertEquals(
                    listOf(ListsRoute, ItemsRoute(requireNotNull(activity.firstListId()))),
                    activity.backStack.toList()
                )
            }
        }
    }

    @Test
    fun `should land on the page a notification names and leave it there`() {
        val app = ApplicationProvider.getApplicationContext<TodoListApplication>()
        val listId = runBlocking {
            val repository = app.container.todoListRepository
            repository.add(
                fr.mandarine.todolist.domain.TodoList(id = "notified-list", name = "Groceries")
            )
            "notified-list"
        }

        val intent = Intent(app, TodoListsActivity::class.java).putExtra("LIST_ID", listId)

        ActivityScenario.launch<TodoListsActivity>(intent).use { scenario ->
            idle()
            scenario.onActivity { activity ->
                assertEquals(
                    listOf(ListsRoute, ItemsRoute(listId)),
                    activity.backStack.toList()
                )
            }
        }
    }

    @Test
    fun `should leave the page of lists alone when nothing is open`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.viewModel.createList("Groceries") }
            idle()

            scenario.onActivity { activity ->
                assertEquals(listOf(ListsRoute), activity.backStack.toList())
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun open(activity: TodoListsActivity): Boolean =
        runBlocking { activity.stage.perform(TutorialAction.OpenFirstList) }

    private fun TodoListsActivity.firstListId(): String? =
        (viewModel.state.value as? TodoListsState.Content)?.activeSummaries?.firstOrNull()?.list?.id
}
