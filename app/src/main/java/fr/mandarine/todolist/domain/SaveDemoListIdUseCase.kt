package fr.mandarine.todolist.domain

class SaveDemoListIdUseCase(
    private val tutorialStateRepository: TutorialStateRepository
) {
    operator fun invoke(id: String) {
        tutorialStateRepository.savePendingDemoListId(id)
    }
}
