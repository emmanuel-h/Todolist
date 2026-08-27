package fr.mandarine.todolist.ui.tutorial

import android.os.Looper
import androidx.compose.runtime.MonotonicFrameClock
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.domain.TutorialStep
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialStage
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialOverlayControllerTest {

    private val tutorialViewModel = mockk<TutorialViewModel>(relaxed = true)
    private val scope = CoroutineScope(SupervisorJob())
    private val controller = TutorialOverlayController(
        tutorialViewModel = tutorialViewModel,
        scope = scope,
        today = { TODAY },
        sceneContext = Dispatchers.Main + ImmediateFrameClock()
    )

    @After
    fun cancelScenes() {
        scope.cancel()
    }

    @Test
    fun `should show the overlay when the tutorial is ready to start on the lists screen`() {
        controller.handleState(TutorialUiState.ReadyToStart, FakeStage(TutorialScreen.LISTS))
        drain()

        assertTrue(controller.overlayState.visible)
    }

    @Test
    fun `should leave the overlay hidden on the items screen while the opening plays`() {
        controller.handleState(TutorialUiState.ReadyToStart, FakeStage(TutorialScreen.ITEMS))
        drain()

        assertFalse(controller.overlayState.visible)
    }

    @Test
    fun `should show the overlay again on replay after it was dismissed`() {
        val stage = FakeStage(TutorialScreen.LISTS)
        controller.handleState(TutorialUiState.ReadyToStart, stage)
        drain()
        controller.handleState(TutorialUiState.Dismissed, stage)
        drain()
        assertFalse(controller.overlayState.visible)

        controller.handleState(TutorialUiState.ReadyToStart, stage)
        drain()

        assertTrue(controller.overlayState.visible)
    }

    @Test
    fun `should show the overlay when a step brings the tutorial onto this screen`() {
        controller.handleState(
            TutorialUiState.Active(TutorialStep.WRITE_ITEMS),
            FakeStage(TutorialScreen.ITEMS)
        )

        assertTrue(controller.overlayState.visible)
    }

    @Test
    fun `should leave the overlay hidden for a step belonging to the other screen`() {
        controller.handleState(
            TutorialUiState.Active(TutorialStep.A_DAY_AND_A_NOTE),
            FakeStage(TutorialScreen.ITEMS)
        )
        drain()

        assertFalse(controller.overlayState.visible)
    }

    @Test
    fun `should fill one progress dot per step reached`() {
        val stage = FakeStage(TutorialScreen.LISTS)

        controller.handleState(TutorialUiState.Active(TutorialStep.TICK_AND_MOVE), stage)

        assertEquals(5, controller.overlayState.filledDots)
    }

    @Test
    fun `should empty the progress dots once the tutorial is hidden`() {
        val stage = FakeStage(TutorialScreen.LISTS)
        controller.handleState(TutorialUiState.Active(TutorialStep.WRITE_ITEMS), stage)

        controller.handleState(TutorialUiState.Hidden, stage)

        assertEquals(0, controller.overlayState.filledDots)
    }

    @Test
    fun `should dismiss the tutorial and fade the overlay away when skip is requested`() {
        val stage = FakeStage(TutorialScreen.LISTS)
        controller.handleState(TutorialUiState.ReadyToStart, stage)
        drain()

        controller.onSkipRequested(stage)
        drain()

        verify { tutorialViewModel.skip() }
        assertFalse(controller.overlayState.visible)
    }

    /**
     * The demo drives the reader's own controls, so the way out has to put them
     * back: a create row left open with the demo's name in it becomes the
     * reader's list on their very next tap.
     */
    @Test
    fun `should put the page back the way the demo found it when skip is requested`() {
        val stage = FakeStage(TutorialScreen.LISTS)
        controller.handleState(TutorialUiState.ReadyToStart, stage)
        drain()

        controller.onSkipRequested(stage)

        assertEquals(1, stage.abandoned)
    }

    private fun drain() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private class FakeStage(override val screen: TutorialScreen) : TutorialStage {
        var abandoned = 0

        override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = null
        override suspend fun perform(action: TutorialAction): Boolean = false
        override suspend fun awaitDemoListId(): String? = null
        override fun bannerContent(): TutorialBannerContent? = null
        override fun abandon() {
            abandoned += 1
        }
    }

    private class ImmediateFrameClock : MonotonicFrameClock {
        private var nanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            nanos += FRAME_NANOS
            return onFrame(nanos)
        }

        private companion object {
            const val FRAME_NANOS = 16_000_000L
        }
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 3, 14)
    }
}
