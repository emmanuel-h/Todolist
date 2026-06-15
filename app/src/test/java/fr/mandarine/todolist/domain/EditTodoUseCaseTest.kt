package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class EditTodoUseCaseTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: EditTodoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = EditTodoUseCase(repository)
    }

    @Test
    fun `should delegate updateTitle to repository with given id and title`() {
        useCase("item-1", "New title")

        verify { repository.updateTitle("item-1", "New title") }
    }

    @Test
    fun `should delegate updateTitle to repository with another id and title`() {
        useCase("item-42", "Another title")

        verify { repository.updateTitle("item-42", "Another title") }
    }

    @Test
    fun `should throw IllegalArgumentException when title is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("item-1", "   ")
        }
    }

    @Test
    fun `should throw IllegalArgumentException when title is empty`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("item-1", "")
        }
    }

    @Test
    fun `should not call repository when title is blank`() {
        runCatching { useCase("item-1", "   ") }

        verify(exactly = 0) { repository.updateTitle(any(), any()) }
    }
}
