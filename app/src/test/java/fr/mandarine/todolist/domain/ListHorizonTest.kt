package fr.mandarine.todolist.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ListHorizonTest {

    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun `should list the horizons in the order the page reads them`() {
        assertEquals(
            listOf(
                ListHorizon.OVERDUE,
                ListHorizon.TODAY,
                ListHorizon.THIS_WEEK,
                ListHorizon.LATER,
                ListHorizon.UNDATED
            ),
            ListHorizon.entries.toList()
        )
    }

    @Test
    fun `should read no date as undated`() {
        assertEquals(ListHorizon.UNDATED, ListHorizon.of(null, today))
    }

    @Test
    fun `should read yesterday as overdue`() {
        assertEquals(ListHorizon.OVERDUE, ListHorizon.of(today.minusDays(1), today))
    }

    @Test
    fun `should read today as today`() {
        assertEquals(ListHorizon.TODAY, ListHorizon.of(today, today))
    }

    @Test
    fun `should read the sixth day ahead as this week`() {
        assertEquals(ListHorizon.THIS_WEEK, ListHorizon.of(today.plusDays(6), today))
    }

    @Test
    fun `should read the seventh day ahead as later`() {
        assertEquals(ListHorizon.LATER, ListHorizon.of(today.plusDays(7), today))
    }
}
