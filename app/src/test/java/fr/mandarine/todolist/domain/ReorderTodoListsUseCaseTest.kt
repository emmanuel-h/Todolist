package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ReorderTodoListsUseCaseTest {

    private lateinit var repository: TodoListRepository
    private lateinit var getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase
    private lateinit var useCase: ReorderTodoListsUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        getTodoListsWithStatusUseCase = mockk()
        useCase = ReorderTodoListsUseCase(repository, getTodoListsWithStatusUseCase)
    }

    private fun summary(id: String, allDone: Boolean = false) =
        TodoListSummary(TodoList(id, "List $id"), allDone)

    @Test
    fun `should reorder with unchanged indices when all lists are active`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(summary("a"), summary("b"), summary("c"))

        useCase(0, 2)

        verify { repository.reorder(0, 2) }
    }

    @Test
    fun `should reorder upward with unchanged indices when all lists are active`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(summary("a"), summary("b"), summary("c"))

        useCase(2, 0)

        verify { repository.reorder(2, 0) }
    }

    @Test
    fun `should map active indices to global indices when a done list precedes active lists`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(
            summary("done", allDone = true), summary("a"), summary("b")
        )

        useCase(0, 1)

        verify { repository.reorder(1, 2) }
    }

    @Test
    fun `should map active indices to global indices when a done list sits between active lists`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(
            summary("a"), summary("done", allDone = true), summary("b")
        )

        useCase(1, 0)

        verify { repository.reorder(2, 0) }
    }

    @Test
    fun `should throw IllegalArgumentException when fromIndex points past the active lists`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(
            summary("a"), summary("done", allDone = true)
        )

        assertThrows(IllegalArgumentException::class.java) { useCase(1, 0) }
    }

    @Test
    fun `should throw IllegalArgumentException when toIndex points past the active lists`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(summary("a"), summary("b"))

        assertThrows(IllegalArgumentException::class.java) { useCase(0, 2) }
    }

    @Test
    fun `should not reorder when fromIndex is negative`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(summary("a"))

        runCatching { useCase(-1, 0) }

        verify(exactly = 0) { repository.reorder(-1, 0) }
    }

    @Test
    fun `should throw IllegalArgumentException when toIndex is negative`() {
        every { getTodoListsWithStatusUseCase() } returns listOf(summary("a"), summary("b"))

        assertThrows(IllegalArgumentException::class.java) { useCase(0, -1) }
    }
}
