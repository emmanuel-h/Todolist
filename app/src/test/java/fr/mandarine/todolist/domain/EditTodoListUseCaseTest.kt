package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class EditTodoListUseCaseTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: EditTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = EditTodoListUseCase(repository)
    }

    @Test
    fun `should delegate update to repository with given id and name`() {
        useCase("list-1", "Groceries", null)

        verify { repository.update("list-1", "Groceries", null, null, ListColour.None) }
    }

    @Test
    fun `should delegate update to repository with another id and name`() {
        useCase("list-42", "Work tasks", null)

        verify { repository.update("list-42", "Work tasks", null, null, ListColour.None) }
    }

    @Test
    fun `should throw IllegalArgumentException when name is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", "   ", null)
        }
    }

    @Test
    fun `should throw IllegalArgumentException when name is empty`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", "", null)
        }
    }

    @Test
    fun `should not call repository when name is blank`() {
        runCatching { useCase("list-1", "   ", null) }

        verify(exactly = 0) { repository.update("list-1", "   ", null, null, ListColour.None) }
    }
}
