package fr.mandarine.todolist.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListNotificationTest {

    private val list = TodoList("list-id", "Test List")

    @Test
    fun `should expose the wrapped list for DueDateToday`() {
        assertEquals(list, ListNotification.DueDateToday(list).list)
    }

    @Test
    fun `should expose the wrapped list for TargetDateTomorrow`() {
        assertEquals(list, ListNotification.TargetDateTomorrow(list).list)
    }
}
