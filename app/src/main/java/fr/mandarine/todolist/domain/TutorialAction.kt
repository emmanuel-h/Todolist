package fr.mandarine.todolist.domain

import java.time.LocalDate

sealed class TutorialAction {
    data object OpenListCreateRow : TutorialAction()
    data class TypeListName(val text: String) : TutorialAction()
    data object OpenDueDatePicker : TutorialAction()
    data class PickDueDate(val date: LocalDate) : TutorialAction()
    data object SubmitList : TutorialAction()
    data object OpenFirstList : TutorialAction()
    data object RequestDeleteFirstList : TutorialAction()
    data object ConfirmDeleteFirstList : TutorialAction()
    data object OpenItemAddRow : TutorialAction()
    data class TypeItemTitle(val text: String) : TutorialAction()
    data object SubmitItem : TutorialAction()
    data class ToggleActiveItem(val index: Int) : TutorialAction()
    data class ToggleCompletedItem(val index: Int) : TutorialAction()
    data class MoveActiveItem(val from: Int, val to: Int) : TutorialAction()
    data class CommitReorder(val from: Int, val to: Int) : TutorialAction()
    data object NavigateBack : TutorialAction()
}
