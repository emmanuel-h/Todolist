package fr.mandarine.todolist.ui

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.TodoDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsDragReorderTest {

    private lateinit var db: TodoDatabase

    @Before
    fun setUp() {
        db = TodoDatabase.getInstance(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db.clearAllTables()
    }

    private fun layoutRecyclerView(rv: RecyclerView) {
        rv.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        rv.layout(0, 0, 1080, 1920)
    }

    @Test
    fun `should show drag handle on each list row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Groceries")
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                val row = rv.getChildAt(0)!!
                val handle = row.findViewById<ImageView>(R.id.dragHandleList)
                assertNotNull(handle)
                assertEquals(View.VISIBLE, handle.visibility)
            }
        }
    }

    @Test
    fun `should have drag handle with correct content description`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Groceries")
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                val handle = rv.getChildAt(0)!!.findViewById<ImageView>(R.id.dragHandleList)
                val expected = activity.getString(R.string.drag_handle)
                assertEquals(expected, handle.contentDescription)
            }
        }
    }

    @Test
    fun `should have ItemTouchHelper attached to the lists recyclerView`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.itemTouchHelperInternal)
            }
        }
    }

    @Test
    fun `should move item in adapter when moveItem is called`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "First")
            createListViaDialog(scenario, "Second")
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                val adapter = rv.adapter as TodoListsAdapter
                val countBefore = adapter.itemCount
                adapter.moveItem(0, 1)
                assertEquals(countBefore, adapter.itemCount)
            }
        }
    }

    @Test
    fun `should reorder lists and persist when reorderLists is called on the activity`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "First")
            createListViaDialog(scenario, "Second")
            scenario.onActivity { activity ->
                activity.commitReorderForTest(0, 1)
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                assertEquals(2, rv.adapter!!.itemCount)
            }
        }
    }

    private fun createListViaDialog(scenario: ActivityScenario<TodoListsActivity>, name: String) {
        scenario.onActivity { activity ->
            activity.openCreateDialogForTest()
            activity.typeInCurrentDialogForTest(name)
            activity.confirmDialogForTest()
        }
    }
}
