package fr.mandarine.todolist.domain

import fr.mandarine.todolist.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ComputePendingNotificationsUseCaseTest {

    private val today = LocalDate.of(2026, 6, 15)
    private val tomorrow = today.plusDays(1)
    private val yesterday = today.minusDays(1)
    private val clock = FakeClock(todayDate = today)
    private val useCase = ComputePendingNotificationsUseCase(clock)

    @Test
    fun `should return empty list when no lists provided`() {
        assertTrue(useCase(emptyList()).isEmpty())
    }

    @Test
    fun `should return empty list when list has no dates`() {
        assertTrue(useCase(listOf(TodoList("1", "Work"))).isEmpty())
    }

    @Test
    fun `should return DueDateToday when list dueDate equals today`() {
        val list = TodoList("1", "Work", dueDate = today)
        assertEquals(listOf(ListNotification.DueDateToday(list)), useCase(listOf(list)))
    }

    @Test
    fun `should not return notification when list dueDate is before today`() {
        assertTrue(useCase(listOf(TodoList("1", "Work", dueDate = yesterday))).isEmpty())
    }

    @Test
    fun `should not return notification when list dueDate is after today`() {
        assertTrue(useCase(listOf(TodoList("1", "Work", dueDate = tomorrow))).isEmpty())
    }

    @Test
    fun `should return TargetDateTomorrow when list targetDate is tomorrow`() {
        val list = TodoList("1", "Work", targetDate = tomorrow)
        assertEquals(listOf(ListNotification.TargetDateTomorrow(list)), useCase(listOf(list)))
    }

    @Test
    fun `should not return notification when list targetDate is today`() {
        assertTrue(useCase(listOf(TodoList("1", "Work", targetDate = today))).isEmpty())
    }

    @Test
    fun `should not return notification when list targetDate is day after tomorrow`() {
        assertTrue(useCase(listOf(TodoList("1", "Work", targetDate = tomorrow.plusDays(1)))).isEmpty())
    }

    @Test
    fun `should not return notification when list targetDate is yesterday`() {
        assertTrue(useCase(listOf(TodoList("1", "Work", targetDate = yesterday))).isEmpty())
    }

    @Test
    fun `should return DueDateToday and TargetDateTomorrow when both match`() {
        val listA = TodoList("1", "Due Today", dueDate = today)
        val listB = TodoList("2", "Target Tomorrow", targetDate = tomorrow)
        val result = useCase(listOf(listA, listB))
        assertEquals(2, result.size)
        assertTrue(result.contains(ListNotification.DueDateToday(listA)))
        assertTrue(result.contains(ListNotification.TargetDateTomorrow(listB)))
    }

    @Test
    fun `should include only matching lists when mixed dates`() {
        val matchingDue = TodoList("1", "Due Today", dueDate = today)
        val futureDue = TodoList("2", "Future Due", dueDate = tomorrow)
        val matchingTarget = TodoList("3", "Target Tomorrow", targetDate = tomorrow)
        val pastTarget = TodoList("4", "Past Target", targetDate = yesterday)
        val noDate = TodoList("5", "No Date")
        val result = useCase(listOf(matchingDue, futureDue, matchingTarget, pastTarget, noDate))
        assertEquals(2, result.size)
        assertTrue(result.any { it is ListNotification.DueDateToday && it.list == matchingDue })
        assertTrue(result.any { it is ListNotification.TargetDateTomorrow && it.list == matchingTarget })
    }

    @Test
    fun `should not return DueDateToday when list has no dueDate`() {
        val list = TodoList("1", "Work", targetDate = today)
        assertTrue(useCase(listOf(list)).isEmpty())
    }

    @Test
    fun `should return separate notifications for each matching list with dueDate today`() {
        val listA = TodoList("1", "A", dueDate = today)
        val listB = TodoList("2", "B", dueDate = today)
        val result = useCase(listOf(listA, listB))
        assertEquals(2, result.size)
        assertTrue(result.contains(ListNotification.DueDateToday(listA)))
        assertTrue(result.contains(ListNotification.DueDateToday(listB)))
    }

    @Test
    fun `should return separate notifications for each matching list with targetDate tomorrow`() {
        val listA = TodoList("1", "A", targetDate = tomorrow)
        val listB = TodoList("2", "B", targetDate = tomorrow)
        val result = useCase(listOf(listA, listB))
        assertEquals(2, result.size)
        assertTrue(result.contains(ListNotification.TargetDateTomorrow(listA)))
        assertTrue(result.contains(ListNotification.TargetDateTomorrow(listB)))
    }
}
