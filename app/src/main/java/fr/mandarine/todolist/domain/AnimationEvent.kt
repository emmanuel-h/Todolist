package fr.mandarine.todolist.domain

sealed class AnimationEvent {
    data class ItemAdded(val itemId: String) : AnimationEvent()
    data class ItemCompleted(val itemId: String) : AnimationEvent()
    data class ItemRestored(val itemId: String) : AnimationEvent()
    data class ItemDeleted(val itemId: String) : AnimationEvent()
    data object ListAdded : AnimationEvent()
}
