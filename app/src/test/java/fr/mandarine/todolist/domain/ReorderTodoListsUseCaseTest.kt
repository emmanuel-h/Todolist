package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ReorderTodoListsUseCaseTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: ReorderTodoListsUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = ReorderTodoListsUseCase(repository)
    }

    @Test
    fun `should hand the named order to the repository`() {
        useCase(listOf("a", "b", "c"))

        verify { repository.reorder(listOf("a", "b", "c")) }
    }

    @Test
    fun `should hand a reordered set of names through unchanged`() {
        useCase(listOf("c", "a", "b"))

        verify { repository.reorder(listOf("c", "a", "b")) }
    }

    @Test
    fun `should carry an order naming a single list`() {
        useCase(listOf("only"))

        verify { repository.reorder(listOf("only")) }
    }

    @Test
    fun `should carry an order naming nothing`() {
        useCase(emptyList())

        verify { repository.reorder(emptyList()) }
    }

    @Test
    fun `should throw IllegalArgumentException when the same list is named twice`() {
        assertThrows(IllegalArgumentException::class.java) { useCase(listOf("a", "b", "a")) }
    }

    @Test
    fun `should not reach the repository when the same list is named twice`() {
        runCatching { useCase(listOf("a", "a")) }

        verify(exactly = 0) { repository.reorder(listOf("a", "a")) }
    }
}
