package fr.mandarine.todolist.ui

import fr.mandarine.todolist.MainThreadDatabaseRule
import org.junit.Rule
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import android.view.ContextThemeWrapper
import com.google.android.material.button.MaterialButtonToggleGroup
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsDueDateTest {

    @get:Rule
    val databaseRule = MainThreadDatabaseRule()

    private lateinit var adapter: TodoListsAdapter
    private lateinit var themedContext: Context
    private lateinit var db: TodoDatabase

    @Before
    fun setUp() {
        adapter = TodoListsAdapter(
            onListClick = mockk(relaxed = true),
            onDeleteConfirmed = mockk(relaxed = true),
            onRenameClick = mockk(relaxed = true),
            onDragStart = mockk(relaxed = true)
        )
        themedContext = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ToDoList
        )
        db = databaseRule.database
    }

    @After
    fun tearDown() {
        db.clearAllTables()
    }

    private fun bindSummary(summary: TodoListSummary): TodoListsAdapter.ViewHolder {
        adapter.submitList(listOf(summary), emptyList())
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListsAdapter.VIEW_TYPE_ITEM)
            as TodoListsAdapter.ViewHolder
        adapter.onBindViewHolder(holder, 0)
        return holder
    }

    private fun summaryWithDueDate(
        dueDate: LocalDate?,
        status: DueDateStatus? = null,
        showYear: Boolean = false
    ) = TodoListSummary(
        list = TodoList("1", "Test", dueDate = dueDate),
        allDone = false,
        dueDateStatus = status,
        showDueDateYear = showYear
    )

    private fun resolveThemeColor(attr: Int): Int {
        val tv = TypedValue()
        themedContext.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    // ── Adapter: row-level display ────────────────────────────────────────────

    @Test
    fun `should hide due date row when dueDate is null`() {
        val holder = bindSummary(summaryWithDueDate(null))
        val dueDateLayout = holder.itemView.findViewById<View>(R.id.layoutDueDate)
        assertEquals(View.GONE, dueDateLayout.visibility)
    }

    @Test
    fun `should show due date row when dueDate is set`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.of(2099, 6, 22), DueDateStatus.FUTURE)
        )
        val dueDateLayout = holder.itemView.findViewById<View>(R.id.layoutDueDate)
        assertEquals(View.VISIBLE, dueDateLayout.visibility)
    }

    @Test
    fun `should display non-empty formatted text when dueDate is set`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.of(2099, 6, 22), DueDateStatus.FUTURE)
        )
        val dateText = holder.itemView.findViewById<MaterialTextView>(R.id.textDueDate)
        assertTrue("Due date text should not be empty", dateText.text.isNotEmpty())
    }

    @Test
    fun `should apply colorPrimary tint to due date icon when status is FUTURE`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.of(2099, 6, 22), DueDateStatus.FUTURE)
        )
        val icon = holder.itemView.findViewById<ImageView>(R.id.iconDueDate)
        val expected = resolveThemeColor(com.google.android.material.R.attr.colorPrimary)
        assertEquals(expected, icon.imageTintList?.defaultColor)
    }

    @Test
    fun `should apply colorPrimary text color when status is FUTURE`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.of(2099, 6, 22), DueDateStatus.FUTURE)
        )
        val text = holder.itemView.findViewById<MaterialTextView>(R.id.textDueDate)
        val expected = resolveThemeColor(com.google.android.material.R.attr.colorPrimary)
        assertEquals(expected, text.currentTextColor)
    }

    @Test
    fun `should apply colorWarning tint to due date icon when status is TODAY`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.now(), DueDateStatus.TODAY)
        )
        val icon = holder.itemView.findViewById<ImageView>(R.id.iconDueDate)
        val expected = resolveThemeColor(R.attr.colorWarning)
        assertEquals(expected, icon.imageTintList?.defaultColor)
    }

    @Test
    fun `should apply colorWarning text color when status is TODAY`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.now(), DueDateStatus.TODAY)
        )
        val text = holder.itemView.findViewById<MaterialTextView>(R.id.textDueDate)
        val expected = resolveThemeColor(R.attr.colorWarning)
        assertEquals(expected, text.currentTextColor)
    }

    @Test
    fun `should apply colorError tint to due date icon when status is OVERDUE`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.of(2020, 1, 1), DueDateStatus.OVERDUE)
        )
        val icon = holder.itemView.findViewById<ImageView>(R.id.iconDueDate)
        val expected = resolveThemeColor(com.google.android.material.R.attr.colorError)
        assertEquals(expected, icon.imageTintList?.defaultColor)
    }

    @Test
    fun `should apply colorError text color when status is OVERDUE`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.of(2020, 1, 1), DueDateStatus.OVERDUE)
        )
        val text = holder.itemView.findViewById<MaterialTextView>(R.id.textDueDate)
        val expected = resolveThemeColor(com.google.android.material.R.attr.colorError)
        assertEquals(expected, text.currentTextColor)
    }

    @Test
    fun `should include year in due date text when showDueDateYear is true`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.of(2027, 6, 22), DueDateStatus.FUTURE, showYear = true)
        )
        val dateText = holder.itemView.findViewById<MaterialTextView>(R.id.textDueDate)
        assertTrue("Year 2027 should appear in due date text", dateText.text.contains("2027"))
    }

    @Test
    fun `should not include year in due date text when showDueDateYear is false`() {
        val holder = bindSummary(
            summaryWithDueDate(LocalDate.of(2027, 6, 22), DueDateStatus.FUTURE, showYear = false)
        )
        val dateText = holder.itemView.findViewById<MaterialTextView>(R.id.textDueDate)
        assertTrue("Year should be absent when showDueDateYear is false",
            !dateText.text.contains("2027"))
    }

    @Test
    fun `target date should never receive colorError tint even when elapsed`() {
        val summary = TodoListSummary(
            list = TodoList("1", "Test", targetDate = LocalDate.of(2020, 1, 1)),
            allDone = false,
            isTargetDateElapsed = true,
            showTargetYear = false
        )
        val holder = bindSummary(summary)
        val icon = holder.itemView.findViewById<ImageView>(R.id.iconTargetDate)
        val colorError = resolveThemeColor(com.google.android.material.R.attr.colorError)
        assertTrue(
            "Target date icon must never use colorError tint",
            icon.imageTintList?.defaultColor != colorError
        )
    }

    // ── Inline add row ────────────────────────────────────────────────────────

    @Test
    fun `should have alarm button in inline create row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                val dueDateButton = activity.inlineAddRowInternal
                    .findViewById<MaterialButton>(R.id.btnListInlineDueDate)
                assertNotNull("Alarm button must exist in inline create row", dueDateButton)
                assertEquals(View.VISIBLE, dueDateButton.visibility)
            }
        }
    }

    @Test
    fun `should show due date row in list when list is created with a due date via inline row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val dueDate = LocalDate.of(2099, 12, 25)
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.setInlineDueDateForTest(dueDate)
                activity.typeInInlineRowForTest("Holiday")
                activity.submitInlineRowForTest()
            }
            scenario.onActivity { activity ->
                assertEquals(1, activity.recyclerViewInternal.adapter!!.itemCount)
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val dueDateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutDueDate)
                assertEquals(View.VISIBLE, dueDateLayout.visibility)
            }
        }
    }

    @Test
    fun `should hide target date row when due date is set instead via inline row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val dueDate = LocalDate.of(2099, 12, 25)
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.setInlineDueDateForTest(dueDate)
                activity.typeInInlineRowForTest("Holiday")
                activity.submitInlineRowForTest()
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val targetDateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutTargetDate)
                assertEquals(View.GONE, targetDateLayout.visibility)
            }
        }
    }

    @Test
    fun `setting inline due date should clear inline target date selection`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.setInlineDueDateForTest(LocalDate.of(2099, 6, 1))
                assertEquals(null, activity.selectedInlineDate)
            }
        }
    }

    @Test
    fun `setting inline target date should clear inline due date selection`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.setInlineDueDateForTest(LocalDate.of(2099, 6, 1))
                activity.setInlineDateForTest(LocalDate.of(2099, 7, 1))
                assertEquals(null, activity.selectedInlineDueDate)
            }
        }
    }

    // ── Rename dialog ─────────────────────────────────────────────────────────

    @Test
    fun `should have alarm toggle button in rename dialog`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val dueBtn = activity.currentDialogView
                    ?.findViewById<MaterialButton>(R.id.btnToggleDueDate)
                assertNotNull("Alarm toggle button must exist in rename dialog", dueBtn)
                assertEquals(View.VISIBLE, dueBtn?.visibility)
            }
        }
    }

    @Test
    fun `should show due date row in list after setting due date via rename dialog`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.setRenameDueDateForTest(LocalDate.of(2099, 8, 20))
                activity.confirmDialogForTest()
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val dueDateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutDueDate)
                assertEquals(View.VISIBLE, dueDateLayout.visibility)
            }
        }
    }

    @Test
    fun `should check alarm toggle button when list has a due date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val toggle = activity.currentDialogView
                    ?.findViewById<MaterialButtonToggleGroup>(R.id.toggleDateKind)
                assertEquals(R.id.btnToggleDueDate, toggle?.checkedButtonId)
            }
        }
    }

    @Test
    fun `should show clear button in rename dialog when due date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val clearButton = activity.currentDialogView
                    ?.findViewById<MaterialButton>(R.id.btnDialogClearDate)
                assertEquals(View.VISIBLE, clearButton?.visibility)
            }
        }
    }

    @Test
    fun `should hide clear button in rename dialog when no date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val clearButton = activity.currentDialogView
                    ?.findViewById<MaterialButton>(R.id.btnDialogClearDate)
                assertEquals(View.GONE, clearButton?.visibility)
            }
        }
    }

    @Test
    fun `should show date text in rename dialog when due date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val dateText = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDialogDate)
                assertEquals(View.VISIBLE, dateText?.visibility)
                assertTrue("Date text should be non-empty", dateText?.text?.isNotEmpty() == true)
            }
        }
    }

    @Test
    fun `should hide date text in rename dialog when no date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val dateText = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDialogDate)
                assertEquals(View.GONE, dateText?.visibility)
            }
        }
    }

    @Test
    fun `should clear due date row when clear button is tapped in rename dialog`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clearRenameDateForTest()
                activity.confirmDialogForTest()
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val dueDateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutDueDate)
                assertEquals(View.GONE, dueDateLayout.visibility)
            }
        }
    }

    @Test
    fun `should clear target date when due date is set in rename dialog`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.setRenameDueDateForTest(LocalDate.of(2099, 8, 20))
                assertEquals(null, activity.selectedRenameDate)
            }
        }
    }

    @Test
    fun `should clear due date when target date is set in rename dialog`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.setRenameTargetDateForTest(LocalDate.of(2099, 8, 20))
                assertEquals(null, activity.selectedRenameDueDate)
            }
        }
    }

    @Test
    fun `should show due date row when list is created with a due date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val dueDate = LocalDate.of(2099, 12, 25)
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Holiday", dueDate)
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val dueDateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutDueDate)
                assertEquals(View.VISIBLE, dueDateLayout.visibility)
            }
        }
    }

    @Test
    fun `should hide due date row when list has no due date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("NoDate", null)
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val dueDateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutDueDate)
                assertEquals(View.GONE, dueDateLayout.visibility)
            }
        }
    }

    // ── Decoupled mode / picker behaviour ────────────────────────────────────

    @Test
    fun `switching mode without a date neither creates nor clears a date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clickToggleDueDateForTest()
                assertEquals(null, activity.selectedRenameDate)
                assertEquals(null, activity.selectedRenameDueDate)
            }
        }
    }

    @Test
    fun `switching mode from target to due carries the existing target date to due`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val originalDate = LocalDate.of(2099, 6, 1)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", originalDate)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clickToggleDueDateForTest()
                assertEquals(null, activity.selectedRenameDate)
                assertEquals(originalDate, activity.selectedRenameDueDate)
            }
        }
    }

    @Test
    fun `switching mode from due to target carries the existing due date to target`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val originalDate = LocalDate.of(2099, 6, 1)
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", originalDate)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clickToggleTargetDateForTest()
                assertEquals(originalDate, activity.selectedRenameDate)
                assertEquals(null, activity.selectedRenameDueDate)
            }
        }
    }

    @Test
    fun `clearing a due date leaves the alarm toggle button checked`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clearRenameDateForTest()
                assertEquals(null, activity.selectedRenameDueDate)
                val toggle = activity.currentDialogView
                    ?.findViewById<MaterialButtonToggleGroup>(R.id.toggleDateKind)
                assertEquals(R.id.btnToggleDueDate, toggle?.checkedButtonId)
            }
        }
    }

    @Test
    fun `clearing a target date leaves the calendar toggle button checked`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 6, 1)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", targetDate)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clearRenameDateForTest()
                assertEquals(null, activity.selectedRenameDate)
                val toggle = activity.currentDialogView
                    ?.findViewById<MaterialButtonToggleGroup>(R.id.toggleDateKind)
                assertEquals(R.id.btnToggleTargetDate, toggle?.checkedButtonId)
            }
        }
    }

    @Test
    fun `default mode is calendar toggle when opening dialog on list with no date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val toggle = activity.currentDialogView
                    ?.findViewById<MaterialButtonToggleGroup>(R.id.toggleDateKind)
                assertEquals(R.id.btnToggleTargetDate, toggle?.checkedButtonId)
            }
        }
    }

    private fun tapEditButtonOnFirstRow(scenario: ActivityScenario<TodoListsActivity>) {
        scenario.onActivity { activity ->
            val rv = activity.recyclerViewInternal
            rv.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
)
            rv.layout(0, 0, 1080, 1920)
            rv.getChildAt(0)!!.findViewById<MaterialButton>(R.id.btnEditList).performClick()
        }
    }
}
