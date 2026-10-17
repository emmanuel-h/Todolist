package fr.mandarine.todolist.ui.todolists

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.paper.PaperTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListActionsSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recoloured = mutableListOf<ListColour>()
    private var dismissed = 0
    private val colour = mutableStateOf(ListColour.None)

    @Test
    fun `should show all seven swatches on the sheet`() {
        render(ListColour.None)

        listOf(COLOUR_NONE, COLOUR_BUTTER, COLOUR_MINT, COLOUR_ROSE, COLOUR_SKY, COLOUR_PEACH, COLOUR_LILAC)
            .forEach { composeRule.onNodeWithContentDescription(it).assertIsDisplayed() }
    }

    @Test
    fun `should circle the list's current colour when the sheet opens`() {
        render(ListColour.Mint)

        composeRule.onNodeWithContentDescription(COLOUR_MINT).assertIsSelected()
        composeRule.onNodeWithContentDescription(COLOUR_BUTTER).assertIsNotSelected()
        composeRule.onNodeWithContentDescription(COLOUR_NONE).assertIsNotSelected()
    }

    @Test
    fun `should circle the None swatch when the list has no colour`() {
        render(ListColour.None)

        composeRule.onNodeWithContentDescription(COLOUR_NONE).assertIsSelected()
    }

    @Test
    fun `should recolour the list when a swatch is pressed`() {
        render(ListColour.None)

        composeRule.onNodeWithContentDescription(COLOUR_SKY).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(ListColour.Sky), recoloured)
    }

    @Test
    fun `should keep the sheet out and move the circle when a swatch is pressed`() {
        render(ListColour.None)

        composeRule.onNodeWithContentDescription(COLOUR_BUTTER).performClick()
        composeRule.waitForIdle()

        assertEquals(0, dismissed)
        composeRule.onNodeWithContentDescription(COLOUR_BUTTER).assertIsSelected()
        composeRule.onNodeWithContentDescription(COLOUR_NONE).assertIsNotSelected()
    }

    private fun render(initial: ListColour) {
        colour.value = initial
        composeRule.setContent {
            PaperTheme {
                ListActionsSheet(
                    summary = TodoListSummary(
                        TodoList("list-1", "Groceries", colour = colour.value),
                        allDone = false
                    ),
                    onDismiss = { dismissed += 1 },
                    onRename = {},
                    onPickDate = {},
                    onDelete = {},
                    onRecolour = {
                        recoloured += it
                        colour.value = it
                    },
                    animated = false
                )
            }
        }
    }

    private companion object {
        const val COLOUR_NONE = "No colour"
        const val COLOUR_BUTTER = "Yellow"
        const val COLOUR_MINT = "Green"
        const val COLOUR_ROSE = "Pink"
        const val COLOUR_SKY = "Blue"
        const val COLOUR_PEACH = "Orange"
        const val COLOUR_LILAC = "Violet"
    }
}
