package fr.mandarine.todolist.domain

sealed class ListNotification {
    abstract val list: TodoList

    fun notificationId(): Int = list.id.hashCode()

    data class DueDateToday(override val list: TodoList) : ListNotification()
    data class TargetDateTomorrow(override val list: TodoList) : ListNotification()
}
