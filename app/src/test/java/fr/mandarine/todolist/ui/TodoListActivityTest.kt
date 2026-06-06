package fr.mandarine.todolist.ui

import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.material.checkbox.MaterialCheckBox
import fr.mandarine.todolist.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListActivityTest {

    @Test
    fun `should show empty list when no todos have been added`() {
        ActivityScenario.launch(TodoListActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should add item to list when valid title is submitted`() {
        ActivityScenario.launch(TodoListActivity::class.java).use { scenario ->
            addTodoViaDialog("Buy milk")
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should accumulate items when multiple todos are added`() {
        ActivityScenario.launch(TodoListActivity::class.java).use { scenario ->
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
        ActivityScenario.launch(TodoListActivity::class.java).use { scenario ->
            onView(withId(R.id.fabAdd)).perform(click())
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add item when title is whitespace only`() {
        ActivityScenario.launch(TodoListActivity::class.java).use { scenario ->
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
        ActivityScenario.launch(TodoListActivity::class.java).use { scenario ->
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
        ActivityScenario.launch(TodoListActivity::class.java).use { scenario ->
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
