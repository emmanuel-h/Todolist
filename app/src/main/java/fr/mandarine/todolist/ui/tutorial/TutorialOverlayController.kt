package fr.mandarine.todolist.ui.tutorial

import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.domain.TutorialStep
import fr.mandarine.todolist.presentation.TutorialDirector
import fr.mandarine.todolist.presentation.TutorialStage
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import androidx.compose.ui.platform.AndroidUiDispatcher
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Scenes animate outside any composition, so they need a context that carries a
 * `MonotonicFrameClock` of its own — a bare `Dispatchers.Main` has none and every
 * `Animatable` in the overlay throws on its first frame.
 */
class TutorialOverlayController(
    private val tutorialViewModel: TutorialViewModel,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val sceneContext: CoroutineContext = AndroidUiDispatcher.Main
) {

    val overlayState = TutorialOverlayState()

    private var sceneJob: Job? = null

    fun onSkipRequested() {
        sceneJob?.cancel()
        sceneJob = null
        tutorialViewModel.skip()
        scope.launch(sceneContext) { overlayState.fadeOut() }
    }

    fun handleState(state: TutorialUiState, stage: TutorialStage) {
        overlayState.filledDots = filledDotsFor(state)
        val director = TutorialDirector(stage, overlayState, tutorialViewModel, today)
        when (state) {
            TutorialUiState.Hidden -> {}
            TutorialUiState.ReadyToStart -> {
                if (stage.screen == TutorialScreen.LISTS) {
                    launchScene {
                        overlayState.begin()
                        director.playOpening()
                    }
                }
            }
            is TutorialUiState.Active -> {
                if (entersScreen(state.step, stage.screen)) overlayState.show()
                launchScene { director.play(state.step) }
            }
            TutorialUiState.Dismissed -> launchScene { overlayState.fadeOut() }
        }
    }

    private fun entersScreen(step: TutorialStep, screen: TutorialScreen): Boolean =
        (step == TutorialStep.OPEN_LIST && screen == TutorialScreen.ITEMS) ||
            (step == TutorialStep.DELETE_LIST && screen == TutorialScreen.LISTS)

    private fun launchScene(block: suspend () -> Unit) {
        sceneJob?.cancel()
        sceneJob = scope.launch(sceneContext) { block() }
    }
}
