package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTodoListsWithStatusUseCaseDueDateTest {

    private lateinit var todoListRepository: TodoListRepository
    private lateinit var todoRepository: TodoRepository
    private lateinit var useCase: GetTodoListsWithStatusUseCase

    private val todayEpochDay = 100L
    private val todayMillis = todayEpochDay * 86_400_000L
    private val today = LocalDate.ofEpochDay(todayEpochDay)
    private val yesterday = LocalDate.ofEpochDay(todayEpochDay - 1)
    private val tomorrow = LocalDate.ofEpochDay(todayEpochDay + 1)

    @Before
    fun setUp() {
        todoListRepository = mockk()
        todoRepository = mockk()
        useCase = GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, Clock { todayMillis })
    }

    @Test
    fun `should return null dueDateStatus when due date is null`() {
        val list = TodoList("list-1", "Groceries", dueDate = null)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertNull(result[0].dueDateStatus)
    }

    @Test
    fun `should return OVERDUE dueDateStatus when due date is in the past`() {
        val list = TodoList("list-1", "Groceries", dueDate = yesterday)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertEquals(DueDateStatus.OVERDUE, result[0].dueDateStatus)
    }

    @Test
    fun `should return TODAY dueDateStatus when due date is today`() {
        val list = TodoList("list-1", "Groceries", dueDate = today)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertEquals(DueDateStatus.TODAY, result[0].dueDateStatus)
    }

    @Test
    fun `should return FUTURE dueDateStatus when due date is in the future`() {
        val list = TodoList("list-1", "Groceries", dueDate = tomorrow)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertEquals(DueDateStatus.FUTURE, result[0].dueDateStatus)
    }

    @Test
    fun `should compute dueDateStatus independently for each list`() {
        val listOverdue = TodoList("list-1", "Overdue", dueDate = yesterday)
        val listToday = TodoList("list-2", "Today", dueDate = today)
        val listFuture = TodoList("list-3", "Future", dueDate = tomorrow)
        val listNone = TodoList("list-4", "NoDueDate", dueDate = null)
        every { todoListRepository.getAll() } returns listOf(listOverdue, listToday, listFuture, listNone)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()
        every { todoRepository.getAllByListId("list-2") } returns emptyList()
        every { todoRepository.getAllByListId("list-3") } returns emptyList()
        every { todoRepository.getAllByListId("list-4") } returns emptyList()

        val result = useCase()

        assertEquals(DueDateStatus.OVERDUE, result[0].dueDateStatus)
        assertEquals(DueDateStatus.TODAY, result[1].dueDateStatus)
        assertEquals(DueDateStatus.FUTURE, result[2].dueDateStatus)
        assertNull(result[3].dueDateStatus)
    }

    @Test
    fun `should return showDueDateYear false when due date is null`() {
        val list = TodoList("list-1", "Groceries", dueDate = null)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertFalse(result[0].showDueDateYear)
    }

    @Test
    fun `should return showDueDateYear false when due date is in the same year as today`() {
        val sameYear = LocalDate.ofEpochDay(todayEpochDay + 30)
        val list = TodoList("list-1", "Groceries", dueDate = sameYear)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertFalse(result[0].showDueDateYear)
    }

    @Test
    fun `should return showDueDateYear true when due date is in a different year from today`() {
        val differentYear = today.withYear(today.year + 1)
        val list = TodoList("list-1", "Groceries", dueDate = differentYear)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertTrue(result[0].showDueDateYear)
    }

    @Test
    fun `should return showDueDateYear true when due date year is before current year`() {
        val pastYear = today.withYear(today.year - 1)
        val list = TodoList("list-1", "Groceries", dueDate = pastYear)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()

        val result = useCase()

        assertTrue(result[0].showDueDateYear)
    }

    @Test
    fun `should compute showDueDateYear independently for each list`() {
        val sameYear = LocalDate.ofEpochDay(todayEpochDay + 10)
        val diffYear = today.withYear(today.year + 2)
        val listA = TodoList("list-1", "SameYear", dueDate = sameYear)
        val listB = TodoList("list-2", "DiffYear", dueDate = diffYear)
        every { todoListRepository.getAll() } returns listOf(listA, listB)
        every { todoRepository.getAllByListId("list-1") } returns emptyList()
        every { todoRepository.getAllByListId("list-2") } returns emptyList()

        val result = useCase()

        assertFalse(result[0].showDueDateYear)
        assertTrue(result[1].showDueDateYear)
    }

    @Test
    fun `should return OVERDUE dueDateStatus even when all items are done`() {
        val list = TodoList("list-1", "Groceries", dueDate = yesterday)
        val item = TodoItem("item-1", "Milk", "list-1", isCompleted = true, completedAt = 1000L)
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.getAllByListId("list-1") } returns listOf(item)

        val result = useCase()

        assertEquals(DueDateStatus.OVERDUE, result[0].dueDateStatus)
    }
}
