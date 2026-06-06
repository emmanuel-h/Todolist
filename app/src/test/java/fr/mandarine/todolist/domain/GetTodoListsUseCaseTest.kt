package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTodoListsUseCaseTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: GetTodoListsUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetTodoListsUseCase(repository)
    }

    @Test
    fun `should return all lists from repository`() {
        val lists = listOf(TodoList("1", "Groceries"), TodoList("2", "Home"))
        every { repository.getAll() } returns lists

        val result = useCase()

        assertEquals(lists, result)
    }

    @Test
    fun `should return empty list when repository has no lists`() {
        every { repository.getAll() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }
}
