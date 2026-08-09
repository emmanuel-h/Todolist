package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CreateTodoListUseCaseDueDateTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: CreateTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = CreateTodoListUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should create list with given due date when due date is provided`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        val result = useCase("Groceries", dueDate = dueDate)

        assertEquals(dueDate, result.dueDate)
    }

    @Test
    fun `should create list with null due date when null is provided`() {
        val result = useCase("Groceries", dueDate = null)

        assertNull(result.dueDate)
    }

    @Test
    fun `should use null as default due date when no due date is given`() {
        val result = useCase("Groceries")

        assertNull(result.dueDate)
    }

    @Test
    fun `should add list with due date to repository`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        val result = useCase("Groceries", dueDate = dueDate)

        verify { repository.addAtTop(result) }
    }

    @Test
    fun `should create list with another due date`() {
        val dueDate = LocalDate.of(2026, 1, 15)

        val result = useCase("Work", dueDate = dueDate)

        assertEquals(dueDate, result.dueDate)
    }

    @Test
    fun `should throw IllegalArgumentException when both target date and due date are provided`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        val dueDate = LocalDate.of(2027, 7, 1)

        assertThrows(IllegalArgumentException::class.java) {
            useCase("Groceries", targetDate = targetDate, dueDate = dueDate)
        }
    }

    @Test
    fun `should not call repository when both target date and due date are provided`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        val dueDate = LocalDate.of(2027, 7, 1)

        runCatching { useCase("Groceries", targetDate = targetDate, dueDate = dueDate) }

        verify(exactly = 0) { repository.addAtTop(any()) }
    }

    @Test
    fun `should have null target date when only due date is provided`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        val result = useCase("Groceries", dueDate = dueDate)

        assertNull(result.targetDate)
    }

    @Test
    fun `should have null due date when only target date is provided`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        val result = useCase("Groceries", targetDate = targetDate)

        assertNull(result.dueDate)
    }
}
