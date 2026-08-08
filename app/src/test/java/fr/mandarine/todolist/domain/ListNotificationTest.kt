package fr.mandarine.todolist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ListNotificationTest {

    private val list = TodoList("list-id", "Test List")

    @Test
    fun `should return list id hashCode as notification id for DueDateToday`() {
        assertEquals("list-id".hashCode(), ListNotification.DueDateToday(list).notificationId())
    }

    @Test
    fun `should return list id hashCode as notification id for TargetDateTomorrow`() {
        assertEquals("list-id".hashCode(), ListNotification.TargetDateTomorrow(list).notificationId())
    }

    @Test
    fun `should return different notification ids for different list ids`() {
        val notif1 = ListNotification.DueDateToday(TodoList("id-alpha", "A"))
        val notif2 = ListNotification.DueDateToday(TodoList("id-beta", "B"))
        assertNotEquals(notif1.notificationId(), notif2.notificationId())
    }
}
