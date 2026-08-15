package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShouldRunTutorialUseCaseTest {

    private lateinit var repository: TutorialStateRepository
    private lateinit var useCase: ShouldRunTutorialUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = ShouldRunTutorialUseCase(repository)
    }

    @Test
    fun `should return true when tutorial has not been seen`() {
        every { repository.isTutorialSeen() } returns false

        assertTrue(useCase())
    }

    @Test
    fun `should return false when tutorial has been seen`() {
        every { repository.isTutorialSeen() } returns true

        assertFalse(useCase())
    }
}
