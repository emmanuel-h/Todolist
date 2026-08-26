package fr.mandarine.todolist.ui

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.MainThreadDatabaseRule
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.data.SharedPreferencesTutorialStateRepository
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialPace
import fr.mandarine.todolist.ui.todolist.ItemsStage
import fr.mandarine.todolist.ui.todolist.TodoListScreenState
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

    private var left = 0

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
    fun `should write on the add line even before the pen has been put on it`() {
        onActivity { activity ->
            assertTrue(perform(activity, TutorialAction.TypeItemTitle("Apples")))
            assertEquals("Apples", activity.screenState.addRowText)
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
    fun `should peel the page off when the tutorial navigates back`() {
        onActivity { activity ->
            assertTrue(perform(activity, TutorialAction.NavigateBack))
            assertEquals(1, left)
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

    private fun onActivity(block: (ItemsStage) -> Unit) {
        val container = ApplicationProvider.getApplicationContext<TodoListApplication>().container
        val repository = container.todoRepository
        val viewModel = TodoListViewModel(
            AddTodoUseCase(repository),
            GetTodosUseCase(repository),
            ToggleTodoUseCase(repository),
            DeleteTodoUseCase(repository),
            EditTodoUseCase(repository),
            ReorderTodosUseCase(repository),
            GetTodoListsUseCase(container.todoListRepository),
            listId = LIST_ID,
            dispatcher = container.databaseDispatcher
        )
        viewModel.refresh()
        block(ItemsStage(viewModel, TodoListScreenState(), { _, bounds -> bounds }, TutorialPace()) { left += 1 })
    }

    @Test
    fun `should close the add row the demo left open when the tour is abandoned`() {
        onActivity { stage ->
            perform(stage, TutorialAction.OpenItemAddRow)
            perform(stage, TutorialAction.TypeItemTitle("🍎 Apples"))

            stage.abandon()

            assertFalse(stage.screenState.addRowExpanded)
            assertEquals("", stage.screenState.addRowText)
        }
    }

    private fun perform(activity: ItemsStage, action: TutorialAction): Boolean =
        runBlocking { activity.perform(action) }

    private fun seedItems(activity: ItemsStage, vararg titles: String) {
        val repository = ApplicationProvider.getApplicationContext<TodoListApplication>()
            .container.todoRepository
        titles.forEachIndexed { index, title ->
            repository.add(TodoItem("item-$index", title, LIST_ID, position = index))
        }
        activity.viewModel.refresh()
    }

    private fun ItemsStage.content(): TodoListState.Content? =
        viewModel.state.value as? TodoListState.Content

    private fun ItemsStage.activeTitles(): List<String> =
        content()?.activeItems.orEmpty().map { it.title }

    private fun ItemsStage.completedTitles(): List<String> =
        content()?.completedItems.orEmpty().map { it.title }

    private fun ItemsStage.activeIds(): List<String> =
        content()?.activeItems.orEmpty().map { it.id }

    private companion object {
        const val LIST_ID = "list-1"
    }
}
