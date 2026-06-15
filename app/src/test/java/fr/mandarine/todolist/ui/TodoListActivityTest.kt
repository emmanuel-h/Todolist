package fr.mandarine.todolist.ui

import android.app.Application
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowInputMethodManager

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

    private fun addItemViaInlineRow(activity: TodoListActivity, title: String) {
        val rv = activity.recyclerView()
        layoutRecyclerView(rv)
        val addRow = rv.getChildAt(rv.childCount - 1)
            ?: error("no children in recycler view")
        val editText = addRow.findViewById<TextInputEditText>(R.id.editInlineAdd)
        editText.setText(title)
        editText.onEditorAction(EditorInfo.IME_ACTION_DONE)
    }

    private fun addItemViaSubmitButton(activity: TodoListActivity, title: String) {
        val rv = activity.recyclerView()
        layoutRecyclerView(rv)
        val addRow = rv.getChildAt(rv.childCount - 1)
            ?: error("no children in recycler view")
        val editText = addRow.findViewById<TextInputEditText>(R.id.editInlineAdd)
        val submitButton = addRow.findViewById<MaterialButton>(R.id.btnInlineSubmit)
        editText.setText(title)
        submitButton.performClick()
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
    fun `should show one item in recycler when no todos have been added (inline add row only)`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(1, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should show empty state view when no todos have been added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.layoutEmptyTodos).visibility)
            }
        }
    }

    @Test
    fun `should hide empty state view after a todo is added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                assertEquals(View.GONE, activity.findViewById<View>(R.id.layoutEmptyTodos).visibility)
            }
        }
    }

    @Test
    fun `should show two items in recycler after one todo is added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(2, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should accumulate items when multiple todos are added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                addItemViaInlineRow(activity, "Call dentist")
                addItemViaInlineRow(activity, "Walk the dog")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(4, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add item when title is blank`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "   ")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(1, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add item when title is empty`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(1, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should check item when checkbox is tapped`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
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
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
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
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                firstCheckBox(activity).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                assertTrue(firstCheckBox(activity).isChecked)
            }
        }
    }

    @Test
    fun `should place inline add row as last item after adding todos`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val lastChild = rv.getChildAt(rv.childCount - 1)
                val editText = lastChild?.findViewById<TextInputEditText>(R.id.editInlineAdd)
                assertTrue(editText != null)
            }
        }
    }

    @Test
    fun `should add item when submit button is tapped with non-blank title`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaSubmitButton(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(2, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should not add item when submit button is tapped with blank title`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaSubmitButton(activity, "   ")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(1, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should clear focus on inline editText when touch event is outside its bounds`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val addRow = rv.getChildAt(rv.childCount - 1)!!
                val editText = addRow.findViewById<TextInputEditText>(R.id.editInlineAdd)

                editText.requestFocus()
                assertTrue(editText.hasFocus())

                val outsideX = 5000f
                val outsideY = 5000f
                val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, outsideX, outsideY, 0)
                activity.dispatchTouchEvent(downEvent)
                downEvent.recycle()

                assertFalse(editText.hasFocus())
            }
        }
    }

    @Test
    fun `should hide soft keyboard when touch event is outside the inline editText bounds`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val addRow = rv.getChildAt(rv.childCount - 1)!!
                val editText = addRow.findViewById<TextInputEditText>(R.id.editInlineAdd)

                editText.requestFocus()

                val imm = activity.getSystemService(InputMethodManager::class.java)
                val shadowImm = Shadows.shadowOf(imm) as ShadowInputMethodManager
                imm.showSoftInput(editText, 0)
                assertTrue(shadowImm.isSoftInputVisible)

                val outsideX = 5000f
                val outsideY = 5000f
                val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, outsideX, outsideY, 0)
                activity.dispatchTouchEvent(downEvent)
                downEvent.recycle()

                assertFalse(shadowImm.isSoftInputVisible)
            }
        }
    }

    @Test
    fun `should not clear focus when touch event is inside the inline editText bounds`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val addRow = rv.getChildAt(rv.childCount - 1)!!
                val editText = addRow.findViewById<TextInputEditText>(R.id.editInlineAdd)

                editText.requestFocus()
                assertTrue(editText.hasFocus())

                val location = IntArray(2)
                editText.getLocationOnScreen(location)
                val insideX = (location[0] + editText.width / 2).toFloat()
                val insideY = (location[1] + editText.height / 2).toFloat()
                val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, insideX, insideY, 0)
                activity.dispatchTouchEvent(downEvent)
                downEvent.recycle()

                assertTrue(editText.hasFocus())
            }
        }
    }

    @Test
    fun `should not crash when no view currently has focus during touch event`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                activity.currentFocus?.clearFocus()

                val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
                activity.dispatchTouchEvent(downEvent)
                downEvent.recycle()
            }
        }
    }

    @Test
    fun `should not clear focus when focused view is not an EditText`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)

                val nonEditView = View(activity)
                nonEditView.isFocusable = true
                nonEditView.isFocusableInTouchMode = true
                activity.addContentView(nonEditView, android.view.ViewGroup.LayoutParams(100, 100))
                nonEditView.layout(0, 0, 100, 100)
                nonEditView.requestFocus()
                assertTrue(nonEditView.hasFocus())

                val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 5000f, 5000f, 0)
                activity.dispatchTouchEvent(downEvent)
                downEvent.recycle()

                assertTrue(nonEditView.hasFocus())
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

    @Test
    fun `should show divider row when both active and completed sections are non-empty`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                addItemViaInlineRow(activity, "Call dentist")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                firstCheckBox(activity).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)

                val hasDivider = (0 until rv.adapter!!.itemCount).any {
                    rv.adapter!!.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_DIVIDER
                }
                assertTrue(hasDivider)
            }
        }
    }

    @Test
    fun `should not show divider row when all items are active`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                addItemViaInlineRow(activity, "Call dentist")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)

                val hasDivider = (0 until rv.adapter!!.itemCount).any {
                    rv.adapter!!.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_DIVIDER
                }
                assertFalse(hasDivider)
            }
        }
    }

    @Test
    fun `should not show divider row when all items are completed`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                firstCheckBox(activity).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)

                val hasDivider = (0 until rv.adapter!!.itemCount).any {
                    rv.adapter!!.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_DIVIDER
                }
                assertFalse(hasDivider)
            }
        }
    }

    @Test
    fun `should place completed item after divider in adapter when toggled`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Active item")
                addItemViaInlineRow(activity, "To complete")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)

                val secondCheckBox = rv.getChildAt(1)!!.findViewById<MaterialCheckBox>(R.id.checkCompleted)
                secondCheckBox.performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)

                val dividerPosition = (0 until rv.adapter!!.itemCount).first {
                    rv.adapter!!.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_DIVIDER
                }
                assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, rv.adapter!!.getItemViewType(dividerPosition + 1))
            }
        }
    }
}
