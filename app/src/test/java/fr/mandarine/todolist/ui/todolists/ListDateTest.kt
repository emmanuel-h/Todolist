package fr.mandarine.todolist.ui.todolists

import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.PaperPalette
import fr.mandarine.todolist.ui.paper.inked
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `should owe the notification ask when a day is written under the alarm`() {
        val written = dueDateWritten(DateSelection.None, DateSelection(DateKind.DUE, date))

        assertTrue(written)
    }

    @Test
    fun `should owe the notification ask when the alarm is rung over a day already written`() {
        val before = DateSelection(DateKind.TARGET, date)

        assertTrue(dueDateWritten(before, before.withKind(DateKind.DUE)))
    }

    @Test
    fun `should owe no notification ask when the day written is a target`() {
        val written = dueDateWritten(DateSelection.None, DateSelection(DateKind.TARGET, date))

        assertFalse(written)
    }

    @Test
    fun `should owe no notification ask when a due date is turned back into a target`() {
        val before = DateSelection(DateKind.DUE, date)

        assertFalse(dueDateWritten(before, before.withKind(DateKind.TARGET)))
    }

    @Test
    fun `should owe no notification ask when the alarm is rung with no day written`() {
        val written = dueDateWritten(DateSelection.None, DateSelection(DateKind.DUE, null))

        assertFalse(written)
    }

    @Test
    fun `should owe no notification ask when a due date is left exactly as it was`() {
        val before = DateSelection(DateKind.DUE, date)

        assertFalse(dueDateWritten(before, before.withDate(date)))
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
    fun `should pencil a target date that has not passed`() {
        assertEquals(InkTone.Margin, targetTone(elapsed = false))
    }

    @Test
    fun `should write a target date that has passed in the same ink as anything done`() {
        assertEquals(InkTone.Crossed, targetTone(elapsed = true))
    }

    @Test
    fun `should keep every date jot tint fully opaque so its contrast is the ink's own`() {
        val tones = listOf(
            targetTone(elapsed = true),
            targetTone(elapsed = false),
            dueTone(DueDateStatus.FUTURE),
            dueTone(DueDateStatus.TODAY),
            dueTone(DueDateStatus.OVERDUE)
        )

        tones.forEach { assertEquals(1f, PaperPalette.light.inked(it).alpha, 0.001f) }
    }

    @Test
    fun `should pencil a future due date like any other date`() {
        assertEquals(InkTone.Margin, dueTone(DueDateStatus.FUTURE))
    }

    @Test
    fun `should warn on a due date falling today`() {
        assertEquals(InkTone.Today, dueTone(DueDateStatus.TODAY))
        assertEquals(PaperPalette.light.inkAmber, PaperPalette.light.inked(InkTone.Today))
    }

    @Test
    fun `should alarm on a due date already passed`() {
        assertEquals(InkTone.Lost, dueTone(DueDateStatus.OVERDUE))
        assertEquals(PaperPalette.light.inkRed, PaperPalette.light.inked(InkTone.Lost))
    }
}
