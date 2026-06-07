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

    @Test
    fun `should display list name in toolbar title`() {
        launchWithListId("Groceries").use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Groceries", activity.supportActionBar?.title)
            }
        }
    }

    @Test
    fun `should show empty list when no todos have been added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should add item to list when valid title is submitted`() {
        launchWithListId().use { scenario ->
            addTodoViaDialog("Buy milk")
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should accumulate items when multiple todos are added`() {
        launchWithListId().use { scenario ->
            addTodoViaDialog("Buy milk")
            addTodoViaDialog("Call dentist")
            addTodoViaDialog("Walk the dog")
            scenario.onActivity { activity ->
                assertEquals(3, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add item when title is blank`() {
        launchWithListId().use { scenario ->
            onView(withId(R.id.fabAdd)).perform(click())
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add item when title is whitespace only`() {
        launchWithListId().use { scenario ->
            onView(withId(R.id.fabAdd)).perform(click())
            onView(isAssignableFrom(EditText::class.java)).inRoot(isDialog()).perform(replaceText("   "))
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should check item when checkbox is tapped`() {
        launchWithListId().use { scenario ->
            addTodoViaDialog("Buy milk")
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
            addTodoViaDialog("Buy milk")
            scenario.onActivity { activity ->
                val checkBox = firstCheckBox(activity)
                checkBox.performClick()
                checkBox.performClick()
                assertFalse(checkBox.isChecked)
            }
        }
    }

    private fun addTodoViaDialog(title: String) {
        onView(withId(R.id.fabAdd)).perform(click())
        onView(isAssignableFrom(EditText::class.java)).inRoot(isDialog()).perform(replaceText(title))
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
    }

    private fun firstCheckBox(activity: TodoListActivity): MaterialCheckBox {
        val rv = activity.recyclerView()
        rv.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        rv.layout(0, 0, 1080, 1920)
        return rv.getChildAt(0)!!.findViewById(R.id.checkCompleted)
    }

    private fun TodoListActivity.recyclerView() = findViewById<RecyclerView>(R.id.recyclerView)
}
