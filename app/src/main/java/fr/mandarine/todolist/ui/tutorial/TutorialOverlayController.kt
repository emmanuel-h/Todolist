package fr.mandarine.todolist.ui.tutorial

import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.domain.TutorialStep
import fr.mandarine.todolist.presentation.TutorialDirector
import fr.mandarine.todolist.presentation.TutorialPace
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

    val pace = TutorialPace()
    val overlayState = TutorialOverlayState(pace)

    private var sceneJob: Job? = null
    private var playing: Pair<TutorialUiState, TutorialScreen>? = null

    /**
     * A reader who has understood the beat being played is not made to sit
     * through the rest of it. The scene keeps every action and drops every rest,
     * so the page arrives at the next scene exactly as it would have.
     */
    fun onNextRequested() {
        pace.hurry()
    }

    fun onSkipRequested(stage: TutorialStage) {
        sceneJob?.cancel()
        sceneJob = null
        stage.abandon()
        tutorialViewModel.skip()
        scope.launch(sceneContext) { overlayState.fadeOut() }
    }

    /**
     * A scene is a step played on a page, and the same beat may be handed in twice
     * — the step changes and the page it is played on catches up a frame later.
     * Playing it again would cancel the scene that is already running it, so a beat
     * already being played is left alone.
     */
    fun handleState(state: TutorialUiState, stage: TutorialStage) {
        val beat = state to stage.screen
        if (playing == beat) return
        playing = beat
        overlayState.filledDots = filledDotsFor(state)
        val director = TutorialDirector(stage, overlayState, tutorialViewModel, pace, today)
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
        pace.settle()
        sceneJob = scope.launch(sceneContext) { block() }
    }
}
