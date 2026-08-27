package fr.mandarine.todolist.ui

import androidx.test.core.app.ActivityScenario
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.MainThreadDatabaseRule
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.data.SharedPreferencesTutorialStateRepository
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.ui.nav.ItemsRoute
import fr.mandarine.todolist.ui.nav.ListsRoute
import fr.mandarine.todolist.ui.todolists.DateKind
import fr.mandarine.todolist.ui.todolists.DatePickerRequest
import fr.mandarine.todolist.ui.todolists.DateTarget
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class TodoListsTutorialStageTest {

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
    fun `should stage the lists screen`() {
        onActivity { activity ->
            assertEquals(TutorialScreen.LISTS, activity.stage.screen)
        }
    }

    @Test
    fun `should open the create row`() {
        onActivity { activity ->
            assertTrue(perform(activity, TutorialAction.OpenListCreateRow))
            assertTrue(activity.screenState.addRowExpanded)
        }
    }

    @Test
    fun `should refuse to open a create row that is already open`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertFalse(perform(activity, TutorialAction.OpenListCreateRow))
        }
    }

    @Test
    fun `should type the list name one character at a time`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertTrue(perform(activity, TutorialAction.TypeListName("Hi")))
            assertEquals("Hi", activity.screenState.addRowText)
        }
    }

    @Test
    fun `should write the name onto the open create row`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertTrue(perform(activity, TutorialAction.TypeListName("Hi")))
            assertEquals("Hi", activity.screenState.addRowText)
        }
    }

    @Test
    fun `should refuse to type a name while the create row is closed`() {
        onActivity { activity ->
            assertFalse(perform(activity, TutorialAction.TypeListName("Hi")))
        }
    }

    @Test
    fun `should ask for a due date on the create row`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertTrue(perform(activity, TutorialAction.OpenDueDatePicker))
            assertEquals(
                DatePickerRequest(DateTarget.AddRow, DateKind.DUE, null),
                activity.screenState.datePickerRequest
            )
        }
    }

    @Test
    fun `should refuse to ask for a due date while the create row is closed`() {
        onActivity { activity ->
            assertFalse(perform(activity, TutorialAction.OpenDueDatePicker))
        }
    }

    /**
     * Choosing a day and putting the calendar away are two beats, not one. They
     * used to be the same one, so the sheet vanished in the frame the day was
     * chosen and the reader saw a calendar flash rather than a day being picked.
     */
    @Test
    fun `should circle the day on the calendar and leave it standing`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.OpenDueDatePicker)

            assertTrue(perform(activity, TutorialAction.PickDueDate(DATE)))
            assertEquals(DATE, activity.screenState.addRowSelection.dueDate)
            assertEquals(DATE, activity.screenState.datePickerRequest?.initial)
        }
    }

    @Test
    fun `should put the calendar away when it is asked to and not before`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.OpenDueDatePicker)
            perform(activity, TutorialAction.PickDueDate(DATE))

            assertTrue(perform(activity, TutorialAction.CloseDatePicker))
            assertNull(activity.screenState.datePickerRequest)
            assertEquals(DATE, activity.screenState.addRowSelection.dueDate)
        }
    }

    @Test
    fun `should refuse to close a calendar that is not open`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertFalse(perform(activity, TutorialAction.CloseDatePicker))
        }
    }

    @Test
    fun `should still write the day when it is chosen with no calendar open`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertTrue(perform(activity, TutorialAction.PickDueDate(DATE)))
            assertEquals(DATE, activity.screenState.addRowSelection.dueDate)
            assertNull(activity.screenState.datePickerRequest)
        }
    }

    @Test
    fun `should create the list with the name and due date it was given`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.TypeListName("Hi"))
            perform(activity, TutorialAction.PickDueDate(DATE))

            assertTrue(perform(activity, TutorialAction.SubmitList))
            assertFalse(activity.screenState.addRowExpanded)
            assertEquals("Hi", activity.firstListName())
        }
    }

    @Test
    fun `should leave every row already on the page where it was when the demo writes`() {
        onActivity { activity ->
            createList(activity, "Travail")
            createList(activity, "Voyage")
            val before = activity.listPositions()

            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.TypeListName("Hi"))
            perform(activity, TutorialAction.SubmitList)

            assertEquals(before, activity.listPositions() - "Hi")
        }
    }

    @Test
    fun `should write the demo list above every row already on the page`() {
        onActivity { activity ->
            createList(activity, "Travail")

            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.TypeListName("Hi"))
            perform(activity, TutorialAction.SubmitList)

            assertEquals("Hi", activity.firstListName())
        }
    }

    @Test
    fun `should claim the demo list as the tutorial's before a row of it is written`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.TypeListName("Hi"))
            perform(activity, TutorialAction.SubmitList)

            assertEquals(activity.firstListId(), pendingDemoListId())
        }
    }

    @Test
    fun `should claim nothing while the demo has written nothing`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertNull(pendingDemoListId())
        }
    }

    @Test
    fun `should refuse to create a list with no name`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertFalse(perform(activity, TutorialAction.SubmitList))
        }
    }

    @Test
    fun `should lay the page of the first list over the page of lists`() {
        onActivity { activity ->
            createList(activity, "Hi")

            assertTrue(perform(activity, TutorialAction.OpenFirstList))
            assertEquals(
                listOf(ListsRoute, ItemsRoute(requireNotNull(activity.firstListId()))),
                activity.backStack.toList()
            )
            assertEquals(TutorialScreen.ITEMS, activity.stage.screen)
        }
    }

    @Test
    fun `should refuse to open a first list that does not exist`() {
        onActivity { activity ->
            assertFalse(perform(activity, TutorialAction.OpenFirstList))
        }
    }

    @Test
    fun `should arm the delete on the first list without deleting it`() {
        onActivity { activity ->
            createList(activity, "Hi")

            assertTrue(perform(activity, TutorialAction.RequestDeleteFirstList))
            assertNotNull(activity.screenState.deletion.pending)
            assertEquals("Hi", activity.firstListName())
        }
    }

    @Test
    fun `should arm the delete on a finished list that has moved below the divider`() {
        onActivity { activity ->
            createFinishedList(activity, "Hi")

            assertTrue(perform(activity, TutorialAction.RequestDeleteFirstList))
            assertNotNull(activity.screenState.deletion.pending)
        }
    }

    @Test
    fun `should open a finished list that has moved below the divider`() {
        onActivity { activity ->
            createFinishedList(activity, "Hi")

            assertTrue(perform(activity, TutorialAction.OpenFirstList))
            assertEquals(TutorialScreen.ITEMS, activity.stage.screen)
        }
    }

    @Test
    fun `should refuse to arm a delete when there is no list`() {
        onActivity { activity ->
            assertFalse(perform(activity, TutorialAction.RequestDeleteFirstList))
        }
    }

    @Test
    fun `should delete the first list once the delete is confirmed`() {
        onActivity { activity ->
            createList(activity, "Hi")
            perform(activity, TutorialAction.RequestDeleteFirstList)

            assertTrue(perform(activity, TutorialAction.ConfirmDeleteFirstList))
            assertNull(activity.screenState.deletion.pending)
            assertNull(activity.firstListName())
        }
    }

    @Test
    fun `should refuse to confirm a delete that was never armed`() {
        onActivity { activity ->
            createList(activity, "Hi")

            assertFalse(perform(activity, TutorialAction.ConfirmDeleteFirstList))
        }
    }

    @Test
    fun `should refuse actions belonging to the other screen`() {
        onActivity { activity ->
            assertFalse(perform(activity, TutorialAction.SubmitItem))
        }
    }

    @Test
    fun `should read anchor bounds from the screen state`() {
        onActivity { activity ->
            val bounds = TutorialBounds(left = 1, top = 2, width = 3, height = 4)
            activity.screenState.putBounds(TutorialAnchor.CreateListButton, bounds)

            assertEquals(bounds, activity.stage.boundsOf(TutorialAnchor.CreateListButton))
        }
    }

    @Test
    fun `should draw the banner from the first list`() {
        onActivity { activity ->
            createList(activity, "Hi")

            val banner = activity.stage.bannerContent()

            assertEquals("Hi", banner?.listName)
        }
    }

    @Test
    fun `should draw no banner while there is no list`() {
        onActivity { activity ->
            assertNull(activity.stage.bannerContent())
        }
    }

    @Test
    fun `should hand back the id of the demo list once it exists`() {
        onActivity { activity ->
            createList(activity, "Hi")

            assertEquals(activity.firstListId(), runBlocking { activity.stage.awaitDemoListId() })
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun onActivity(block: (TodoListsActivity) -> Unit) {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity(block)
        }
    }

    /**
     * A tour that stops partway through has the reader's own create row open with
     * the demo's name typed into it. Left there, the reader's next tap on the page
     * submits it and they own a list they watched somebody else start.
     */
    @Test
    fun `should close the create row the demo left open when the tour is abandoned`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.TypeListName("🛒 Groceries"))

            activity.stage.abandon()

            assertFalse(activity.screenState.addRowExpanded)
            assertEquals("", activity.screenState.addRowText)
        }
    }

    @Test
    fun `should close the calendar the demo left open when the tour is abandoned`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.OpenDueDatePicker)

            activity.stage.abandon()

            assertNull(activity.screenState.datePickerRequest)
        }
    }

    @Test
    fun `should close the sheet the demo left open when the tour is abandoned`() {
        onActivity { activity ->
            createList(activity, "Reader's own")
            perform(activity, TutorialAction.OpenFirstListEditor)

            activity.stage.abandon()

            assertNull(activity.screenState.rename)
        }
    }

    @Test
    fun `should let go of the row the demo left held aside when the tour is abandoned`() {
        onActivity { activity ->
            createList(activity, "Reader's own")
            perform(activity, TutorialAction.PullFirstList(-40f))

            activity.stage.abandon()

            assertEquals(0f, activity.screenState.demoPull, 0.001f)
        }
    }

    private fun perform(activity: TodoListsActivity, action: TutorialAction): Boolean =
        runBlocking { activity.stage.perform(action) }

    private fun createList(activity: TodoListsActivity, name: String) {
        activity.viewModel.createList(name)
    }

    private fun createFinishedList(activity: TodoListsActivity, name: String) {
        createList(activity, name)
        val container = ApplicationProvider.getApplicationContext<TodoListApplication>().container
        val listId = requireNotNull(activity.firstListId())
        val item = TodoItem(id = "item-1", title = "done", listId = listId)
        container.todoRepository.add(item)
        container.todoRepository.toggle(item.id)
        activity.viewModel.refresh()
    }

    private fun pendingDemoListId(): String? {
        val app = ApplicationProvider.getApplicationContext<TodoListApplication>()
        return SharedPreferencesTutorialStateRepository(app).getPendingDemoListId()
    }

    private fun TodoListsActivity.listPositions(): Map<String, Int> =
        (viewModel.state.value as? fr.mandarine.todolist.presentation.TodoListsState.Content)
            ?.let { it.activeSummaries + it.doneSummaries }
            ?.associate { it.list.name to it.list.position }
            .orEmpty()

    private fun TodoListsActivity.firstListName(): String? = firstSummary()?.list?.name

    private fun TodoListsActivity.firstListId(): String? = firstSummary()?.list?.id

    private fun TodoListsActivity.firstSummary() =
        (viewModel.state.value as? fr.mandarine.todolist.presentation.TodoListsState.Content)
            ?.activeSummaries
            ?.firstOrNull()

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 3, 14)
    }
}
