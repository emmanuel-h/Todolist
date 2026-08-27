package fr.mandarine.todolist.domain

/**
 * What just happened to the page, for whatever wants to draw it happening.
 *
 * [ListCompleted] is the tick that emptied the list, and it comes *after* the
 * [ItemCompleted] for that same tick rather than instead of it — a tick is still a
 * tick when it is the last one. It carries the item it was written on, because the
 * page wants to celebrate from where the reader's finger was, not from the middle
 * of the sheet.
 */
sealed class AnimationEvent {
    data class ItemAdded(val itemId: String) : AnimationEvent()
    data class ItemCompleted(val itemId: String) : AnimationEvent()
    data class ItemRestored(val itemId: String) : AnimationEvent()
    data class ItemDeleted(val itemId: String) : AnimationEvent()
    data class ListCompleted(val lastItemId: String) : AnimationEvent()
    data object ListAdded : AnimationEvent()
}
