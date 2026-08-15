package fr.mandarine.todolist.domain

class ShouldRunTutorialUseCase(
    private val tutorialStateRepository: TutorialStateRepository
) {
    operator fun invoke(): Boolean = !tutorialStateRepository.isTutorialSeen()
}
