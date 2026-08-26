package fr.mandarine.todolist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.todolist.domain.CleanupAbandonedTutorialUseCase
import fr.mandarine.todolist.domain.FinishTutorialUseCase
import fr.mandarine.todolist.domain.SaveDemoListIdUseCase
import fr.mandarine.todolist.domain.ShouldRunTutorialUseCase
import fr.mandarine.todolist.domain.StartTutorialUseCase
import fr.mandarine.todolist.domain.TutorialScript
import fr.mandarine.todolist.domain.TutorialStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TutorialViewModel(
    private val shouldRunTutorialUseCase: ShouldRunTutorialUseCase,
    private val startTutorialUseCase: StartTutorialUseCase,
    private val saveDemoListIdUseCase: SaveDemoListIdUseCase,
    private val finishTutorialUseCase: FinishTutorialUseCase,
    private val cleanupAbandonedTutorialUseCase: CleanupAbandonedTutorialUseCase,
    private val tutorialScript: TutorialScript,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow<TutorialUiState>(TutorialUiState.Hidden)
    val uiState: StateFlow<TutorialUiState> = _uiState

    private var currentStepIndex: Int = -1

    /**
     * Called from every onCreate, and a rotation is another one of those. The
     * first act here is to tear off an abandoned demo list — which, mid-tour, is
     * the demo list currently being written on. Turning the device sideways
     * deleted the tour out from under itself and then declared it over, because
     * the seen flag is written at the opening beat.
     *
     * A tour already on the paper is left alone. A tour that is over is not: the
     * window is rebuilt far more often than the process is, so a demo list that
     * outlived its tour would never have been swept up while the app stayed
     * alive, and the reader would keep finding it.
     */
    fun initialize() {
        if (tourIsOnThePaper()) return
        viewModelScope.launch(dispatcher) {
            cleanupAbandonedTutorialUseCase()
            if (shouldRunTutorialUseCase()) {
                startTutorialUseCase()
                _uiState.value = TutorialUiState.ReadyToStart
            } else {
                _uiState.value = TutorialUiState.Dismissed
            }
        }
    }

    private fun tourIsOnThePaper(): Boolean {
        val current = _uiState.value
        return current is TutorialUiState.Active || current is TutorialUiState.ReadyToStart
    }

    fun onDemoListCreated(listId: String) {
        saveDemoListIdUseCase(listId)
        currentStepIndex = 0
        _uiState.value = TutorialUiState.Active(tutorialScript.steps[currentStepIndex])
    }

    fun advanceStep() {
        if (_uiState.value !is TutorialUiState.Active) return
        val nextIndex = currentStepIndex + 1
        val nextStep = tutorialScript.steps.getOrNull(nextIndex)
        if (nextStep != null) {
            currentStepIndex = nextIndex
            _uiState.value = TutorialUiState.Active(nextStep)
        } else {
            viewModelScope.launch(dispatcher) {
                finishTutorialUseCase()
                _uiState.value = TutorialUiState.Dismissed
            }
        }
    }

    val animationsSuppressed: Boolean
        get() {
            val s = _uiState.value
            return s is TutorialUiState.Active &&
                (s.step == TutorialStep.OPEN_LIST || s.step == TutorialStep.COMPLETE_AND_REORDER)
        }

    fun replay() {
        val current = _uiState.value
        if (current is TutorialUiState.ReadyToStart || current is TutorialUiState.Active) return
        _uiState.value = TutorialUiState.ReadyToStart
    }

    fun skip() {
        viewModelScope.launch(dispatcher) {
            cleanupAbandonedTutorialUseCase()
            _uiState.value = TutorialUiState.Dismissed
        }
    }
}
