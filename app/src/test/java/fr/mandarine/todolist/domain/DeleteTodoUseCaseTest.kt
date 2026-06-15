package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DeleteTodoUseCaseTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: DeleteTodoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = DeleteTodoUseCase(repository)
    }

    @Test
    fun `should delegate delete to repository with given id`() {
        useCase("item-1")

        verify { repository.delete("item-1") }
    }

    @Test
    fun `should delegate delete to repository with another id`() {
        useCase("item-99")

        verify { repository.delete("item-99") }
    }
}
