package fr.mandarine.todolist.domain

class StartTutorialUseCase(
    private val tutorialStateRepository: TutorialStateRepository
) {
    operator fun invoke() {
        tutorialStateRepository.markTutorialSeen()
    }
}
