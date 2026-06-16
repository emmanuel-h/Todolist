package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateTodoListUseCaseTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: CreateTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = CreateTodoListUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should return list with correct name when name is valid`() {
        val result = useCase("Groceries")
        assertEquals("Groceries", result.name)
    }

    @Test
    fun `should return list with generated id when name is valid`() {
        val result = useCase("Groceries")
        assertEquals("fixed-id", result.id)
    }

    @Test
    fun `should add list to repository when name is valid`() {
        val result = useCase("Groceries")
        verify { repository.add(result) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw when name is blank`() {
        useCase("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw when name is empty`() {
        useCase("")
    }

    @Test
    fun `should produce non-blank id when using default id generator`() {
        val defaultUseCase = CreateTodoListUseCase(repository)
        val result = defaultUseCase("Groceries")
        assertTrue(result.id.isNotBlank())
    }

    @Test
    fun `should assign position zero when repository is empty`() {
        every { repository.getAll() } returns emptyList()
        val result = useCase("Groceries")
        assertEquals(0, result.position)
    }

    @Test
    fun `should assign position zero when repository has lists`() {
        every { repository.getAll() } returns listOf(
            TodoList("1", "First", 0),
            TodoList("2", "Second", 1)
        )
        val result = useCase("Third")
        assertEquals(0, result.position)
    }
}
