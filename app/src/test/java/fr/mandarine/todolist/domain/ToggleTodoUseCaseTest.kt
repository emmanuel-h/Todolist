package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ToggleTodoUseCaseTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: ToggleTodoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = ToggleTodoUseCase(repository)
    }

    @Test
    fun `should delegate toggle to repository with given id`() {
        useCase("item-1")
        verify { repository.toggle("item-1") }
    }

    @Test
    fun `should delegate toggle to repository with different id`() {
        useCase("item-42")
        verify { repository.toggle("item-42") }
    }
}
