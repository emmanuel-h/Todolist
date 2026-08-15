package fr.mandarine.todolist.domain

class FinishTutorialUseCase(
    private val tutorialStateRepository: TutorialStateRepository
) {
    operator fun invoke() {
        tutorialStateRepository.clearPendingDemoListId()
    }
}
