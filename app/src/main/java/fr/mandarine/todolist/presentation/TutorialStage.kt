package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import java.time.LocalDate

data class TutorialBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

data class TutorialBannerContent(
    val listName: String,
    val dueDate: LocalDate?
)

interface TutorialStage {
    val screen: TutorialScreen

    fun boundsOf(anchor: TutorialAnchor): TutorialBounds?

    suspend fun perform(action: TutorialAction): Boolean

    suspend fun awaitDemoListId(): String?

    fun bannerContent(): TutorialBannerContent?
}
