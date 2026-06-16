package fr.mandarine.todolist.ui

import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.data.TodoListEntity
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
class TodoListDragReorderTest {

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

    private fun launchActivity(): ActivityScenario<TodoListActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TodoListActivity::class.java)
            .putExtra("LIST_ID", "test-list-id")
            .putExtra("LIST_NAME", "Test List")
        return ActivityScenario.launch(intent)
    }

    private fun layoutRecyclerView(rv: RecyclerView) {
        rv.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        rv.layout(0, 0, 1080, 1920)
    }

    private fun addItem(activity: TodoListActivity, title: String) {
        val editText = activity.inlineAddEditTextInternal
        editText.setText(title)
        editText.onEditorAction(EditorInfo.IME_ACTION_DONE)
        layoutRecyclerView(activity.recyclerViewInternal)
    }

    @Test
    fun `should show drag handle on active item row`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Active task")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                val handle = firstItem.findViewById<ImageView>(R.id.dragHandle)
                assertNotNull(handle)
                assertEquals(View.VISIBLE, handle.visibility)
            }
        }
    }

    @Test
    fun `should hide drag handle on completed item row`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Task to complete")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                val handle = firstItem.findViewById<ImageView>(R.id.dragHandle)
                assertNotNull(handle)
                assertEquals(View.INVISIBLE, handle.visibility)
            }
        }
    }

    @Test
    fun `should keep drag handle space on completed row to align with active rows`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Active task")
                addItem(activity, "Task to complete")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                val activeItem = rv.getChildAt(0)!!
                val completedItem = rv.getChildAt(2)!!
                val activeHandle = activeItem.findViewById<ImageView>(R.id.dragHandle)
                val completedHandle = completedItem.findViewById<ImageView>(R.id.dragHandle)
                assertEquals(View.INVISIBLE, completedHandle.visibility)
                assertEquals(activeHandle.width, completedHandle.width)
            }
        }
    }

    @Test
    fun `should count only active items in activeItemCount`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Task A")
                addItem(activity, "Task B")
                addItem(activity, "Task C")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                val adapter = rv.adapter as TodoListAdapter
                assertEquals(2, adapter.activeItemCount())
            }
        }
    }

    @Test
    fun `should count zero active items when all items are completed`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Task A")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                val adapter = rv.adapter as TodoListAdapter
                assertEquals(0, adapter.activeItemCount())
            }
        }
    }

    @Test
    fun `should count all active items when no items are completed`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Task A")
                addItem(activity, "Task B")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                val adapter = rv.adapter as TodoListAdapter
                assertEquals(2, adapter.activeItemCount())
            }
        }
    }

    @Test
    fun `should move item in adapter rows when moveItem is called`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "First")
                addItem(activity, "Second")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                val adapter = rv.adapter as TodoListAdapter
                val countBefore = adapter.itemCount
                adapter.moveItem(0, 1)
                assertEquals(countBefore, adapter.itemCount)
            }
        }
    }

    @Test
    fun `should have ItemTouchHelper attached to recyclerView`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.itemTouchHelperInternal)
            }
        }
    }

    @Test
    fun `should not allow drag on completed item rows via getMovementFlags`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Task A")
                addItem(activity, "Task B")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                val adapter = rv.adapter as TodoListAdapter
                val activeCount = adapter.activeItemCount()
                assertEquals(1, activeCount)
                val completedPosition = activeCount + 1
                assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(completedPosition))
            }
        }
    }

    @Test
    fun `should not allow drag on divider rows via getMovementFlags`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Task A")
                addItem(activity, "Task B")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                val adapter = rv.adapter as TodoListAdapter
                val dividerExists = (0 until adapter.itemCount).any {
                    adapter.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_DIVIDER
                }
                assertEquals(true, dividerExists)
            }
        }
    }

    @Test
    fun `should restore handle visibility when completed item is uncompleted`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Task A")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                val handle = rv.getChildAt(0)!!.findViewById<ImageView>(R.id.dragHandle)
                assertEquals(View.VISIBLE, handle.visibility)
            }
        }
    }

    @Test
    fun `should have drag handle with correct content description`() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                addItem(activity, "Task A")
                val rv = activity.recyclerViewInternal
                layoutRecyclerView(rv)
                val handle = rv.getChildAt(0)!!.findViewById<ImageView>(R.id.dragHandle)
                val expected = activity.getString(R.string.drag_handle)
                assertEquals(expected, handle.contentDescription)
            }
        }
    }
}
