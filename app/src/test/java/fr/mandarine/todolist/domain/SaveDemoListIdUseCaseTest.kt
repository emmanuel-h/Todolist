package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class SaveDemoListIdUseCaseTest {

    private lateinit var repository: TutorialStateRepository
    private lateinit var useCase: SaveDemoListIdUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = SaveDemoListIdUseCase(repository)
    }

    @Test
    fun `should save the given id to repository when invoked`() {
        useCase("list-abc")

        verify { repository.savePendingDemoListId("list-abc") }
    }

    @Test
    fun `should pass the exact id to repository when invoked with different id`() {
        useCase("list-xyz")

        verify { repository.savePendingDemoListId("list-xyz") }
    }
}
