package fr.mandarine.todolist.ui

import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.MainThreadDatabaseRule
import fr.mandarine.todolist.R
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.data.SharedPreferencesTutorialStateRepository
import fr.mandarine.todolist.presentation.TutorialUiState
import io.mockk.mockk
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
    fun `should reattach overlay on replay when a previous run detached it`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val decor = activity.window.decorView as ViewGroup
                fun overlayCount() = (0 until decor.childCount)
                    .count { decor.getChildAt(it).id == R.id.tutorialOverlay }
                val baseline = overlayCount()

                val controller = TutorialOverlayController(
                    mockk(relaxed = true),
                    activity.lifecycleScope
                )
                controller.attachToActivity(activity)
                assertEquals(baseline + 1, overlayCount())

                controller.detachFromActivity()
                assertEquals(baseline, overlayCount())

                controller.handleState(TutorialUiState.ReadyToStart, activity)
                assertEquals(baseline + 1, overlayCount())
            }
        }
    }
}
