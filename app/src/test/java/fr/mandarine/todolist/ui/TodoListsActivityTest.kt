package fr.mandarine.todolist.ui

import android.content.Intent
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.material.button.MaterialButton
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.TodoDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsActivityTest {

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
    fun `should show empty state when no lists have been created`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.textEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should add list when valid name is submitted`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog("Work")
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerView().adapter!!.itemCount)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.textEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should accumulate lists when multiple names are submitted`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog("Work")
            createListViaDialog("Shopping")
            createListViaDialog("Personal")
            scenario.onActivity { activity ->
                assertEquals(3, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add list when name is blank`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            onView(withId(R.id.fabAddList)).perform(click())
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add list when name is whitespace only`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            onView(withId(R.id.fabAddList)).perform(click())
            onView(isAssignableFrom(EditText::class.java)).inRoot(isDialog()).perform(replaceText("   "))
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should remove list when delete is confirmed`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog("Work")
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerView().adapter!!.itemCount)
            }
            deleteFirstListViaDialog(scenario)
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.textEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should not remove list when delete is cancelled`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog("Work")
            tapDeleteButtonOnFirstRow(scenario)
            onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click())
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should navigate to TodoListActivity when list card is tapped`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog("Work")
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                rv.getChildAt(0)!!.performClick()
            }
            scenario.onActivity { activity ->
                val shadowActivity = org.robolectric.Shadows.shadowOf(activity)
                val nextIntent: Intent? = shadowActivity.nextStartedActivity
                assertNotNull(nextIntent)
                assertTrue(nextIntent!!.component!!.className.contains("TodoListActivity"))
                assertNotNull(nextIntent.getStringExtra("LIST_ID"))
                assertEquals("Work", nextIntent.getStringExtra("LIST_NAME"))
            }
        }
    }

    private fun createListViaDialog(name: String) {
        onView(withId(R.id.fabAddList)).perform(click())
        onView(isAssignableFrom(EditText::class.java)).inRoot(isDialog()).perform(replaceText(name))
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
    }

    private fun tapDeleteButtonOnFirstRow(scenario: ActivityScenario<TodoListsActivity>) {
        scenario.onActivity { activity ->
            val rv = activity.recyclerView()
            rv.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
            )
            rv.layout(0, 0, 1080, 1920)
            rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnDeleteList).performClick()
        }
    }

    private fun deleteFirstListViaDialog(scenario: ActivityScenario<TodoListsActivity>) {
        tapDeleteButtonOnFirstRow(scenario)
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
    }

    private fun TodoListsActivity.recyclerView() = findViewById<RecyclerView>(R.id.recyclerViewLists)
}
