package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
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
    fun `should delegate reorder to repository with given fromIndex and toIndex`() {
        useCase(0, 2)

        verify { repository.reorder(0, 2) }
    }

    @Test
    fun `should delegate reorder to repository with another pair of indices`() {
        useCase(1, 3)

        verify { repository.reorder(1, 3) }
    }

    @Test
    fun `should delegate reorder when moving item upward`() {
        useCase(2, 0)

        verify { repository.reorder(2, 0) }
    }
}
