package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CreateTodoListUseCaseTargetDateTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: CreateTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = CreateTodoListUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should create list with given target date when target date is provided`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        val result = useCase("Groceries", targetDate)

        assertEquals(targetDate, result.targetDate)
    }

    @Test
    fun `should create list with null target date when null is provided`() {
        val result = useCase("Groceries", null)

        assertNull(result.targetDate)
    }

    @Test
    fun `should use null as default target date when no target date is given`() {
        val result = useCase("Groceries")

        assertNull(result.targetDate)
    }

    @Test
    fun `should add list with target date to repository`() {
        val targetDate = LocalDate.of(2027, 6, 22)

        val result = useCase("Groceries", targetDate)

        verify { repository.addAtTop(result) }
    }

    @Test
    fun `should create list with another target date`() {
        val targetDate = LocalDate.of(2026, 1, 15)

        val result = useCase("Work", targetDate)

        assertEquals(targetDate, result.targetDate)
    }
}
