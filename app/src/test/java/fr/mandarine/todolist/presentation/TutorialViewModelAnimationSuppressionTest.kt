package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CleanupAbandonedTutorialUseCase
import fr.mandarine.todolist.domain.FinishTutorialUseCase
import fr.mandarine.todolist.domain.SaveDemoListIdUseCase
import fr.mandarine.todolist.domain.ShouldRunTutorialUseCase
import fr.mandarine.todolist.domain.StartTutorialUseCase
import fr.mandarine.todolist.domain.TutorialScript
import fr.mandarine.todolist.domain.TutorialStep
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TutorialViewModelAnimationSuppressionTest {

    private lateinit var shouldRunTutorialUseCase: ShouldRunTutorialUseCase
    private lateinit var startTutorialUseCase: StartTutorialUseCase
    private lateinit var saveDemoListIdUseCase: SaveDemoListIdUseCase
    private lateinit var finishTutorialUseCase: FinishTutorialUseCase
    private lateinit var cleanupAbandonedTutorialUseCase: CleanupAbandonedTutorialUseCase
    private lateinit var viewModel: TutorialViewModel

    @Before
    fun setUp() {
        shouldRunTutorialUseCase = mockk()
        startTutorialUseCase = mockk(relaxed = true)
        saveDemoListIdUseCase = mockk(relaxed = true)
        finishTutorialUseCase = mockk(relaxed = true)
        cleanupAbandonedTutorialUseCase = mockk(relaxed = true)
        viewModel = TutorialViewModel(
            shouldRunTutorialUseCase,
            startTutorialUseCase,
            saveDemoListIdUseCase,
            finishTutorialUseCase,
            cleanupAbandonedTutorialUseCase,
            TutorialScript.defaultScript(),
            Dispatchers.Unconfined
        )
    }

    @Test
    fun `should not suppress animations when state is Hidden`() {
        assertFalse(viewModel.animationsSuppressed)
    }

    @Test
    fun `should not suppress animations when state is ReadyToStart`() {
        every { shouldRunTutorialUseCase() } returns true
        viewModel.initialize()

        assertFalse(viewModel.animationsSuppressed)
    }

    @Test
    fun `should not suppress animations when state is Dismissed`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()

        assertFalse(viewModel.animationsSuppressed)
    }

    @Test
    fun `should not suppress animations when tutorial is Active with CREATE_LIST step`() {
        viewModel.onDemoListCreated("list-1")

        assertFalse(viewModel.animationsSuppressed)
    }

    @Test
    fun `should not suppress animations when tutorial is Active with SET_DUE_DATE step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()

        assertFalse(viewModel.animationsSuppressed)
    }

    @Test
    fun `should suppress animations when tutorial is Active with OPEN_LIST step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()
        viewModel.advanceStep()

        assertTrue(viewModel.animationsSuppressed)
    }

    @Test
    fun `should suppress animations when tutorial is Active with COMPLETE_AND_REORDER step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()
        viewModel.advanceStep()
        viewModel.advanceStep()

        assertTrue(viewModel.animationsSuppressed)
    }

    @Test
    fun `should not suppress animations when tutorial is Active with DELETE_LIST step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()
        viewModel.advanceStep()
        viewModel.advanceStep()
        viewModel.advanceStep()

        assertFalse(viewModel.animationsSuppressed)
    }
}
