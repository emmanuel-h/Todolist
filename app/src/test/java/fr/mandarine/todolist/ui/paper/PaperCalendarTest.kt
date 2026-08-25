package fr.mandarine.todolist.ui.paper

import android.text.format.DateFormat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import fr.mandarine.todolist.ui.staticText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaperCalendarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val picked = mutableListOf<LocalDate>()

    // ── The month's own arithmetic ────────────────────────────────────────────

    @Test
    fun `should start the week where the reader's locale starts it`() {
        assertEquals(DayOfWeek.MONDAY, WeekFields.of(Locale.FRANCE).firstDayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, WeekFields.of(Locale.US).firstDayOfWeek)
    }

    @Test
    fun `should write the weekday initials in the reader's own hand and order`() {
        assertEquals(
            listOf("L", "M", "M", "J", "V", "S", "D"),
            weekdayInitials(DayOfWeek.MONDAY, Locale.FRANCE)
        )
    }

    @Test
    fun `should write seven weekday initials starting on Sunday for an American reader`() {
        val initials = weekdayInitials(DayOfWeek.SUNDAY, Locale.US)

        assertEquals(7, initials.size)
        assertEquals("S", initials.first())
    }

    @Test
    fun `should leave five blanks before a month that opens on a Saturday in a Monday week`() {
        assertEquals(5, leadingBlanks(YearMonth.of(2026, 8), DayOfWeek.MONDAY))
    }

    @Test
    fun `should leave six blanks before a month that opens on a Saturday in a Sunday week`() {
        assertEquals(6, leadingBlanks(YearMonth.of(2026, 8), DayOfWeek.SUNDAY))
    }

    @Test
    fun `should leave no blank before a month that opens on the first day of the week`() {
        assertEquals(0, leadingBlanks(YearMonth.of(2026, 6), DayOfWeek.MONDAY))
    }

    @Test
    fun `should count the pages between two months`() {
        assertEquals(1207, pageOf(YearMonth.of(1926, 1), YearMonth.of(2026, 8)))
    }

    @Test
    fun `should throw the same ring around a day every time it is drawn`() {
        assertEquals(ringSeed(LocalDate.of(2026, 8, 15)), ringSeed(LocalDate.of(2026, 8, 15)))
    }

    @Test
    fun `should give two different days two different rings`() {
        assertNotEquals(
            ringSeed(LocalDate.of(2026, 8, 15)),
            ringSeed(LocalDate.of(2026, 8, 16))
        )
    }

    // ── The month on the sheet ────────────────────────────────────────────────

    @Test
    fun `should write the days of the opened month on the page`() {
        render(selected = SELECTED)

        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("31").assertIsDisplayed()
    }

    /**
     * The weekday initials are the one thing on this sheet that is a letter rather
     * than a number. They are date vocabulary — the same class of writing as the
     * month name in the header — and nothing else on the sheet is allowed to be
     * text at all.
     */
    @Test
    fun `should write nothing on the sheet that is not a date`() {
        render(selected = SELECTED)

        val locale = Locale.getDefault(Locale.Category.FORMAT)
        val allowed = weekdayInitials(WeekFields.of(locale).firstDayOfWeek, locale) +
            (1..31).map { it.toString() } +
            monthLabel(YearMonth.of(2026, 8))
        val written = composeRule.onRoot().fetchSemanticsNode().staticText()

        assertEquals(emptyList<String>(), written.filterNot { it in allowed })
    }

    @Test
    fun `should circle only the chosen day`() {
        render(selected = SELECTED)

        composeRule.onNodeWithText("15").assertIsSelected()
        composeRule.onNodeWithText("16").assertIsNotSelected()
    }

    @Test
    fun `should report the day that was circled`() {
        render(selected = SELECTED)

        composeRule.onNodeWithText("20").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(LocalDate.of(2026, 8, 20)), picked)
    }

    @Test
    fun `should report the same day again when it is chosen twice`() {
        render(selected = SELECTED)

        composeRule.onNodeWithText("15").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(SELECTED), picked)
    }

    @Test
    fun `should open on today's month when nothing has been chosen yet`() {
        render(selected = null)

        composeRule.onNodeWithText(monthLabel(YearMonth.of(2026, 8))).assertIsDisplayed()
    }

    @Test
    fun `should turn back a month on the back glyph`() {
        render(selected = SELECTED)

        composeRule.onNodeWithContentDescription(PREVIOUS_MONTH).performClick()

        composeRule.onNodeWithText(monthLabel(YearMonth.of(2026, 7))).assertIsDisplayed()
    }

    @Test
    fun `should turn on a month on the forward glyph`() {
        render(selected = SELECTED)

        composeRule.onNodeWithContentDescription(NEXT_MONTH).performClick()

        composeRule.onNodeWithText(monthLabel(YearMonth.of(2026, 9))).assertIsDisplayed()
    }

    @Test
    fun `should report a day from the month it was turned to`() {
        render(selected = SELECTED)

        composeRule.onNodeWithContentDescription(PREVIOUS_MONTH).performClick()
        composeRule.onNodeWithText("3").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(LocalDate.of(2026, 7, 3)), picked)
    }

    // ── The years on the same rules ───────────────────────────────────────────

    @Test
    fun `should open the years on the header`() {
        render(selected = SELECTED)

        composeRule.onNodeWithText(monthLabel(YearMonth.of(2026, 8))).performClick()

        composeRule.onNodeWithText("2026").assertIsSelected()
        composeRule.onNodeWithText("15").assertDoesNotExist()
    }

    @Test
    fun `should put the month navigation away while the years are open`() {
        render(selected = SELECTED)

        composeRule.onNodeWithText(monthLabel(YearMonth.of(2026, 8))).performClick()

        composeRule.onNodeWithContentDescription(PREVIOUS_MONTH).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(NEXT_MONTH).assertDoesNotExist()
    }

    @Test
    fun `should come back to the same month of the year that was chosen`() {
        render(selected = SELECTED)

        composeRule.onNodeWithText(monthLabel(YearMonth.of(2026, 8))).performClick()
        composeRule.onNodeWithText("2029").performClick()

        composeRule.onNodeWithText(monthLabel(YearMonth.of(2029, 8))).assertIsDisplayed()
    }

    private fun render(selected: LocalDate?) {
        composeRule.setContent {
            PaperTheme {
                PaperCalendar(
                    selected = selected,
                    today = TODAY,
                    onPick = { picked += it },
                    animated = false
                )
            }
        }
    }

    private fun monthLabel(month: YearMonth): String {
        val locale = Locale.getDefault(Locale.Category.FORMAT)
        val pattern = DateFormat.getBestDateTimePattern(locale, "MMMM y")
        return month.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 8, 23)
        val SELECTED: LocalDate = LocalDate.of(2026, 8, 15)
        const val PREVIOUS_MONTH = "Previous month"
        const val NEXT_MONTH = "Next month"
    }
}
