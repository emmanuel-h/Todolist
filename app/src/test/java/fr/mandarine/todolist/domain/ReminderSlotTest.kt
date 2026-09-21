package fr.mandarine.todolist.domain

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSlotTest {

    @Test
    fun `should return AppWide when list has no reminder time`() {
        val list = TodoList("1", "Groceries")

        assertEquals(ReminderSlot.AppWide, list.reminderSlot)
    }

    @Test
    fun `should return At with the list time when list has a reminder time`() {
        val time = LocalTime.of(7, 30)
        val list = TodoList("1", "Groceries", reminderTime = time)

        val slot = list.reminderSlot

        assertEquals(ReminderSlot.At(time), slot)
    }

    @Test
    fun `should expose the time in the At slot`() {
        val time = LocalTime.of(14, 45)
        val list = TodoList("1", "Groceries", reminderTime = time)

        val slot = list.reminderSlot as ReminderSlot.At

        assertEquals(time, slot.time)
        assertEquals(14, slot.time.hour)
        assertEquals(45, slot.time.minute)
    }

    @Test
    fun `should not equal AppWide when list has a reminder time`() {
        val list = TodoList("1", "Groceries", reminderTime = LocalTime.of(7, 30))

        val isAppWide = list.reminderSlot == ReminderSlot.AppWide

        assertEquals(false, isAppWide)
    }

    @Test
    fun `should not equal At when list has no reminder time`() {
        val list = TodoList("1", "Groceries")

        val isAt = list.reminderSlot is ReminderSlot.At

        assertEquals(false, isAt)
    }
}
