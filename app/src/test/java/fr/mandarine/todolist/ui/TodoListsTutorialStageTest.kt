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
import org.robolectric.Shadows
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
            assertEquals(TutorialScreen.LISTS, activity.screen)
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
                DatePickerRequest(DateTarget.ADD_ROW, DateKind.DUE, null),
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

    @Test
    fun `should close the picker once a due date is chosen`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)
            perform(activity, TutorialAction.OpenDueDatePicker)

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
    fun `should refuse to create a list with no name`() {
        onActivity { activity ->
            perform(activity, TutorialAction.OpenListCreateRow)

            assertFalse(perform(activity, TutorialAction.SubmitList))
        }
    }

    @Test
    fun `should open the first list`() {
        onActivity { activity ->
            createList(activity, "Hi")

            assertTrue(perform(activity, TutorialAction.OpenFirstList))
            val next = Shadows.shadowOf(activity).nextStartedActivity
            assertNotNull(next)
            assertTrue(next!!.component!!.className.contains("TodoListActivity"))
            assertEquals("Hi", next.getStringExtra("LIST_NAME"))
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
            assertNotNull(Shadows.shadowOf(activity).nextStartedActivity)
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

            assertEquals(bounds, activity.boundsOf(TutorialAnchor.CreateListButton))
        }
    }

    @Test
    fun `should draw the banner from the first list`() {
        onActivity { activity ->
            createList(activity, "Hi")

            val banner = activity.bannerContent()

            assertEquals("Hi", banner?.listName)
        }
    }

    @Test
    fun `should draw no banner while there is no list`() {
        onActivity { activity ->
            assertNull(activity.bannerContent())
        }
    }

    @Test
    fun `should hand back the id of the demo list once it exists`() {
        onActivity { activity ->
            createList(activity, "Hi")

            assertEquals(activity.firstListId(), runBlocking { activity.awaitDemoListId() })
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun onActivity(block: (TodoListsActivity) -> Unit) {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity(block)
        }
    }

    private fun perform(activity: TodoListsActivity, action: TutorialAction): Boolean =
        runBlocking { activity.perform(action) }

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
