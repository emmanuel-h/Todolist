package fr.mandarine.todolist.domain

sealed class ListNotification {
    abstract val list: TodoList

    data class DueDateToday(override val list: TodoList) : ListNotification()
    data class TargetDateTomorrow(override val list: TodoList) : ListNotification()
}
