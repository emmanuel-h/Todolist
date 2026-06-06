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
    fun `should return all items for given list from repository`() {
        val items = listOf(TodoItem("1", "Item 1", "list-1"), TodoItem("2", "Item 2", "list-1"))
        every { repository.getAllByListId("list-1") } returns items

        val result = useCase("list-1")

        assertEquals(items, result)
    }

    @Test
    fun `should return empty list when repository has no items for given list`() {
        every { repository.getAllByListId("list-1") } returns emptyList()

        val result = useCase("list-1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should pass the listId to the repository`() {
        every { repository.getAllByListId("list-2") } returns listOf(TodoItem("3", "Item 3", "list-2"))

        val result = useCase("list-2")

        assertEquals(1, result.size)
        assertEquals("list-2", result.first().listId)
    }
}
