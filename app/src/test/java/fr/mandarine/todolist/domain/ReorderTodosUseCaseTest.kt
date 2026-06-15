package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
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
    fun `should delegate reorder to repository with given listId fromIndex and toIndex`() {
        useCase("list-1", 0, 2)

        verify { repository.reorder("list-1", 0, 2) }
    }

    @Test
    fun `should delegate reorder to repository with another listId`() {
        useCase("list-99", 1, 3)

        verify { repository.reorder("list-99", 1, 3) }
    }

    @Test
    fun `should delegate reorder when moving item downward`() {
        useCase("list-1", 2, 0)

        verify { repository.reorder("list-1", 2, 0) }
    }
}
