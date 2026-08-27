package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CleanupAbandonedTutorialUseCase
import fr.mandarine.todolist.domain.FinishTutorialUseCase
import fr.mandarine.todolist.domain.SaveDemoListIdUseCase
import fr.mandarine.todolist.domain.ShouldRunTutorialUseCase
import fr.mandarine.todolist.domain.StartTutorialUseCase
import fr.mandarine.todolist.domain.TutorialScript
import fr.mandarine.todolist.domain.TutorialStep
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TutorialViewModelTest {

    private lateinit var shouldRunTutorialUseCase: ShouldRunTutorialUseCase
    private lateinit var startTutorialUseCase: StartTutorialUseCase
    private lateinit var saveDemoListIdUseCase: SaveDemoListIdUseCase
    private lateinit var finishTutorialUseCase: FinishTutorialUseCase
    private lateinit var cleanupAbandonedTutorialUseCase: CleanupAbandonedTutorialUseCase
    private lateinit var tutorialScript: TutorialScript
    private lateinit var viewModel: TutorialViewModel

    @Before
    fun setUp() {
        shouldRunTutorialUseCase = mockk()
        startTutorialUseCase = mockk(relaxed = true)
        saveDemoListIdUseCase = mockk(relaxed = true)
        finishTutorialUseCase = mockk(relaxed = true)
        cleanupAbandonedTutorialUseCase = mockk(relaxed = true)
        tutorialScript = TutorialScript.defaultScript()
        viewModel = TutorialViewModel(
            shouldRunTutorialUseCase,
            startTutorialUseCase,
            saveDemoListIdUseCase,
            finishTutorialUseCase,
            cleanupAbandonedTutorialUseCase,
            tutorialScript,
            Dispatchers.Unconfined
        )
    }

    @Test
    fun `should start with hidden state`() {
        assertEquals(TutorialUiState.Hidden, viewModel.uiState.value)
    }

    @Test
    fun `should dismiss when tutorial has already been seen`() {
        every { shouldRunTutorialUseCase() } returns false

        viewModel.initialize()

        assertEquals(TutorialUiState.Dismissed, viewModel.uiState.value)
    }

    @Test
    fun `should transition to ready to start when tutorial has not been seen`() {
        every { shouldRunTutorialUseCase() } returns true

        viewModel.initialize()

        assertEquals(TutorialUiState.ReadyToStart, viewModel.uiState.value)
    }

    /**
     * Every onCreate calls initialize, and a rotation is another one of those.
     * Cleanup tears off an abandoned demo list — which, mid-tour, is the list the
     * tour is being written on.
     */
    @Test
    fun `should not clean up again when a tour is already on the paper`() {
        every { shouldRunTutorialUseCase() } returns true
        viewModel.initialize()
        clearMocks(cleanupAbandonedTutorialUseCase, answers = false)

        viewModel.initialize()

        verify(exactly = 0) { cleanupAbandonedTutorialUseCase() }
    }

    @Test
    fun `should leave a running tour running when the window is rebuilt`() {
        every { shouldRunTutorialUseCase() } returns true
        viewModel.initialize()
        viewModel.onDemoListCreated("demo-1")

        viewModel.initialize()

        assertTrue(viewModel.uiState.value is TutorialUiState.Active)
    }

    @Test
    fun `should leave a dismissed tour dismissed when the window is rebuilt`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()

        viewModel.initialize()

        assertEquals(TutorialUiState.Dismissed, viewModel.uiState.value)
    }

    /**
     * The window is rebuilt far more often than the process is, and a demo list
     * that outlived its own tour has to be found by somebody. Skipping the sweep
     * once the tour was over left it on the page for as long as the app stayed
     * alive, which is where the reader kept finding a list they never wrote.
     */
    @Test
    fun `should sweep up an abandoned demo list when the window is rebuilt after the tour`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()
        clearMocks(cleanupAbandonedTutorialUseCase, answers = false)

        viewModel.initialize()

        verify { cleanupAbandonedTutorialUseCase() }
    }

    @Test
    fun `should call cleanup before checking whether tutorial should run`() {
        every { shouldRunTutorialUseCase() } returns false

        viewModel.initialize()

        verifyOrder {
            cleanupAbandonedTutorialUseCase()
            shouldRunTutorialUseCase()
        }
    }

    @Test
    fun `should call start tutorial when tutorial has not been seen`() {
        every { shouldRunTutorialUseCase() } returns true

        viewModel.initialize()

        verify { startTutorialUseCase() }
    }

    @Test
    fun `should not call start tutorial when tutorial has already been seen`() {
        every { shouldRunTutorialUseCase() } returns false

        viewModel.initialize()

        verify(exactly = 0) { startTutorialUseCase() }
    }

    @Test
    fun `should transition to active state with CREATE_LIST step when onDemoListCreated is called`() {
        viewModel.onDemoListCreated("list-1")

        assertEquals(TutorialUiState.Active(TutorialStep.A_DAY_AND_A_NOTE), viewModel.uiState.value)
    }

    @Test
    fun `should save demo list id when onDemoListCreated is called`() {
        viewModel.onDemoListCreated("list-demo-1")

        verify { saveDemoListIdUseCase("list-demo-1") }
    }

    @Test
    fun `should save the exact id passed to onDemoListCreated`() {
        viewModel.onDemoListCreated("list-xyz")

        verify { saveDemoListIdUseCase("list-xyz") }
    }

    @Test
    fun `should advance from CREATE_LIST to SET_DUE_DATE step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()

        assertEquals(TutorialUiState.Active(TutorialStep.OPEN_IT), viewModel.uiState.value)
    }

    @Test
    fun `should advance from SET_DUE_DATE to OPEN_LIST step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()
        viewModel.advanceStep()

        assertEquals(TutorialUiState.Active(TutorialStep.WRITE_ITEMS), viewModel.uiState.value)
    }

    @Test
    fun `should advance from OPEN_LIST to COMPLETE_AND_REORDER step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()
        viewModel.advanceStep()
        viewModel.advanceStep()

        assertEquals(TutorialUiState.Active(TutorialStep.TICK_AND_MOVE), viewModel.uiState.value)
    }

    @Test
    fun `should advance from COMPLETE_AND_REORDER to DELETE_LIST step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()
        viewModel.advanceStep()
        viewModel.advanceStep()
        viewModel.advanceStep()

        assertEquals(TutorialUiState.Active(TutorialStep.EDIT_AND_TEAR), viewModel.uiState.value)
    }

    @Test
    fun `should transition to dismissed after the last step is advanced`() {
        viewModel.onDemoListCreated("list-1")
        repeat(tutorialScript.steps.size) { viewModel.advanceStep() }

        assertEquals(TutorialUiState.Dismissed, viewModel.uiState.value)
    }

    @Test
    fun `should call finishTutorial when last step is advanced`() {
        viewModel.onDemoListCreated("list-1")
        repeat(tutorialScript.steps.size) { viewModel.advanceStep() }

        verify { finishTutorialUseCase() }
    }

    @Test
    fun `should not call finishTutorial when advancing from a non-last step`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()

        verify(exactly = 0) { finishTutorialUseCase() }
    }

    @Test
    fun `should be no-op when advanceStep is called and state is Hidden`() {
        viewModel.advanceStep()

        assertEquals(TutorialUiState.Hidden, viewModel.uiState.value)
        verify(exactly = 0) { finishTutorialUseCase() }
    }

    @Test
    fun `should be no-op when advanceStep is called and state is ReadyToStart`() {
        every { shouldRunTutorialUseCase() } returns true
        viewModel.initialize()

        viewModel.advanceStep()

        assertEquals(TutorialUiState.ReadyToStart, viewModel.uiState.value)
    }

    @Test
    fun `should be no-op when advanceStep is called and state is Dismissed`() {
        viewModel.onDemoListCreated("list-1")
        repeat(tutorialScript.steps.size) { viewModel.advanceStep() }

        viewModel.advanceStep()

        assertEquals(TutorialUiState.Dismissed, viewModel.uiState.value)
        verify(exactly = 1) { finishTutorialUseCase() }
    }

    @Test
    fun `should transition to dismissed when skip is called`() {
        viewModel.skip()

        assertEquals(TutorialUiState.Dismissed, viewModel.uiState.value)
    }

    @Test
    fun `should call cleanup when skip is called`() {
        viewModel.skip()

        verify { cleanupAbandonedTutorialUseCase() }
    }

    @Test
    fun `should call cleanup on skip even when tutorial is active`() {
        viewModel.onDemoListCreated("list-1")
        viewModel.advanceStep()

        viewModel.skip()

        verify(atLeast = 1) { cleanupAbandonedTutorialUseCase() }
    }

    @Test
    fun `should use custom script steps when provided`() {
        val customScript = TutorialScript(listOf(TutorialStep.WRITE_ITEMS, TutorialStep.EDIT_AND_TEAR))
        val vm = TutorialViewModel(
            shouldRunTutorialUseCase,
            startTutorialUseCase,
            saveDemoListIdUseCase,
            finishTutorialUseCase,
            cleanupAbandonedTutorialUseCase,
            customScript,
            Dispatchers.Unconfined
        )

        vm.onDemoListCreated("list-1")

        assertEquals(TutorialUiState.Active(TutorialStep.WRITE_ITEMS), vm.uiState.value)
    }

    @Test
    fun `should advance to second step of custom script correctly`() {
        val customScript = TutorialScript(listOf(TutorialStep.WRITE_ITEMS, TutorialStep.EDIT_AND_TEAR))
        val vm = TutorialViewModel(
            shouldRunTutorialUseCase,
            startTutorialUseCase,
            saveDemoListIdUseCase,
            finishTutorialUseCase,
            cleanupAbandonedTutorialUseCase,
            customScript,
            Dispatchers.Unconfined
        )
        vm.onDemoListCreated("list-1")

        vm.advanceStep()

        assertEquals(TutorialUiState.Active(TutorialStep.EDIT_AND_TEAR), vm.uiState.value)
    }

    @Test
    fun `should dismiss after single step custom script is advanced`() {
        val customScript = TutorialScript(listOf(TutorialStep.A_DAY_AND_A_NOTE))
        val vm = TutorialViewModel(
            shouldRunTutorialUseCase,
            startTutorialUseCase,
            saveDemoListIdUseCase,
            finishTutorialUseCase,
            cleanupAbandonedTutorialUseCase,
            customScript,
            Dispatchers.Unconfined
        )
        vm.onDemoListCreated("list-1")

        vm.advanceStep()

        assertEquals(TutorialUiState.Dismissed, vm.uiState.value)
    }

    @Test
    fun `should transition to ReadyToStart when replay is called and state is Hidden`() {
        viewModel.replay()

        assertEquals(TutorialUiState.ReadyToStart, viewModel.uiState.value)
    }

    @Test
    fun `should transition to ReadyToStart when replay is called and state is Dismissed`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()

        viewModel.replay()

        assertEquals(TutorialUiState.ReadyToStart, viewModel.uiState.value)
    }

    @Test
    fun `should be no-op when replay is called and state is ReadyToStart`() {
        every { shouldRunTutorialUseCase() } returns true
        viewModel.initialize()

        viewModel.replay()

        assertEquals(TutorialUiState.ReadyToStart, viewModel.uiState.value)
    }

    @Test
    fun `should be no-op when replay is called and state is Active`() {
        viewModel.onDemoListCreated("list-1")

        viewModel.replay()

        assertEquals(TutorialUiState.Active(TutorialStep.A_DAY_AND_A_NOTE), viewModel.uiState.value)
    }

    @Test
    fun `should not call shouldRunTutorialUseCase when replay is called`() {
        viewModel.replay()

        verify(exactly = 0) { shouldRunTutorialUseCase() }
    }

    @Test
    fun `should not call startTutorialUseCase when replay is called`() {
        viewModel.replay()

        verify(exactly = 0) { startTutorialUseCase() }
    }

    @Test
    fun `should not call shouldRunTutorialUseCase when replay is called from Dismissed`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()

        viewModel.replay()

        verify(exactly = 1) { shouldRunTutorialUseCase() }
    }

    @Test
    fun `should not call startTutorialUseCase when replay is called from Dismissed`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()

        viewModel.replay()

        verify(exactly = 0) { startTutorialUseCase() }
    }

    @Test
    fun `should allow tour to finish normally after replay`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()
        viewModel.replay()
        viewModel.onDemoListCreated("list-replay")
        repeat(tutorialScript.steps.size) { viewModel.advanceStep() }

        assertEquals(TutorialUiState.Dismissed, viewModel.uiState.value)
    }

    @Test
    fun `should call finishTutorial when last step is advanced after replay`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()
        viewModel.replay()
        viewModel.onDemoListCreated("list-replay")
        repeat(tutorialScript.steps.size) { viewModel.advanceStep() }

        verify(exactly = 1) { finishTutorialUseCase() }
    }

    @Test
    fun `should allow skip to work normally after replay`() {
        every { shouldRunTutorialUseCase() } returns false
        viewModel.initialize()
        viewModel.replay()
        viewModel.onDemoListCreated("list-replay")

        viewModel.skip()

        assertEquals(TutorialUiState.Dismissed, viewModel.uiState.value)
    }
}
