package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class EditTodoListUseCaseTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: EditTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = EditTodoListUseCase(repository)
    }

    @Test
    fun `should delegate updateName to repository with given id and name`() {
        useCase("list-1", "Groceries")

        verify { repository.updateName("list-1", "Groceries") }
    }

    @Test
    fun `should delegate updateName to repository with another id and name`() {
        useCase("list-42", "Work tasks")

        verify { repository.updateName("list-42", "Work tasks") }
    }

    @Test
    fun `should throw IllegalArgumentException when name is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", "   ")
        }
    }

    @Test
    fun `should throw IllegalArgumentException when name is empty`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase("list-1", "")
        }
    }

    @Test
    fun `should not call repository when name is blank`() {
        runCatching { useCase("list-1", "   ") }

        verify(exactly = 0) { repository.updateName(any(), any()) }
    }
}
