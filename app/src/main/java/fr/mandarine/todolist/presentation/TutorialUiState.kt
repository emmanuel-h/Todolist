package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TutorialStep

sealed class TutorialUiState {
    data object Hidden : TutorialUiState()
    data object ReadyToStart : TutorialUiState()
    data class Active(val step: TutorialStep) : TutorialUiState()
    data object Dismissed : TutorialUiState()
}
