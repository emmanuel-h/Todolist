package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddTodoUseCaseTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: AddTodoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = AddTodoUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should return item with correct title when title is valid`() {
        val result = useCase("Buy milk")
        assertEquals("Buy milk", result.title)
    }

    @Test
    fun `should return item with generated id when title is valid`() {
        val result = useCase("Buy milk")
        assertEquals("fixed-id", result.id)
    }

    @Test
    fun `should add item to repository when title is valid`() {
        val result = useCase("Buy milk")
        verify { repository.add(result) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw when title is blank`() {
        useCase("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw when title is empty`() {
        useCase("")
    }

    @Test
    fun `should produce non-blank id when using default id generator`() {
        val defaultUseCase = AddTodoUseCase(repository)
        val result = defaultUseCase("Default id test")
        assertTrue(result.id.isNotBlank())
    }
}
