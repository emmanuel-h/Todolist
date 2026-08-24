package fr.mandarine.todolist.ui.listmeta

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DateJotTest {

    @Test
    fun `should jot a date as a day and a short month with no weekday`() {
        assertEquals("14 Mar", formatJotDate(DATE, showYear = false, locale = Locale.UK))
    }

    @Test
    fun `should keep the jot free of the year when the year is the current one`() {
        val jotted = formatJotDate(DATE, showYear = false, locale = Locale.UK)

        assertFalse("Expected no year in $jotted", jotted.contains("26"))
    }

    @Test
    fun `should append a two-digit year when the date is not from this year`() {
        val jotted = formatJotDate(DATE, showYear = true, locale = Locale.UK)

        assertTrue("Expected a short year in $jotted", jotted.contains("26"))
        assertFalse("Expected no four-digit year in $jotted", jotted.contains("2026"))
    }

    @Test
    fun `should not impose English field order on a French locale`() {
        assertEquals("14 mars", formatJotDate(DATE, showYear = false, locale = Locale.FRANCE))
    }

    @Test
    fun `should not impose English field order on a German locale`() {
        assertEquals("14. März", formatJotDate(DATE, showYear = false, locale = Locale.GERMANY))
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 3, 14)
    }
}
