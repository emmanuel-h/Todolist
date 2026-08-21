package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaperPreviewsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should render the paper surface preview`() {
        composeRule.setContent { PaperSurfacePreview() }

        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `should render the ruled row preview with both row heights and both badges`() {
        composeRule.setContent { RuledRowPreview() }

        composeRule.onNodeWithText("🍎 Apples").assertIsDisplayed()
        composeRule.onNodeWithText("🛒 Groceries").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertIsDisplayed()
    }

    @Test
    fun `should render the ghost row preview in both heights`() {
        composeRule.setContent { GhostRowPreview() }

        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `should render the count badge preview from zero upwards`() {
        composeRule.setContent { CountBadgePreview() }

        composeRule.onNodeWithText("0").assertIsDisplayed()
        composeRule.onNodeWithText("7").assertIsDisplayed()
        composeRule.onNodeWithText("128").assertIsDisplayed()
    }

    @Test
    fun `should render the ink icon preview with an enabled and a disabled button`() {
        composeRule.setContent { InkIconPreview() }

        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `should render the sticky note pad preview both full and taken`() {
        composeRule.setContent { StickyNotePadPreview() }

        composeRule.onNodeWithContentDescription("Create new list").assertIsDisplayed()
    }
}
