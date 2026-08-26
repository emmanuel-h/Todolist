package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ReorderTodosUseCaseTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: ReorderTodosUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = ReorderTodosUseCase(repository)
    }

    @Test
    fun `should hand the named order to the repository with the list it happened on`() {
        useCase("list-1", listOf("a", "b", "c"))

        verify { repository.reorder("list-1", listOf("a", "b", "c")) }
    }

    @Test
    fun `should hand a reordered set of names through unchanged`() {
        useCase("list-99", listOf("c", "a", "b"))

        verify { repository.reorder("list-99", listOf("c", "a", "b")) }
    }

    @Test
    fun `should carry an order naming nothing`() {
        useCase("list-1", emptyList())

        verify { repository.reorder("list-1", emptyList()) }
    }

    @Test
    fun `should throw IllegalArgumentException when the list is not named`() {
        assertThrows(IllegalArgumentException::class.java) { useCase("", listOf("a")) }
    }

    @Test
    fun `should not reach the repository when the list is not named`() {
        runCatching { useCase("  ", listOf("a")) }

        verify(exactly = 0) { repository.reorder("  ", listOf("a")) }
    }

    @Test
    fun `should throw IllegalArgumentException when the same item is named twice`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", listOf("a", "b", "a"))
        }
    }

    @Test
    fun `should not reach the repository when the same item is named twice`() {
        runCatching { useCase("list-1", listOf("a", "a")) }

        verify(exactly = 0) { repository.reorder("list-1", listOf("a", "a")) }
    }
}
