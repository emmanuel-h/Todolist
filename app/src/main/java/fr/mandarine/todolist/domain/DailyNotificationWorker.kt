package fr.mandarine.todolist.domain

class DailyNotificationWorker(
    private val todoListRepository: TodoListRepository,
    private val computeUseCase: ComputePendingNotificationsUseCase,
    private val listNotifier: ListNotifier,
    private val scheduler: NotificationScheduler
) {
    fun execute() {
        val lists = todoListRepository.getAll()
        val notifications = computeUseCase(lists)
        listNotifier.postNotifications(notifications)
        scheduler.scheduleNextDailyCheck()
    }
}
