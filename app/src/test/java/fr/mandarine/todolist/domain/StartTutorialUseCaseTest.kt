package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class StartTutorialUseCaseTest {

    private lateinit var repository: TutorialStateRepository
    private lateinit var useCase: StartTutorialUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = StartTutorialUseCase(repository)
    }

    @Test
    fun `should mark tutorial seen when invoked`() {
        useCase()

        verify { repository.markTutorialSeen() }
    }
}
