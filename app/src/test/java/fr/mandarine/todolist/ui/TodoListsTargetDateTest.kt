package fr.mandarine.todolist.ui

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsTargetDateTest {

    private lateinit var adapter: TodoListsAdapter
    private lateinit var themedContext: Context
    private lateinit var db: TodoDatabase

    @Before
    fun setUp() {
        adapter = TodoListsAdapter(
            onListClick = mockk(relaxed = true),
            onDeleteClick = mockk(relaxed = true),
            onRenameClick = mockk(relaxed = true),
            onDragStart = mockk(relaxed = true)
        )
        themedContext = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ToDoList
        )
        db = TodoDatabase.getInstance(ApplicationProvider.getApplicationContext())
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

    private fun summaryWithDate(date: LocalDate?, elapsed: Boolean = false, showYear: Boolean = false) =
        TodoListSummary(
            list = TodoList("1", "Test", targetDate = date),
            allDone = false,
            isTargetDateElapsed = elapsed,
            showTargetYear = showYear
        )

    private fun resolveThemeColor(attr: Int): Int {
        val tv = TypedValue()
        themedContext.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    @Test
    fun `should hide date line when targetDate is null`() {
        val holder = bindSummary(summaryWithDate(null))
        val dateLayout = holder.itemView.findViewById<View>(R.id.layoutTargetDate)
        assertEquals(View.GONE, dateLayout.visibility)
    }

    @Test
    fun `should show date line when targetDate is set`() {
        val holder = bindSummary(summaryWithDate(LocalDate.of(2026, 6, 22)))
        val dateLayout = holder.itemView.findViewById<View>(R.id.layoutTargetDate)
        assertEquals(View.VISIBLE, dateLayout.visibility)
    }

    @Test
    fun `should display non-empty formatted text when targetDate is set`() {
        val holder = bindSummary(summaryWithDate(LocalDate.of(2026, 6, 22)))
        val dateText = holder.itemView.findViewById<MaterialTextView>(R.id.textTargetDate)
        assertTrue("Date text should not be empty", dateText.text.isNotEmpty())
    }

    @Test
    fun `should apply colorPrimary tint to icon when date is not elapsed`() {
        val holder = bindSummary(summaryWithDate(LocalDate.of(2099, 6, 22), elapsed = false))
        val icon = holder.itemView.findViewById<android.widget.ImageView>(R.id.iconTargetDate)
        val expected = resolveThemeColor(com.google.android.material.R.attr.colorPrimary)
        assertEquals(expected, icon.imageTintList?.defaultColor)
    }

    @Test
    fun `should apply colorOnSurfaceVariant tint to icon when date is elapsed`() {
        val holder = bindSummary(summaryWithDate(LocalDate.of(2020, 1, 1), elapsed = true))
        val icon = holder.itemView.findViewById<android.widget.ImageView>(R.id.iconTargetDate)
        val expected = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        assertEquals(expected, icon.imageTintList?.defaultColor)
    }

    @Test
    fun `should apply colorPrimary text color when date is not elapsed`() {
        val holder = bindSummary(summaryWithDate(LocalDate.of(2099, 6, 22), elapsed = false))
        val dateText = holder.itemView.findViewById<MaterialTextView>(R.id.textTargetDate)
        val expected = resolveThemeColor(com.google.android.material.R.attr.colorPrimary)
        assertEquals(expected, dateText.currentTextColor)
    }

    @Test
    fun `should apply colorOnSurfaceVariant text color when date is elapsed`() {
        val holder = bindSummary(summaryWithDate(LocalDate.of(2020, 1, 1), elapsed = true))
        val dateText = holder.itemView.findViewById<MaterialTextView>(R.id.textTargetDate)
        val expected = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        assertEquals(expected, dateText.currentTextColor)
    }

    @Test
    fun `should include year in formatted date when showTargetYear is true`() {
        val holder = bindSummary(summaryWithDate(LocalDate.of(2027, 6, 22), showYear = true))
        val dateText = holder.itemView.findViewById<MaterialTextView>(R.id.textTargetDate)
        assertTrue("Year 2027 should appear in date text", dateText.text.contains("2027"))
    }

    @Test
    fun `should not include year in formatted date when showTargetYear is false`() {
        val holder = bindSummary(summaryWithDate(LocalDate.of(2027, 6, 22), showYear = false))
        val dateText = holder.itemView.findViewById<MaterialTextView>(R.id.textTargetDate)
        assertFalse("Year should be absent when showTargetYear is false", dateText.text.contains("2027"))
    }

    @Test
    fun `should format date with en-GB locale using ICU skeleton`() {
        val date = LocalDate.of(2025, 6, 22)
        val result = TodoListsAdapter.formatTargetDate(date, showYear = false, Locale.UK)
        assertTrue(
            "en-GB June should appear as 'Jun' but was: $result",
            result.contains("Jun") || result.contains("June")
        )
        assertFalse("Year should be absent when showYear=false but was: $result", result.contains("2025"))
    }

    @Test
    fun `should include year in en-GB formatted date when showYear is true`() {
        val date = LocalDate.of(2027, 6, 22)
        val result = TodoListsAdapter.formatTargetDate(date, showYear = true, Locale.UK)
        assertTrue("Year 2027 must appear in output but was: $result", result.contains("2027"))
        assertTrue(
            "en-GB June should appear as 'Jun' but was: $result",
            result.contains("Jun") || result.contains("June")
        )
    }

    @Test
    fun `should format date with German locale using ICU skeleton`() {
        val date = LocalDate.of(2025, 6, 22)
        val result = TodoListsAdapter.formatTargetDate(date, showYear = false, Locale.forLanguageTag("de-DE"))
        assertTrue(
            "German June should contain 'Jun' but was: $result",
            result.lowercase().contains("jun")
        )
    }

    @Test
    fun `should format date with French locale using ICU skeleton`() {
        val date = LocalDate.of(2025, 6, 22)
        val result = TodoListsAdapter.formatTargetDate(date, showYear = false, Locale.FRANCE)
        assertTrue(
            "French June should be 'juin' but was: $result",
            result.lowercase().contains("juin")
        )
    }

    @Test
    fun `should not use hardcoded English field order for German locale`() {
        val date = LocalDate.of(2025, 6, 22)
        val enResult = TodoListsAdapter.formatTargetDate(date, showYear = false, Locale.UK)
        val deResult = TodoListsAdapter.formatTargetDate(date, showYear = false, Locale.forLanguageTag("de-DE"))
        assertFalse(
            "German pattern should differ from English pattern (ICU localizes field order)",
            enResult == deResult
        )
    }

    @Test
    fun `should have calendar button in inline create row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.tapFab()
                val dateButton = activity.inlineAddRowInternal
                    .findViewById<MaterialButton>(R.id.btnListInlineDate)
                assertNotNull("Calendar button must exist in inline create row", dateButton)
                assertEquals(View.VISIBLE, dateButton.visibility)
            }
        }
    }

    @Test
    fun `should show date line in row when list is created with a target date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 12, 25)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Holiday", targetDate)
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val row = rv.getChildAt(0)!!
                val dateLayout = row.findViewById<View>(R.id.layoutTargetDate)
                assertEquals(View.VISIBLE, dateLayout.visibility)
            }
        }
    }

    @Test
    fun `should hide date line in row when list has no target date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("NoDate", null)
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val row = rv.getChildAt(0)!!
                val dateLayout = row.findViewById<View>(R.id.layoutTargetDate)
                assertEquals(View.GONE, dateLayout.visibility)
            }
        }
    }

    @Test
    fun `should create list with target date when inline date is set before submit`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 3, 15)
            scenario.onActivity { activity ->
                activity.tapFab()
                activity.setInlineDateForTest(targetDate)
                activity.typeInInlineRowForTest("Sprint")
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
                val dateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutTargetDate)
                assertEquals(View.VISIBLE, dateLayout.visibility)
            }
        }
    }

    @Test
    fun `should clear target date in rename dialog via clear button`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 3, 15)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", targetDate)
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
                val dateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutTargetDate)
                assertEquals(View.GONE, dateLayout.visibility)
            }
        }
    }

    @Test
    fun `should set target date in rename dialog and show date line in row`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            val newDate = LocalDate.of(2099, 8, 20)
            scenario.onActivity { activity ->
                activity.setRenameTargetDateForTest(newDate)
                activity.confirmDialogForTest()
            }
            scenario.onActivity { activity ->
                val rv = activity.recyclerViewInternal
                rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                rv.layout(0, 0, 1080, 1920)
                val dateLayout = rv.getChildAt(0)!!.findViewById<View>(R.id.layoutTargetDate)
                assertEquals(View.VISIBLE, dateLayout.visibility)
            }
        }
    }

    @Test
    fun `should show clear button in rename dialog when date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 3, 15)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", targetDate)
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
                activity.createListWithDateForTest("Work", null)
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
    fun `should display formatted date text in rename dialog when date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 3, 15)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", targetDate)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val dateText = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDialogTargetDate)
                assertTrue(
                    "Dialog date text should be non-empty when date is set",
                    dateText?.text?.isNotEmpty() == true
                )
            }
        }
    }

    @Test
    fun `should show add icon in dialog date row when no date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val addIcon = activity.currentDialogView
                    ?.findViewById<android.widget.ImageView>(R.id.iconDateAddAffordance)
                assertEquals(View.VISIBLE, addIcon?.visibility)
            }
        }
    }

    @Test
    fun `should hide add icon in dialog date row when date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 3, 15)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", targetDate)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val addIcon = activity.currentDialogView
                    ?.findViewById<android.widget.ImageView>(R.id.iconDateAddAffordance)
                assertEquals(View.GONE, addIcon?.visibility)
            }
        }
    }

    @Test
    fun `should show calendar icon in dialog date row even when no date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val calendarIcon = activity.currentDialogView
                    ?.findViewById<android.widget.ImageView>(R.id.iconDialogTargetDate)
                assertEquals(View.VISIBLE, calendarIcon?.visibility)
            }
        }
    }

    @Test
    fun `should show calendar icon in dialog date row when date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 3, 15)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", targetDate)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val calendarIcon = activity.currentDialogView
                    ?.findViewById<android.widget.ImageView>(R.id.iconDialogTargetDate)
                assertEquals(View.VISIBLE, calendarIcon?.visibility)
            }
        }
    }

    @Test
    fun `should hide date text in dialog date row when no date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val dateText = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDialogTargetDate)
                assertEquals(View.GONE, dateText?.visibility)
            }
        }
    }

    @Test
    fun `should show date text in dialog date row when date is set`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            val targetDate = LocalDate.of(2099, 3, 15)
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", targetDate)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val dateText = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDialogTargetDate)
                assertEquals(View.VISIBLE, dateText?.visibility)
            }
        }
    }

    @Test
    fun `should keep calendar icon visible and toggle add icon when date is set in rename dialog`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val calendarIcon = activity.currentDialogView
                    ?.findViewById<android.widget.ImageView>(R.id.iconDialogTargetDate)
                val addIconBefore = activity.currentDialogView
                    ?.findViewById<android.widget.ImageView>(R.id.iconDateAddAffordance)
                assertEquals("Calendar always visible before date set", View.VISIBLE, calendarIcon?.visibility)
                assertEquals("Add icon visible before date set", View.VISIBLE, addIconBefore?.visibility)
                activity.setRenameTargetDateForTest(LocalDate.of(2099, 8, 20))
                val addIconAfter = activity.currentDialogView
                    ?.findViewById<android.widget.ImageView>(R.id.iconDateAddAffordance)
                assertEquals("Calendar always visible after date set", View.VISIBLE, calendarIcon?.visibility)
                assertEquals("Add icon hidden after date set", View.GONE, addIconAfter?.visibility)
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
