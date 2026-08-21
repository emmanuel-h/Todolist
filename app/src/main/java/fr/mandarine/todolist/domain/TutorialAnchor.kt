package fr.mandarine.todolist.domain

sealed class TutorialAnchor {
    data object CreateListButton : TutorialAnchor()
    data object ListNameField : TutorialAnchor()
    data object ListCreateRow : TutorialAnchor()
    data object TargetDateButton : TutorialAnchor()
    data object DueDateButton : TutorialAnchor()
    data object SubmitListButton : TutorialAnchor()
    data object FirstListRow : TutorialAnchor()
    data object DeleteListButton : TutorialAnchor()
    data object ConfirmDeleteButton : TutorialAnchor()
    data object ItemGhostRow : TutorialAnchor()
    data object SubmitItemButton : TutorialAnchor()
    data class ActiveItemToggle(val index: Int) : TutorialAnchor()
    data class CompletedItemToggle(val index: Int) : TutorialAnchor()
    data class ActiveItemDragHandle(val index: Int) : TutorialAnchor()
    data class ActiveItemRow(val index: Int) : TutorialAnchor()
}
