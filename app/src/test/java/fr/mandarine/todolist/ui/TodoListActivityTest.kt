package fr.mandarine.todolist.ui

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.material.checkbox.MaterialCheckBox
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.data.TodoListEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListActivityTest {

    private lateinit var db: TodoDatabase

    @Before
    fun setUp() {
        db = TodoDatabase.getInstance(ApplicationProvider.getApplicationContext())
        db.todoListDao().insert(TodoListEntity("test-list-id", "Test List"))
    }

    @After
    fun tearDown() {
        db.clearAllTables()
    }

    private fun launchWithListId(listName: String = "Test List"): ActivityScenario<TodoListActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TodoListActivity::class.java)
            .putExtra("LIST_ID", "test-list-id")
            .putExtra("LIST_NAME", listName)
        return ActivityScenario.launch(intent)
    }

    private fun addItemViaInlineBar(title: String) {
        onView(withId(R.id.editAddItem)).perform(replaceText(title))
        onView(withId(R.id.btnAddItem)).perform(click())
    }

    @Test
    fun `should display list name in toolbar title`() {
        launchWithListId("Groceries").use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Groceries", activity.supportActionBar?.title)
            }
        }
    }

    @Test
    fun `should show zero todo items when no todos have been added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(0, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should show empty state when no todos have been added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.layoutEmptyTodos).visibility)
            }
        }
    }

    @Test
    fun `should add item to list when valid title is submitted via inline bar`() {
        launchWithListId().use { scenario ->
            addItemViaInlineBar("Buy milk")
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(1, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should hide empty state after first item is added`() {
        launchWithListId().use { scenario ->
            addItemViaInlineBar("Buy milk")
            scenario.onActivity { activity ->
                assertEquals(View.GONE, activity.findViewById<View>(R.id.layoutEmptyTodos).visibility)
            }
        }
    }

    @Test
    fun `should accumulate items when multiple todos are added`() {
        launchWithListId().use { scenario ->
            addItemViaInlineBar("Buy milk")
            addItemViaInlineBar("Call dentist")
            addItemViaInlineBar("Walk the dog")
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(3, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add item when title is blank`() {
        launchWithListId().use { scenario ->
            addItemViaInlineBar("   ")
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(0, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add item when title is empty`() {
        launchWithListId().use { scenario ->
            addItemViaInlineBar("")
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(0, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should check item when checkbox is tapped`() {
        launchWithListId().use { scenario ->
            addItemViaInlineBar("Buy milk")
            scenario.onActivity { activity ->
                val checkBox = firstCheckBox(activity)
                assertFalse(checkBox.isChecked)
                checkBox.performClick()
                assertTrue(checkBox.isChecked)
            }
        }
    }

    @Test
    fun `should uncheck item when checked checkbox is tapped again`() {
        launchWithListId().use { scenario ->
            addItemViaInlineBar("Buy milk")
            scenario.onActivity { activity ->
                val checkBox = firstCheckBox(activity)
                checkBox.performClick()
                checkBox.performClick()
                assertFalse(checkBox.isChecked)
            }
        }
    }

    @Test
    fun `should persist completed state across list refresh when item is toggled`() {
        launchWithListId().use { scenario ->
            addItemViaInlineBar("Buy milk")
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                firstCheckBox(activity).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                assertTrue(firstCheckBox(activity).isChecked)
            }
        }
    }

    private fun firstCheckBox(activity: TodoListActivity): MaterialCheckBox {
        val rv = activity.recyclerView()
        layoutRecyclerView(rv)
        return rv.getChildAt(0)!!.findViewById(R.id.checkCompleted)
    }

    private fun layoutRecyclerView(rv: RecyclerView) {
        rv.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        rv.layout(0, 0, 1080, 1920)
    }

    private fun TodoListActivity.recyclerView() = recyclerViewInternal
}
