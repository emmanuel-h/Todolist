package fr.mandarine.todolist.domain

fun interface NotificationScheduler {
    fun scheduleNextDailyCheck()
}
