package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class FinishTutorialUseCaseTest {

    private lateinit var repository: TutorialStateRepository
    private lateinit var useCase: FinishTutorialUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = FinishTutorialUseCase(repository)
    }

    @Test
    fun `should clear pending demo list id when invoked`() {
        useCase()

        verify { repository.clearPendingDemoListId() }
    }
}
