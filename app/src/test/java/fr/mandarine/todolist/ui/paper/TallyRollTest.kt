package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TallyRollTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var count = mutableIntStateOf(2)

    @Test
    fun `should come to rest on the tally it rolled to`() {
        render()

        count.intValue = 3
        composeRule.waitForIdle()

        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertDoesNotExist()
    }

    @Test
    fun `should come to rest on the tally it rolled back to`() {
        render()

        count.intValue = 1
        composeRule.waitForIdle()

        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertDoesNotExist()
    }

    @Test
    fun `should leave the tally standing when nothing has counted`() {
        render(animated = false)

        count.intValue = 5
        composeRule.waitForIdle()

        composeRule.onNodeWithText("5").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertDoesNotExist()
    }

    private fun render(animated: Boolean = true) {
        composeRule.setContent {
            PaperTheme {
                val tally by remember { count }
                SectionSkip(completedCount = tally, animated = animated)
            }
        }
    }
}
