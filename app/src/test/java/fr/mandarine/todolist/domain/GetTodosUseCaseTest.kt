package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTodosUseCaseTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: GetTodosUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetTodosUseCase(repository)
    }

    @Test
    fun `should return all items from repository`() {
        val items = listOf(TodoItem("1", "Item 1"), TodoItem("2", "Item 2"))
        every { repository.getAll() } returns items

        val result = useCase()

        assertEquals(items, result)
    }

    @Test
    fun `should return empty list when repository is empty`() {
        every { repository.getAll() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }
}
