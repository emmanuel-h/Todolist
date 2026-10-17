package fr.mandarine.todolist.ui.todolists

import android.os.Bundle
import fr.mandarine.todolist.ui.calendar.CalendarView
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A rotation rebuilds the window, and this state hangs off the window rather than
 * off a composition. What the reader was in the middle of writing has to make the
 * crossing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsScreenStateSaveTest {

    @Test
    fun `should carry a half-written list name across`() {
        val saved = Bundle()
        TodoListsScreenState().apply {
            openAddRow()
            addRowText = "Groc"
        }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertTrue(restored.addRowExpanded)
        assertEquals("Groc", restored.addRowText)
    }

    @Test
    fun `should carry the day circled beside a half-written name across`() {
        val saved = Bundle()
        TodoListsScreenState().apply {
            addRowSelection = DateSelection(DateKind.DUE, LocalDate.of(2026, 3, 4))
        }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertEquals(DateKind.DUE, restored.addRowSelection.kind)
        assertEquals(LocalDate.of(2026, 3, 4), restored.addRowSelection.date)
    }

    @Test
    fun `should carry a line with no day circled across as no day`() {
        val saved = Bundle()
        TodoListsScreenState().apply { addRowText = "Work" }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertNull(restored.addRowSelection.date)
        assertEquals(DateKind.TARGET, restored.addRowSelection.kind)
    }

    @Test
    fun `should carry an open edit sheet across`() {
        val saved = Bundle()
        TodoListsScreenState().apply {
            rename = RenameState(
                listId = "list-1",
                name = "Weekend",
                selection = DateSelection(DateKind.TARGET, LocalDate.of(2026, 5, 6))
            )
        }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertEquals("list-1", restored.rename?.listId)
        assertEquals("Weekend", restored.rename?.name)
        assertEquals(LocalDate.of(2026, 5, 6), restored.rename?.selection?.date)
    }

    @Test
    fun `should leave the edit sheet shut when none was open`() {
        val saved = Bundle()
        TodoListsScreenState().saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertNull(restored.rename)
        assertFalse(restored.addRowExpanded)
    }

    @Test
    fun `should carry the open tab and the month the calendar is turned to across`() {
        val saved = Bundle()
        TodoListsScreenState().apply {
            tab = BoardTab.Calendar
            calendarMonth = YearMonth.of(2026, 12)
        }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertEquals(BoardTab.Calendar, restored.tab)
        assertEquals(YearMonth.of(2026, 12), restored.calendarMonth)
    }

    @Test
    fun `should open on the board with the calendar on this month when nothing was saved`() {
        val restored = TodoListsScreenState().apply { restoreFrom(Bundle()) }

        assertEquals(BoardTab.Lists, restored.tab)
        assertNull(restored.calendarMonth)
        assertEquals(CalendarView.Month, restored.calendarView)
        assertNull(restored.calendarDay)
    }

    @Test
    fun `should carry the calendar's view and the day it is turned to across`() {
        val saved = Bundle()
        TodoListsScreenState().apply {
            calendarView = CalendarView.Week
            calendarDay = LocalDate.of(2026, 11, 3)
        }.saveTo(saved)

        val restored = TodoListsScreenState().apply { restoreFrom(saved) }

        assertEquals(CalendarView.Week, restored.calendarView)
        assertEquals(LocalDate.of(2026, 11, 3), restored.calendarDay)
    }

    @Test
    fun `should open a day on today when leaving this month for the day view`() {
        val state = TodoListsScreenState()

        state.chooseCalendarView(CalendarView.Day, TODAY)

        assertEquals(CalendarView.Day, state.calendarView)
        assertEquals(TODAY, state.calendarDay)
    }

    @Test
    fun `should open a week on the first of the month the reader turned to`() {
        val state = TodoListsScreenState().apply { calendarMonth = YearMonth.of(2026, 12) }

        state.chooseCalendarView(CalendarView.Week, TODAY)

        assertEquals(LocalDate.of(2026, 12, 1), state.calendarDay)
    }

    @Test
    fun `should keep the day last opened when it is in the month on show`() {
        val state = TodoListsScreenState().apply { calendarDay = LocalDate.of(2026, 10, 24) }

        state.chooseCalendarView(CalendarView.Day, TODAY)

        assertEquals(LocalDate.of(2026, 10, 24), state.calendarDay)
    }

    @Test
    fun `should keep the day when moving between the day and the week`() {
        val state = TodoListsScreenState().apply {
            calendarView = CalendarView.Day
            calendarDay = LocalDate.of(2027, 1, 5)
        }

        state.chooseCalendarView(CalendarView.Week, TODAY)

        assertEquals(CalendarView.Week, state.calendarView)
        assertEquals(LocalDate.of(2027, 1, 5), state.calendarDay)
    }

    @Test
    fun `should turn the month to the day on show when going back to it`() {
        val state = TodoListsScreenState().apply {
            calendarView = CalendarView.Day
            calendarDay = LocalDate.of(2027, 1, 5)
        }

        state.chooseCalendarView(CalendarView.Month, TODAY)

        assertEquals(CalendarView.Month, state.calendarView)
        assertEquals(YearMonth.of(2027, 1), state.calendarMonth)
    }

    @Test
    fun `should open a pressed day in the day view`() {
        val state = TodoListsScreenState()

        state.openCalendarDay(LocalDate.of(2026, 10, 24))

        assertEquals(CalendarView.Day, state.calendarView)
        assertEquals(LocalDate.of(2026, 10, 24), state.calendarDay)
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 17)
    }
}
