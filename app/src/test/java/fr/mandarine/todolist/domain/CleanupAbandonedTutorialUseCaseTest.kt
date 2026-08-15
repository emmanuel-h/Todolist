package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test

class CleanupAbandonedTutorialUseCaseTest {

    private lateinit var repository: TutorialStateRepository
    private lateinit var deleteTodoListUseCase: DeleteTodoListUseCase
    private lateinit var useCase: CleanupAbandonedTutorialUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        deleteTodoListUseCase = mockk(relaxed = true)
        useCase = CleanupAbandonedTutorialUseCase(repository, deleteTodoListUseCase)
    }

    @Test
    fun `should delete list and clear id when pending id exists`() {
        every { repository.getPendingDemoListId() } returns "list-demo-1"

        useCase()

        verifyOrder {
            deleteTodoListUseCase("list-demo-1")
            repository.clearPendingDemoListId()
        }
    }

    @Test
    fun `should delete the exact list id returned by repository`() {
        every { repository.getPendingDemoListId() } returns "list-abc"

        useCase()

        verify { deleteTodoListUseCase("list-abc") }
    }

    @Test
    fun `should not delete any list when no pending id exists`() {
        every { repository.getPendingDemoListId() } returns null

        useCase()

        verify(exactly = 0) { deleteTodoListUseCase(any()) }
    }

    @Test
    fun `should not clear id when no pending id exists`() {
        every { repository.getPendingDemoListId() } returns null

        useCase()

        verify(exactly = 0) { repository.clearPendingDemoListId() }
    }
}
