package fr.mandarine.todolist.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.TodoDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsInlineAddTest {

    private lateinit var db: TodoDatabase

    @Before
    fun setUp() {
        db = TodoDatabase.getInstance(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db.clearAllTables()
    }

    @Test
    fun `should show inline add row when FAB is tapped`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                assertEquals(View.VISIBLE, activity.inlineAddRowInternal.visibility)
            }
        }
    }

    @Test
    fun `should hide FAB when inline add row is active`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                assertEquals(View.GONE, activity.findViewById<FloatingActionButton>(R.id.fabAddList).visibility)
            }
        }
    }

    @Test
    fun `should hide inline add row initially`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.GONE, activity.inlineAddRowInternal.visibility)
            }
        }
    }

    @Test
    fun `should show FAB initially`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<FloatingActionButton>(R.id.fabAddList).visibility)
            }
        }
    }

    @Test
    fun `should add list when valid name is submitted via inline row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaInlineRow(scenario, "Work")
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerViewLists().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should hide inline add row after successful submission`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaInlineRow(scenario, "Work")
            scenario.onActivity { activity ->
                assertEquals(View.GONE, activity.inlineAddRowInternal.visibility)
            }
        }
    }

    @Test
    fun `should show FAB after successful submission`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaInlineRow(scenario, "Work")
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<FloatingActionButton>(R.id.fabAddList).visibility)
            }
        }
    }

    @Test
    fun `should not add list when name is blank and keep inline row visible`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.typeInInlineRowForTest("   ")
                activity.submitInlineRowForTest()
            }
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerViewLists().adapter!!.itemCount)
                assertEquals(View.VISIBLE, activity.inlineAddRowInternal.visibility)
            }
        }
    }

    @Test
    fun `should not add list when name is empty and keep inline row visible`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.submitInlineRowForTest()
            }
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerViewLists().adapter!!.itemCount)
                assertEquals(View.VISIBLE, activity.inlineAddRowInternal.visibility)
            }
        }
    }

    @Test
    fun `should hide inline add row when cancel is tapped`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.cancelInlineRowForTest()
                assertEquals(View.GONE, activity.inlineAddRowInternal.visibility)
            }
        }
    }

    @Test
    fun `should show FAB when cancel is tapped`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.cancelInlineRowForTest()
                assertEquals(View.VISIBLE, activity.findViewById<FloatingActionButton>(R.id.fabAddList).visibility)
            }
        }
    }

    @Test
    fun `should hide empty-state illustration when inline add is active`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                assertEquals(View.GONE, activity.findViewById<View>(R.id.layoutEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should restore empty-state illustration after cancel when no lists exist`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.cancelInlineRowForTest()
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.layoutEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should not show empty-state after cancel when lists exist`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaInlineRow(scenario, "Work")
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.cancelInlineRowForTest()
                assertEquals(View.GONE, activity.findViewById<View>(R.id.layoutEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should accumulate lists when multiple names are submitted via inline row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaInlineRow(scenario, "Work")
            createListViaInlineRow(scenario, "Shopping")
            createListViaInlineRow(scenario, "Personal")
            scenario.onActivity { activity ->
                assertEquals(3, activity.recyclerViewLists().adapter!!.itemCount)
            }
        }
    }

    private fun createListViaInlineRow(scenario: ActivityScenario<TodoListsActivity>, name: String) {
        scenario.onActivity { activity ->
            activity.tapFab()
            activity.typeInInlineRowForTest(name)
            activity.submitInlineRowForTest()
        }
    }

    private fun TodoListsActivity.recyclerViewLists() =
        findViewById<RecyclerView>(R.id.recyclerViewLists)
}
