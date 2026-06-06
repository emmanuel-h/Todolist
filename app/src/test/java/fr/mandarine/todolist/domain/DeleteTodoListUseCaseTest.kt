package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test

class DeleteTodoListUseCaseTest {

    private lateinit var todoListRepository: TodoListRepository
    private lateinit var todoRepository: TodoRepository
    private lateinit var useCase: DeleteTodoListUseCase

    @Before
    fun setUp() {
        todoListRepository = mockk(relaxed = true)
        todoRepository = mockk(relaxed = true)
        useCase = DeleteTodoListUseCase(todoListRepository, todoRepository)
    }

    @Test
    fun `should delete items before deleting list when invoked`() {
        useCase("list-1")

        verifyOrder {
            todoRepository.deleteAllByListId("list-1")
            todoListRepository.delete("list-1")
        }
    }

    @Test
    fun `should delete items for the correct list`() {
        useCase("list-2")

        verifyOrder {
            todoRepository.deleteAllByListId("list-2")
            todoListRepository.delete("list-2")
        }
    }
}
