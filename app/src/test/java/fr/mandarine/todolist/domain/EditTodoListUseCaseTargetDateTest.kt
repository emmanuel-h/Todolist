package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class EditTodoListUseCaseTargetDateTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: EditTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = EditTodoListUseCase(repository)
    }

    @Test
    fun `should call updateTargetDate with given date when target date is provided`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        useCase("list-1", "Groceries", targetDate)

        verify { repository.updateTargetDate("list-1", targetDate) }
    }

    @Test
    fun `should call updateTargetDate with null when target date is null`() {
        useCase("list-1", "Groceries", null)

        verify { repository.updateTargetDate("list-1", null) }
    }

    @Test
    fun `should call both updateName and updateTargetDate when editing a list`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        useCase("list-1", "Groceries", targetDate)

        verify { repository.updateName("list-1", "Groceries") }
        verify { repository.updateTargetDate("list-1", targetDate) }
    }

    @Test
    fun `should call updateTargetDate with another id and date`() {
        val targetDate = LocalDate.of(2026, 1, 15)

        useCase("list-42", "Work", targetDate)

        verify { repository.updateTargetDate("list-42", targetDate) }
    }

    @Test
    fun `should not call updateTargetDate when name is blank`() {
        val targetDate = LocalDate.of(2027, 1, 1)

        runCatching { useCase("list-1", "   ", targetDate) }

        verify(exactly = 0) { repository.updateTargetDate(any(), any()) }
    }

    @Test
    fun `should throw IllegalArgumentException when name is blank and targetDate is provided`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", "   ", LocalDate.of(2027, 1, 1))
        }
    }
}
