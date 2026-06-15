package fr.mandarine.todolist.ui

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
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
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.layoutEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should add list when valid name is submitted`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerView().adapter!!.itemCount)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.layoutEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should accumulate lists when multiple names are submitted`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            createListViaDialog(scenario, "Shopping")
            createListViaDialog(scenario, "Personal")
            scenario.onActivity { activity ->
                assertEquals(3, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add list when name is blank`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.openCreateDialogForTest()
                activity.confirmDialogForTest()
            }
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add list when name is whitespace only`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.openCreateDialogForTest()
                activity.typeInCurrentDialogForTest("   ")
                activity.confirmDialogForTest()
            }
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should remove list when delete is confirmed`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerView().adapter!!.itemCount)
            }
            deleteFirstListViaDialog(scenario)
            scenario.onActivity { activity ->
                assertEquals(0, activity.recyclerView().adapter!!.itemCount)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.layoutEmptyLists).visibility)
            }
        }
    }

    @Test
    fun `should not remove list when delete is cancelled`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            tapDeleteButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.cancelCurrentDialogForTest()
            }
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerView().adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should navigate to TodoListActivity when list card is tapped`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
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

    @Test
    fun `should rename list when edit icon is tapped and new name is confirmed`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.typeInRenameDialogForTest("Work Revised")
                activity.confirmDialogForTest()
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val nameView = rv.getChildAt(0)!!.findViewById<MaterialTextView>(R.id.textListName)
                assertEquals("Work Revised", nameView.text.toString())
            }
        }
    }

    @Test
    fun `should not rename list when new name is blank`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.typeInRenameDialogForTest("")
                activity.confirmDialogForTest()
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val nameView = rv.getChildAt(0)!!.findViewById<MaterialTextView>(R.id.textListName)
                assertEquals("Work", nameView.text.toString())
            }
        }
    }

    @Test
    fun `should not rename list when new name is whitespace only`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.typeInRenameDialogForTest("   ")
                activity.confirmDialogForTest()
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val nameView = rv.getChildAt(0)!!.findViewById<MaterialTextView>(R.id.textListName)
                assertEquals("Work", nameView.text.toString())
            }
        }
    }

    @Test
    fun `should not rename list when rename dialog is cancelled`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.typeInRenameDialogForTest("Should Not Save")
                activity.cancelCurrentDialogForTest()
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val nameView = rv.getChildAt(0)!!.findViewById<MaterialTextView>(R.id.textListName)
                assertEquals("Work", nameView.text.toString())
            }
        }
    }

    @Test
    fun `should pre-fill rename dialog with current list name`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Groceries")
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val input = activity.currentDialogView
                    ?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editDialogRenameList)
                assertEquals("Groceries", input?.text.toString())
            }
        }
    }

    @Test
    fun `should have edit icon button on each list row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            createListViaDialog(scenario, "Work")
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val editBtn = rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnEditList)
                assertNotNull(editBtn)
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

    private fun tapEditButtonOnFirstRow(scenario: ActivityScenario<TodoListsActivity>) {
        scenario.onActivity { activity ->
            val rv = activity.recyclerView()
            rv.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
            )
            rv.layout(0, 0, 1080, 1920)
            rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnEditList).performClick()
        }
    }

    private fun deleteFirstListViaDialog(scenario: ActivityScenario<TodoListsActivity>) {
        tapDeleteButtonOnFirstRow(scenario)
        scenario.onActivity { activity ->
            activity.confirmDialogForTest()
        }
    }

    private fun TodoListsActivity.recyclerView() = findViewById<RecyclerView>(R.id.recyclerViewLists)
}
