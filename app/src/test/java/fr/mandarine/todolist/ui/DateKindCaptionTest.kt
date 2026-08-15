package fr.mandarine.todolist.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.MainThreadDatabaseRule
import fr.mandarine.todolist.R
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.data.SharedPreferencesTutorialStateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DateKindCaptionTest {

    @get:Rule
    val databaseRule = MainThreadDatabaseRule()

    private lateinit var themedContext: Context

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<TodoListApplication>()
        SharedPreferencesTutorialStateRepository(app).markTutorialSeen()
        themedContext = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_ToDoList)
    }

    // ── Dialog caption: initial state ─────────────────────────────────────────

    @Test
    fun `should show target caption when dialog opens with no date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val caption = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDateKindCaption)
                assertNotNull("textDateKindCaption must exist in dialog", caption)
                val expected = activity.getString(R.string.date_kind_target_caption)
                assertEquals(expected, caption?.text?.toString())
            }
        }
    }

    @Test
    fun `should show due caption when dialog opens on a list with a due date`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                val caption = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDateKindCaption)
                val expected = activity.getString(R.string.date_kind_due_caption)
                assertEquals(expected, caption?.text?.toString())
            }
        }
    }

    // ── Dialog caption: toggling direction ────────────────────────────────────

    @Test
    fun `should switch to due caption when alarm toggle is tapped`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clickToggleDueDateForTest()
                val caption = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDateKindCaption)
                val expected = activity.getString(R.string.date_kind_due_caption)
                assertEquals(expected, caption?.text?.toString())
            }
        }
    }

    @Test
    fun `should switch back to target caption when calendar toggle is tapped after due`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", null)
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clickToggleDueDateForTest()
                activity.clickToggleTargetDateForTest()
                val caption = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDateKindCaption)
                val expected = activity.getString(R.string.date_kind_target_caption)
                assertEquals(expected, caption?.text?.toString())
            }
        }
    }

    @Test
    fun `should show due caption after date carries across from target to due toggle`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDateForTest("Work", LocalDate.of(2099, 3, 10))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clickToggleDueDateForTest()
                val caption = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDateKindCaption)
                val expected = activity.getString(R.string.date_kind_due_caption)
                assertEquals(expected, caption?.text?.toString())
            }
        }
    }

    @Test
    fun `should show target caption after date carries across from due to target toggle`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.createListWithDueDateForTest("Work", LocalDate.of(2099, 6, 1))
            }
            tapEditButtonOnFirstRow(scenario)
            scenario.onActivity { activity ->
                activity.clickToggleTargetDateForTest()
                val caption = activity.currentDialogView
                    ?.findViewById<MaterialTextView>(R.id.textDateKindCaption)
                val expected = activity.getString(R.string.date_kind_target_caption)
                assertEquals(expected, caption?.text?.toString())
            }
        }
    }

    // ── Tutorial overlay: caption pill structure (tested via direct inflation) ──

    @Test
    fun `should have tutorialCaptionPill hidden by default in overlay layout`() {
        val parent = FrameLayout(themedContext)
        val overlayView = LayoutInflater.from(themedContext)
            .inflate(R.layout.overlay_tutorial, parent, false)
        val captionPill = overlayView.findViewById<View>(R.id.tutorialCaptionPill)
        assertNotNull("tutorialCaptionPill must exist in overlay_tutorial.xml", captionPill)
        assertEquals(
            "tutorialCaptionPill must be invisible by default",
            View.INVISIBLE,
            captionPill.visibility
        )
    }

    @Test
    fun `should have tutorialCaptionText in overlay layout`() {
        val parent = FrameLayout(themedContext)
        val overlayView = LayoutInflater.from(themedContext)
            .inflate(R.layout.overlay_tutorial, parent, false)
        val captionText = overlayView.findViewById<View>(R.id.tutorialCaptionText)
        assertNotNull("tutorialCaptionText must exist in overlay_tutorial.xml", captionText)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
