package fr.mandarine.todolist.domain

fun interface ListNotifier {
    fun postNotifications(notifications: List<ListNotification>)
}
