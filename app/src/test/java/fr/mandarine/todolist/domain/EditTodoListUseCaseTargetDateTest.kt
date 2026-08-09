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
    fun `should update with given target date when target date is provided`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        useCase("list-1", "Groceries", targetDate)

        verify { repository.update("list-1", "Groceries", targetDate, null) }
    }

    @Test
    fun `should update with null target date when target date is null`() {
        useCase("list-1", "Groceries", null)

        verify { repository.update("list-1", "Groceries", null, null) }
    }

    @Test
    fun `should update name and target date in a single repository call`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        useCase("list-1", "Groceries", targetDate)

        verify(exactly = 1) { repository.update("list-1", "Groceries", targetDate, null) }
    }

    @Test
    fun `should update with another id and date`() {
        val targetDate = LocalDate.of(2026, 1, 15)

        useCase("list-42", "Work", targetDate)

        verify { repository.update("list-42", "Work", targetDate, null) }
    }

    @Test
    fun `should not update when name is blank`() {
        val targetDate = LocalDate.of(2027, 1, 1)

        runCatching { useCase("list-1", "   ", targetDate) }

        verify(exactly = 0) { repository.update("list-1", "   ", targetDate, null) }
    }

    @Test
    fun `should throw IllegalArgumentException when name is blank and targetDate is provided`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", "   ", LocalDate.of(2027, 1, 1))
        }
    }
}
