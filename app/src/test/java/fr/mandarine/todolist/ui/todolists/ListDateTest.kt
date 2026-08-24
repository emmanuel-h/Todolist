package fr.mandarine.todolist.ui.todolists

import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.ui.paper.PaperPalette
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListDateTest {

    private val date = LocalDate.of(2026, 3, 14)

    @Test
    fun `should read a due date as the due kind`() {
        val selection = DateSelection.of(targetDate = null, dueDate = date)

        assertEquals(DateKind.DUE, selection.kind)
        assertEquals(date, selection.dueDate)
        assertNull(selection.targetDate)
    }

    @Test
    fun `should read a target date as the target kind`() {
        val selection = DateSelection.of(targetDate = date, dueDate = null)

        assertEquals(DateKind.TARGET, selection.kind)
        assertEquals(date, selection.targetDate)
        assertNull(selection.dueDate)
    }

    @Test
    fun `should read no date at all as an empty target selection`() {
        val selection = DateSelection.of(targetDate = null, dueDate = null)

        assertEquals(DateKind.TARGET, selection.kind)
        assertNull(selection.date)
    }

    @Test
    fun `should carry the date across when the kind switches to due`() {
        val selection = DateSelection(DateKind.TARGET, date).withKind(DateKind.DUE)

        assertEquals(date, selection.dueDate)
        assertNull(selection.targetDate)
    }

    @Test
    fun `should carry the date across when the kind switches back to target`() {
        val selection = DateSelection(DateKind.DUE, date).withKind(DateKind.TARGET)

        assertEquals(date, selection.targetDate)
        assertNull(selection.dueDate)
    }

    @Test
    fun `should keep the kind when the date is cleared`() {
        val selection = DateSelection(DateKind.DUE, date).cleared()

        assertEquals(DateKind.DUE, selection.kind)
        assertNull(selection.date)
    }

    @Test
    fun `should replace the date while keeping the kind`() {
        val other = LocalDate.of(2026, 4, 1)

        val selection = DateSelection(DateKind.DUE, date).withDate(other)

        assertEquals(DateKind.DUE, selection.kind)
        assertEquals(other, selection.date)
    }

    @Test
    fun `should format a date without its year using the locale field order`() {
        val formatted = formatListDate(date, showYear = false, locale = Locale.UK)

        assertEquals("Sat, 14 Mar", formatted)
    }

    @Test
    fun `should format a date with its year using the locale field order`() {
        val formatted = formatListDate(date, showYear = true, locale = Locale.UK)

        assertTrue("Expected the year in $formatted", formatted.contains("2026"))
    }

    @Test
    fun `should not impose English field order on a German locale`() {
        val formatted = formatListDate(date, showYear = false, locale = Locale.GERMANY)

        assertEquals("Sa., 14. März", formatted)
    }

    @Test
    fun `should not impose English field order on a French locale`() {
        val formatted = formatListDate(date, showYear = false, locale = Locale.FRANCE)

        assertEquals("sam. 14 mars", formatted)
    }

    @Test
    fun `should survive a round trip through the picker's epoch millis`() {
        assertEquals(date, localDateFromPickerMillis(date.toPickerMillis()))
    }

    @Test
    fun `should survive a round trip for a date before the epoch`() {
        val old = LocalDate.of(1965, 7, 2)

        assertEquals(old, localDateFromPickerMillis(old.toPickerMillis()))
    }

    @Test
    fun `should read a mid-day picker value as that same day`() {
        val midday = date.toPickerMillis() + 43_200_000L

        assertEquals(date, localDateFromPickerMillis(midday))
    }

    @Test
    fun `should ink a target date that has not passed`() {
        assertEquals(PaperPalette.light.inkBlue, targetTint(PaperPalette.light, elapsed = false))
    }

    @Test
    fun `should fade a target date that has passed`() {
        assertEquals(PaperPalette.light.inkSoft, targetTint(PaperPalette.light, elapsed = true))
    }

    @Test
    fun `should ink a future due date like any other date`() {
        assertEquals(PaperPalette.light.inkBlue, dueTint(PaperPalette.light, DueDateStatus.FUTURE))
    }

    @Test
    fun `should warn on a due date falling today`() {
        assertEquals(PaperPalette.light.inkAmber, dueTint(PaperPalette.light, DueDateStatus.TODAY))
    }

    @Test
    fun `should alarm on a due date already passed`() {
        assertEquals(PaperPalette.light.inkRed, dueTint(PaperPalette.light, DueDateStatus.OVERDUE))
    }
}
