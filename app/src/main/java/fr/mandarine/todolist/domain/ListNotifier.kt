package fr.mandarine.todolist.domain

/**
 * Posts the notifications the app has decided on. Implemented by
 * [fr.mandarine.todolist.data.AndroidListNotifier].
 *
 * `fun interface` is a Kotlin SAM interface: because it declares exactly one
 * abstract method, a caller can pass a lambda where one is expected
 * (`ListNotifier { notifications -> ... }`) instead of writing an object
 * expression. Handy in tests.
 */
fun interface ListNotifier {
    fun postNotifications(notifications: List<ListNotification>)
}
