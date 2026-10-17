package fr.mandarine.todolist.ui.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.ListHorizon
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.PaperTheme
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1200dp")
class CalendarPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val turned = mutableListOf<YearMonth>()
    private val opened = mutableListOf<String>()

    @Test
    fun `should mark each day a list falls on in the ink its day is written in`() {
        val marks = marksIn(
            listOf(late("a", TODAY.minusDays(2)), due("b", TODAY, DueDateStatus.TODAY), target("c", TODAY.plusDays(3))),
            YearMonth.from(TODAY)
        )

        assertEquals(
            mapOf(15 to InkTone.Lost, 17 to InkTone.Today, 20 to InkTone.Margin),
            marks
        )
    }

    @Test
    fun `should mark a day several lists share in the loudest of their inks`() {
        val marks = marksIn(
            listOf(target("a", TODAY), due("b", TODAY, DueDateStatus.TODAY)),
            YearMonth.from(TODAY)
        )

        assertEquals(mapOf(17 to InkTone.Today), marks)
    }

    @Test
    fun `should mark no day outside the month being shown`() {
        assertEquals(emptyMap<Int, InkTone>(), marksIn(listOf(target("a", TODAY.plusMonths(1))), YearMonth.from(TODAY)))
    }

    @Test
    fun `should turn the month forwards and back`() {
        render(content(target("a", TODAY.plusDays(3))))

        composeRule.onNodeWithContentDescription("Next month").performClick()
        composeRule.onNodeWithContentDescription("Previous month").performClick()

        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 9)), turned)
    }

    @Test
    fun `should say what kind of day each dated list carries`() {
        render(content(target("a", TODAY.plusDays(3)), late("b", TODAY.minusDays(2))))

        composeRule.onNodeWithText("To do on this day", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Finish before this day", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("LATE · THU, OCT 15").assertIsDisplayed()
    }

    @Test
    fun `should say the lists with no day stay on the board`() {
        render(content(target("a", TODAY.plusDays(3)), undated("b"), undated("c")))

        composeRule.onNodeWithText("2 lists have no date. They stay in Lists.").assertIsDisplayed()
    }

    @Test
    fun `should say so when no list has a day`() {
        render(content(undated("b")))

        composeRule.onNodeWithText("No list has a day yet.").assertIsDisplayed()
    }

    @Test
    fun `should open the list a slip is for`() {
        render(content(target("a", TODAY.plusDays(3))))

        composeRule.onNodeWithText("a").performClick()

        assertEquals(listOf("a"), opened)
    }

    private fun render(state: TodoListsState) {
        composeRule.setContent {
            PaperTheme {
                CalendarPage(
                    state = state,
                    today = TODAY,
                    month = null,
                    onMonthTurned = { turned += it },
                    onOpenList = { opened += it.name },
                    animated = false
                )
            }
        }
    }

    private fun content(vararg summaries: TodoListSummary) = TodoListsState.Content(summaries.toList(), emptyList())

    private fun target(name: String, day: LocalDate) = TodoListSummary(
        TodoList(name, name, targetDate = day),
        allDone = false,
        horizon = ListHorizon.of(day, TODAY)
    )

    private fun due(name: String, day: LocalDate, status: DueDateStatus) = TodoListSummary(
        TodoList(name, name, dueDate = day),
        allDone = false,
        dueDateStatus = status,
        horizon = ListHorizon.of(day, TODAY)
    )

    private fun late(name: String, day: LocalDate) =
        due(name, day, DueDateStatus.OVERDUE).copy(daysLate = 2)

    private fun undated(name: String) = TodoListSummary(TodoList(name, name), allDone = false)

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 17)
    }
}
