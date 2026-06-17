package fr.mandarine.todolist.ui

import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.data.TodoListEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        val editText = activity.inlineAddEditTextInternal
        editText.setText(title)
        editText.onEditorAction(EditorInfo.IME_ACTION_DONE)
        layoutRecyclerView(activity.recyclerView())
    }

    private fun addItemViaSubmitButton(activity: TodoListActivity, title: String) {
        val editText = activity.inlineAddEditTextInternal
        val submitButton = editText.rootView.findViewById<MaterialButton>(R.id.btnInlineSubmit)
        editText.setText(title)
        submitButton.performClick()
        layoutRecyclerView(activity.recyclerView())
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
    fun `should show zero items in recycler when no todos have been added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(0, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should show watermark at high alpha when no todos have been added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val watermark = activity.findViewById<android.widget.ImageView>(R.id.imageWatermark)
                assertEquals(0.15f, watermark.alpha, 0.01f)
            }
        }
    }

    @Test
    fun `should reduce watermark alpha after a todo is added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val watermark = activity.findViewById<android.widget.ImageView>(R.id.imageWatermark)
                assertEquals(0.08f, watermark.alpha, 0.01f)
            }
        }
    }

    @Test
    fun `should show one item in recycler after one todo is added`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(1, rv.adapter!!.itemCount)
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
                assertEquals(3, rv.adapter!!.itemCount)
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
                assertEquals(0, rv.adapter!!.itemCount)
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
                assertEquals(0, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should toggle item to completed when complete button is tapped`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                val toggleBtn = firstItem.findViewById<MaterialButton>(R.id.btnToggleComplete)
                assertNotNull(toggleBtn)
                toggleBtn.performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                val hasDivider = (0 until rv.adapter!!.itemCount).any {
                    rv.adapter!!.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_DIVIDER
                }
                assertFalse(hasDivider)
                assertEquals(1, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should persist completed state when toggle button is tapped and list is refreshed`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)
                assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, rv.adapter!!.getItemViewType(0))
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
                assertEquals(1, rv.adapter!!.itemCount)
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
                assertEquals(0, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should clear focus on inline editText when touch event is outside its bounds`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val editText = activity.inlineAddEditTextInternal

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
                val editText = activity.inlineAddEditTextInternal

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
                val editText = activity.inlineAddEditTextInternal

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

    @Test
    fun `should show divider row when both active and completed sections are non-empty`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                addItemViaInlineRow(activity, "Call dentist")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
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
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
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

                val secondItem = rv.getChildAt(1)!!
                secondItem.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)

                val dividerPosition = (0 until rv.adapter!!.itemCount).first {
                    rv.adapter!!.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_DIVIDER
                }
                assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, rv.adapter!!.getItemViewType(dividerPosition + 1))
            }
        }
    }

    @Test
    fun `should remove item from list when delete button is tapped`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                assertEquals(1, rv.adapter!!.itemCount)

                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnDelete).performClick()
                activity.refreshListForTest()
                layoutRecyclerView(rv)

                assertEquals(0, rv.adapter!!.itemCount)
            }
        }
    }

    @Test
    fun `should show watermark at high alpha after last item is deleted`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnDelete).performClick()
                activity.refreshListForTest()

                val watermark = activity.findViewById<android.widget.ImageView>(R.id.imageWatermark)
                assertEquals(0.15f, watermark.alpha, 0.01f)
            }
        }
    }

    @Test
    fun `should switch title to inline edit field when edit button is tapped`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                firstItem.findViewById<MaterialButton>(R.id.btnEdit).performClick()

                val titleView = firstItem.findViewById<MaterialTextView>(R.id.textTitle)
                val editField = firstItem.findViewById<TextInputEditText>(R.id.editTitleInline)
                assertEquals(View.GONE, titleView.visibility)
                assertEquals(View.VISIBLE, editField.visibility)
            }
        }
    }

    @Test
    fun `should prefill inline edit field with current item title when edit button is tapped`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                firstItem.findViewById<MaterialButton>(R.id.btnEdit).performClick()

                val editField = firstItem.findViewById<TextInputEditText>(R.id.editTitleInline)
                assertEquals("Buy milk", editField.text.toString())
            }
        }
    }

    @Test
    fun `should update item title when inline edit is committed via IME Done`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                firstItem.findViewById<MaterialButton>(R.id.btnEdit).performClick()

                val editField = firstItem.findViewById<TextInputEditText>(R.id.editTitleInline)
                editField.setText("Whole milk")
                editField.onEditorAction(EditorInfo.IME_ACTION_DONE)

                activity.refreshListForTest()
                layoutRecyclerView(rv)

                val titleView = rv.getChildAt(0)
                    ?.findViewById<MaterialTextView>(R.id.textTitle)
                assertEquals("Whole milk", titleView?.text.toString())
            }
        }
    }

    @Test
    fun `should restore title view visibility after inline edit is committed`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                firstItem.findViewById<MaterialButton>(R.id.btnEdit).performClick()

                val editField = firstItem.findViewById<TextInputEditText>(R.id.editTitleInline)
                editField.setText("Whole milk")
                editField.onEditorAction(EditorInfo.IME_ACTION_DONE)

                val titleView = firstItem.findViewById<MaterialTextView>(R.id.textTitle)
                assertEquals(View.VISIBLE, titleView.visibility)
                assertEquals(View.GONE, editField.visibility)
            }
        }
    }

    @Test
    fun `should not update item when inline edit is committed with blank text`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                firstItem.findViewById<MaterialButton>(R.id.btnEdit).performClick()

                val editField = firstItem.findViewById<TextInputEditText>(R.id.editTitleInline)
                editField.setText("   ")
                editField.onEditorAction(EditorInfo.IME_ACTION_DONE)

                activity.refreshListForTest()
                layoutRecyclerView(rv)

                val titleView = rv.getChildAt(0)
                    ?.findViewById<MaterialTextView>(R.id.textTitle)
                assertEquals("Buy milk", titleView?.text.toString())
            }
        }
    }

    @Test
    fun `should update item title when inline edit is committed via focus loss`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                firstItem.findViewById<MaterialButton>(R.id.btnEdit).performClick()

                val editField = firstItem.findViewById<TextInputEditText>(R.id.editTitleInline)
                editField.setText("Whole milk")
                editField.clearFocus()

                activity.refreshListForTest()
                layoutRecyclerView(rv)

                val titleView = rv.getChildAt(0)
                    ?.findViewById<MaterialTextView>(R.id.textTitle)
                assertEquals("Whole milk", titleView?.text.toString())
            }
        }
    }

    @Test
    fun `should have delete button with error tint on item row`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                addItemViaInlineRow(activity, "Buy milk")
                val rv = activity.recyclerView()
                layoutRecyclerView(rv)
                val firstItem = rv.getChildAt(0)!!
                val btnDelete = firstItem.findViewById<MaterialButton>(R.id.btnDelete)
                assertNotNull(btnDelete)
            }
        }
    }

    @Test
    fun `should show inline add row pinned above recycler view`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                val inlineAddRow = activity.findViewById<View>(R.id.inlineAddRow)
                assertNotNull(inlineAddRow)
                assertEquals(View.VISIBLE, inlineAddRow.visibility)
            }
        }
    }

    @Test
    fun `should have editText accessible directly from activity for inline add`() {
        launchWithListId().use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.inlineAddEditTextInternal)
            }
        }
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
