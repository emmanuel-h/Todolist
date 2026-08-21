package fr.mandarine.todolist.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.MainThreadDatabaseRule
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.data.SharedPreferencesTutorialStateRepository
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TutorialBounds
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListTutorialStageTest {

    @get:Rule
    val databaseRule = MainThreadDatabaseRule()

    @Before
    fun markTutorialSeen() {
        val app = ApplicationProvider.getApplicationContext<TodoListApplication>()
        SharedPreferencesTutorialStateRepository(app).markTutorialSeen()
        app.container.todoListRepository.add(TodoList(LIST_ID, "Groceries"))
    }

    @Test
    fun `should stage the items screen`() {
        onActivity { activity ->
            assertEquals(TutorialScreen.ITEMS, activity.screen)
        }
    }

    @Test
    fun `should read anchor bounds from the screen state`() {
        onActivity { activity ->
            val bounds = TutorialBounds(left = 1, top = 2, width = 3, height = 4)
            activity.screenState.putBounds(TutorialAnchor.ItemGhostRow, bounds)

            assertEquals(bounds, activity.boundsOf(TutorialAnchor.ItemGhostRow))
        }
    }

    @Test
    fun `should open the add row`() {
        onActivity { activity ->
            assertTrue(perform(activity, TutorialAction.OpenItemAddRow))
            assertTrue(activity.screenState.addRowExpanded)
        }
    }

    @Test
    fun `should refuse to open an add row that is already open`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenItemAddRow)

            assertFalse(perform(activity, TutorialAction.OpenItemAddRow))
        }
    }

    @Test
    fun `should type the item title one character at a time`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenItemAddRow)

            assertTrue(perform(activity, TutorialAction.TypeItemTitle("Apples")))
            assertEquals("Apples", activity.screenState.addRowText)
        }
    }

    @Test
    fun `should put the keyboard away once the title is typed`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenItemAddRow)
            val before = activity.screenState.hideKeyboardSignal

            perform(activity, TutorialAction.TypeItemTitle("Apples"))

            assertEquals(before + 1, activity.screenState.hideKeyboardSignal)
        }
    }

    @Test
    fun `should refuse to type a title while the add row is closed`() {
        onActivity { activity ->
            assertFalse(perform(activity, TutorialAction.TypeItemTitle("Apples")))
        }
    }

    @Test
    fun `should add the item that was typed and clear the field`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenItemAddRow)
            perform(activity, TutorialAction.TypeItemTitle("Apples"))

            assertTrue(perform(activity, TutorialAction.SubmitItem))
            assertEquals(listOf("Apples"), activity.activeTitles())
            assertEquals("", activity.screenState.addRowText)
        }
    }

    @Test
    fun `should refuse to add an item with no title`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenItemAddRow)

            assertFalse(perform(activity, TutorialAction.SubmitItem))
            assertEquals(emptyList<String>(), activity.activeTitles())
        }
    }

    @Test
    fun `should refuse to add an item while the add row is closed`() {
        onActivity { activity ->
            assertFalse(perform(activity, TutorialAction.SubmitItem))
        }
    }

    @Test
    fun `should complete the active item it is pointed at`() {
        onActivity { activity ->
            seedItems(activity, "Apples", "Bread")

            assertTrue(perform(activity, TutorialAction.ToggleActiveItem(1)))
            assertEquals(listOf("Apples"), activity.activeTitles())
            assertEquals(listOf("Bread"), activity.completedTitles())
        }
    }

    @Test
    fun `should refuse to complete an active item that is not there`() {
        onActivity { activity ->
            seedItems(activity, "Apples")

            assertFalse(perform(activity, TutorialAction.ToggleActiveItem(1)))
        }
    }

    @Test
    fun `should restore the completed item it is pointed at`() {
        onActivity { activity ->
            seedItems(activity, "Apples", "Bread")
            perform(activity, TutorialAction.ToggleActiveItem(0))

            assertTrue(perform(activity, TutorialAction.ToggleCompletedItem(0)))
            assertEquals(emptyList<String>(), activity.completedTitles())
        }
    }

    @Test
    fun `should refuse to restore a completed item that is not there`() {
        onActivity { activity ->
            seedItems(activity, "Apples")

            assertFalse(perform(activity, TutorialAction.ToggleCompletedItem(0)))
        }
    }

    @Test
    fun `should stage a move without persisting it`() {
        onActivity { activity ->
            seedItems(activity, "Apples", "Bread")

            assertTrue(perform(activity, TutorialAction.MoveActiveItem(1, 0)))
            assertEquals(
                listOf(activity.activeIds()[1], activity.activeIds()[0]),
                activity.screenState.previewOrder
            )
            assertEquals(listOf("Apples", "Bread"), activity.activeTitles())
        }
    }

    @Test
    fun `should stage a second move on top of the first`() {
        onActivity { activity ->
            seedItems(activity, "Apples", "Bread", "Cheese")
            val ids = activity.activeIds()
            perform(activity, TutorialAction.MoveActiveItem(2, 0))

            perform(activity, TutorialAction.MoveActiveItem(1, 2))

            assertEquals(listOf(ids[2], ids[1], ids[0]), activity.screenState.previewOrder)
        }
    }

    @Test
    fun `should refuse to stage a move outside the active section`() {
        onActivity { activity ->
            seedItems(activity, "Apples", "Bread")

            assertFalse(perform(activity, TutorialAction.MoveActiveItem(0, 5)))
            assertNull(activity.screenState.previewOrder)
        }
    }

    @Test
    fun `should persist the reorder and drop the staged order`() {
        onActivity { activity ->
            seedItems(activity, "Apples", "Bread")
            perform(activity, TutorialAction.MoveActiveItem(1, 0))

            assertTrue(perform(activity, TutorialAction.CommitReorder(1, 0)))
            assertNull(activity.screenState.previewOrder)
            assertEquals(listOf("Bread", "Apples"), activity.activeTitles())
        }
    }

    @Test
    fun `should leave the screen when the tutorial navigates back`() {
        onActivity { activity ->
            activity.tutorialBackCallback.isEnabled = true

            assertTrue(perform(activity, TutorialAction.NavigateBack))
            assertFalse(activity.tutorialBackCallback.isEnabled)
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun `should refuse actions belonging to the other screen`() {
        onActivity { activity ->
            assertFalse(perform(activity, TutorialAction.SubmitList))
        }
    }

    @Test
    fun `should hand back no demo list id from the items screen`() {
        onActivity { activity ->
            assertNull(runBlocking { activity.awaitDemoListId() })
        }
    }

    @Test
    fun `should draw no notification banner from the items screen`() {
        onActivity { activity ->
            assertNull(activity.bannerContent())
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun onActivity(block: (TodoListActivity) -> Unit) {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            TodoListActivity::class.java
        ).putExtra("LIST_ID", LIST_ID).putExtra("LIST_NAME", "Groceries")
        ActivityScenario.launch<TodoListActivity>(intent).use { scenario ->
            scenario.onActivity(block)
        }
    }

    private fun perform(activity: TodoListActivity, action: TutorialAction): Boolean =
        runBlocking { activity.perform(action) }

    private fun seedItems(activity: TodoListActivity, vararg titles: String) {
        val repository = ApplicationProvider.getApplicationContext<TodoListApplication>()
            .container.todoRepository
        titles.forEachIndexed { index, title ->
            repository.add(TodoItem("item-$index", title, LIST_ID, position = index))
        }
        activity.viewModel.refresh()
    }

    private fun TodoListActivity.content(): TodoListState.Content? =
        viewModel.state.value as? TodoListState.Content

    private fun TodoListActivity.activeTitles(): List<String> =
        content()?.activeItems.orEmpty().map { it.title }

    private fun TodoListActivity.completedTitles(): List<String> =
        content()?.completedItems.orEmpty().map { it.title }

    private fun TodoListActivity.activeIds(): List<String> =
        content()?.activeItems.orEmpty().map { it.id }

    private companion object {
        const val LIST_ID = "list-1"
    }
}
