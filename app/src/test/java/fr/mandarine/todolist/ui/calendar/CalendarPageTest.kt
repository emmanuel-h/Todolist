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
import java.time.DayOfWeek
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
    private val turnedTo = mutableListOf<LocalDate>()
    private val dayOpened = mutableListOf<LocalDate>()
    private val chosen = mutableListOf<CalendarView>()
    private val opened = mutableListOf<String>()

    @Test
    fun `should mark each day a list falls on in the ink its day is written in`() {
        val marks = marksOn(
            listOf(late("a", TODAY.minusDays(2)), due("b", TODAY, DueDateStatus.TODAY), target("c", TODAY.plusDays(3)))
        )

        assertEquals(
            mapOf(TODAY.minusDays(2) to InkTone.Lost, TODAY to InkTone.Today, TODAY.plusDays(3) to InkTone.Margin),
            marks
        )
    }

    @Test
    fun `should mark a day several lists share in the loudest of their inks`() {
        val marks = marksOn(listOf(target("a", TODAY), due("b", TODAY, DueDateStatus.TODAY)))

        assertEquals(mapOf(TODAY to InkTone.Today), marks)
    }

    @Test
    fun `should lay a week out from its first day`() {
        val week = weekOf(TODAY, DayOfWeek.MONDAY)

        assertEquals(LocalDate.of(2026, 10, 12), week.first())
        assertEquals(LocalDate.of(2026, 10, 18), week.last())
        assertEquals(7, week.size)
    }

    @Test
    fun `should start a week on sunday where the week does`() {
        assertEquals(LocalDate.of(2026, 10, 11), weekOf(TODAY, DayOfWeek.SUNDAY).first())
    }

    @Test
    fun `should list under the month what is dated in it and what is still outstanding`() {
        val missed = late("missed", LocalDate.of(2026, 9, 30))
        val inMonth = target("in", TODAY.plusDays(3))
        val nextMonth = target("next", TODAY.plusMonths(1))

        assertEquals(
            listOf(missed, inMonth),
            agendaFor(listOf(missed, inMonth, nextMonth), YearMonth.from(TODAY), TODAY)
        )
    }

    @Test
    fun `should open a day pressed on the month`() {
        render(content(target("a", TODAY.plusDays(3))))

        composeRule.onNodeWithContentDescription("Saturday, October 24, 2026").performClick()

        assertEquals(listOf(LocalDate.of(2026, 10, 24)), dayOpened)
    }

    @Test
    fun `should choose the view by name`() {
        render(content(target("a", TODAY.plusDays(3))))

        composeRule.onNodeWithText("Week").performClick()
        composeRule.onNodeWithText("Day").performClick()

        assertEquals(listOf(CalendarView.Week, CalendarView.Day), chosen)
    }

    @Test
    fun `should show under the month only what it holds`() {
        render(content(target("soon", TODAY.plusDays(3)), target("later", TODAY.plusMonths(1))))

        composeRule.onNodeWithText("soon").assertIsDisplayed()
        composeRule.onNodeWithText("later").assertDoesNotExist()
    }

    @Test
    fun `should write each list of the week on its day's line`() {
        render(content(target("trip", TODAY), target("away", TODAY.plusDays(1))), CalendarView.Week)

        composeRule.onNodeWithText("11 – 17").assertIsDisplayed()
        composeRule.onNodeWithText("trip").assertIsDisplayed()
        composeRule.onNodeWithText("away").assertDoesNotExist()
    }

    @Test
    fun `should name both months when a week runs across them`() {
        render(content(), CalendarView.Week, day = LocalDate.of(2026, 10, 1))

        composeRule.onNodeWithText("Sep 27 – Oct 3").assertIsDisplayed()
    }

    @Test
    fun `should open the list a scrap on the week is for`() {
        render(content(target("trip", TODAY)), CalendarView.Week)

        composeRule.onNodeWithText("trip").performClick()

        assertEquals(listOf("trip"), opened)
    }

    @Test
    fun `should open a day pressed on the week`() {
        render(content(), CalendarView.Week)

        composeRule.onNodeWithContentDescription("Wednesday, October 14, 2026").performClick()

        assertEquals(listOf(LocalDate.of(2026, 10, 14)), dayOpened)
    }

    @Test
    fun `should turn the week a week at a time`() {
        render(content(), CalendarView.Week)

        composeRule.onNodeWithContentDescription("Next week").performClick()
        composeRule.onNodeWithContentDescription("Previous week").performClick()

        assertEquals(listOf(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 4)), turnedTo)
    }

    @Test
    fun `should show the lists of the day and what is still late on today`() {
        render(content(due("party", TODAY, DueDateStatus.TODAY), late("work", TODAY.minusDays(2))), CalendarView.Day)

        composeRule.onNodeWithText("Saturday · TODAY").assertIsDisplayed()
        composeRule.onNodeWithText("October 17").assertIsDisplayed()
        composeRule.onNodeWithText("party").assertIsDisplayed()
        composeRule.onNodeWithText("Still late").assertIsDisplayed()
        composeRule.onNodeWithText("work").assertIsDisplayed()
    }

    @Test
    fun `should leave what is late to today on any other day`() {
        render(content(late("work", TODAY.minusDays(2))), CalendarView.Day, day = TODAY.plusDays(1))

        composeRule.onNodeWithText("Sunday").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing on this day.").assertIsDisplayed()
        composeRule.onNodeWithText("Still late").assertDoesNotExist()
    }

    @Test
    fun `should write the year on a day in another year`() {
        render(content(), CalendarView.Day, day = LocalDate.of(2027, 1, 5))

        composeRule.onNodeWithText("January 5, 2027").assertIsDisplayed()
    }

    @Test
    fun `should step a day at a time and move to a day pressed on its week`() {
        render(content(), CalendarView.Day)

        composeRule.onNodeWithContentDescription("Next day").performClick()
        composeRule.onNodeWithContentDescription("Previous day").performClick()
        composeRule.onNodeWithContentDescription("Monday, October 12, 2026").performClick()

        assertEquals(listOf(TODAY.plusDays(1), TODAY.minusDays(1), LocalDate.of(2026, 10, 12)), turnedTo)
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

    private fun render(state: TodoListsState, view: CalendarView = CalendarView.Month, day: LocalDate? = null) {
        composeRule.setContent {
            PaperTheme {
                CalendarPage(
                    state = state,
                    today = TODAY,
                    view = view,
                    month = null,
                    day = day,
                    onViewChosen = { chosen += it },
                    onMonthTurned = { turned += it },
                    onDayTurned = { turnedTo += it },
                    onDayOpened = { dayOpened += it },
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
