package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class EditTodoListUseCaseDueDateTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: EditTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = EditTodoListUseCase(repository)
    }

    @Test
    fun `should update with given due date when due date is provided`() {
        val dueDate = LocalDate.of(2027, 6, 22)

        useCase("list-1", "Groceries", targetDate = null, dueDate = dueDate)

        verify { repository.update("list-1", "Groceries", null, dueDate) }
    }

    @Test
    fun `should update with null due date when due date is null`() {
        useCase("list-1", "Groceries", targetDate = null, dueDate = null)

        verify { repository.update("list-1", "Groceries", null, null) }
    }

    @Test
    fun `should clear due date when target date is provided`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        useCase("list-1", "Groceries", targetDate = targetDate, dueDate = null)

        verify { repository.update("list-1", "Groceries", targetDate, null) }
    }

    @Test
    fun `should update with another id and due date`() {
        val dueDate = LocalDate.of(2026, 1, 15)

        useCase("list-42", "Work", targetDate = null, dueDate = dueDate)

        verify { repository.update("list-42", "Work", null, dueDate) }
    }

    @Test
    fun `should throw IllegalArgumentException when both target date and due date are non-null`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        val dueDate = LocalDate.of(2027, 7, 1)

        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", "Groceries", targetDate = targetDate, dueDate = dueDate)
        }
    }

    @Test
    fun `should not update when name is blank`() {
        val dueDate = LocalDate.of(2027, 1, 1)

        runCatching { useCase("list-1", "   ", targetDate = null, dueDate = dueDate) }

        verify(exactly = 0) { repository.update("list-1", "   ", null, dueDate) }
    }

    @Test
    fun `should throw IllegalArgumentException when name is blank and dueDate is provided`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", "   ", targetDate = null, dueDate = LocalDate.of(2027, 1, 1))
        }
    }

    @Test
    fun `should not update when both target date and due date are non-null`() {
        val targetDate = LocalDate.of(2027, 6, 22)
        val dueDate = LocalDate.of(2027, 7, 1)

        runCatching { useCase("list-1", "Groceries", targetDate = targetDate, dueDate = dueDate) }

        verify(exactly = 0) { repository.update("list-1", "Groceries", targetDate, dueDate) }
    }

    @Test
    fun `should use null as default due date when not provided`() {
        useCase("list-1", "Groceries", targetDate = null)

        verify { repository.update("list-1", "Groceries", null, null) }
    }
}
