package fr.mandarine.todolist.domain

class CleanupAbandonedTutorialUseCase(
    private val tutorialStateRepository: TutorialStateRepository,
    private val deleteTodoListUseCase: DeleteTodoListUseCase
) {
    operator fun invoke() {
        val id = tutorialStateRepository.getPendingDemoListId() ?: return
        deleteTodoListUseCase(id)
        tutorialStateRepository.clearPendingDemoListId()
    }
}
