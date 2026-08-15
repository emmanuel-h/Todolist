package fr.mandarine.todolist.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.mandarine.todolist.MainThreadDatabaseRule
import fr.mandarine.todolist.R
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.data.SharedPreferencesTutorialStateRepository
import fr.mandarine.todolist.presentation.TutorialUiState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialReplayUiTest {

    @get:Rule
    val databaseRule = MainThreadDatabaseRule()

    @Before
    fun markTutorialSeen() {
        val app = ApplicationProvider.getApplicationContext<TodoListApplication>()
        SharedPreferencesTutorialStateRepository(app).markTutorialSeen()
    }

    @Test
    fun `should show replay button when tutorial is dismissed`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val replay = activity.findViewById<MaterialButton>(R.id.btnReplayTutorial)
                assertEquals(View.VISIBLE, replay.visibility)
            }
        }
    }

    @Test
    fun `should hide replay button while inline add row is open and restore it on cancel`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val replay = activity.findViewById<MaterialButton>(R.id.btnReplayTutorial)
                activity.findViewById<FloatingActionButton>(R.id.fabAddList).performClick()
                assertEquals(View.GONE, replay.visibility)

                activity.inlineAddRowInternal
                    .findViewById<MaterialButton>(R.id.btnListInlineCancel)
                    .performClick()
                assertEquals(View.VISIBLE, replay.visibility)
            }
        }
    }

    @Test
    fun `should restart tutorial when replay button is tapped after dismissal`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val container =
                    ApplicationProvider.getApplicationContext<TodoListApplication>().container
                assertEquals(
                    TutorialUiState.Dismissed,
                    container.tutorialViewModel.uiState.value
                )

                activity.findViewById<MaterialButton>(R.id.btnReplayTutorial).performClick()

                assertEquals(
                    TutorialUiState.ReadyToStart,
                    container.tutorialViewModel.uiState.value
                )
            }
        }
    }
}
