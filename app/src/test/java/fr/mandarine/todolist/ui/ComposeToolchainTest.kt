package fr.mandarine.todolist.ui

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import fr.mandarine.todolist.MainThreadDatabaseRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposeToolchainTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val databaseRule = MainThreadDatabaseRule()

    @Test
    fun `should compose and expose a semantics tree under Robolectric`() {
        composeRule.setContent {
            Box(modifier = Modifier.testTag("probe")) {
                Text("composed")
            }
        }

        composeRule.onNodeWithTag("probe").assertIsDisplayed()
    }

    @Test
    fun `should compose inside a ComposeView hosted by a View-system activity`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = FrameLayout(activity)
                val composeView = ComposeView(activity).apply {
                    setContent { Text("interop") }
                }
                host.addView(composeView)
                activity.addContentView(
                    host,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                assertTrue(composeView.isAttachedToWindow)
            }
        }
    }
}
