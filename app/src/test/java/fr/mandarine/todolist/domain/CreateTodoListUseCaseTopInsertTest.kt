package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CreateTodoListUseCaseTopInsertTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: CreateTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = CreateTodoListUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should assign position zero when repository is empty`() {
        every { repository.getAll() } returns emptyList()
        val result = useCase("Groceries")
        assertEquals(0, result.position)
    }

    @Test
    fun `should assign position zero when repository already has lists`() {
        every { repository.getAll() } returns listOf(
            TodoList("1", "First", 0),
            TodoList("2", "Second", 1)
        )
        val result = useCase("Third")
        assertEquals(0, result.position)
    }

    @Test
    fun `should call shiftAllPositionsUp before add when repository has existing lists`() {
        every { repository.getAll() } returns listOf(TodoList("1", "First", 0))
        val result = useCase("New")
        verifyOrder {
            repository.shiftAllPositionsUp()
            repository.add(result)
        }
    }

    @Test
    fun `should call shiftAllPositionsUp before add when repository is empty`() {
        every { repository.getAll() } returns emptyList()
        val result = useCase("New")
        verifyOrder {
            repository.shiftAllPositionsUp()
            repository.add(result)
        }
    }
}
