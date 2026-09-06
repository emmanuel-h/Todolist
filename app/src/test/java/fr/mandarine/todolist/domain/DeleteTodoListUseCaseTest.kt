package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DeleteTodoListUseCaseTest {

    private lateinit var todoListRepository: TodoListRepository
    private lateinit var useCase: DeleteTodoListUseCase

    @Before
    fun setUp() {
        todoListRepository = mockk(relaxed = true)
        useCase = DeleteTodoListUseCase(todoListRepository)
    }

    /**
     * The items used to be deleted here, in a second statement the list delete could
     * be torn away from. They go with the list in one transaction now, so what this
     * use case owes is the one call.
     */
    @Test
    fun `should delete the named list when invoked`() {
        useCase("list-1")

        verify { todoListRepository.delete("list-1") }
    }

    @Test
    fun `should delete the list it was given when invoked`() {
        useCase("list-2")

        verify { todoListRepository.delete("list-2") }
    }
}
