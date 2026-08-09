package fr.mandarine.todolist.domain

class DailyNotificationWorker(
    private val todoListRepository: TodoListRepository,
    private val computeUseCase: ComputePendingNotificationsUseCase,
    private val listNotifier: ListNotifier
) {
    fun execute() {
        val lists = todoListRepository.getAll()
        listNotifier.postNotifications(computeUseCase(lists))
    }
}
