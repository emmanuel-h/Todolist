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
) {
    /**
     * A point along the row, for a beat the hand travels across rather than lands
     * on. The height is collapsed to the row's middle so the hand stays on the
     * rule it is dragging.
     */
    fun alongRow(fraction: Float): TutorialBounds = copy(
        left = left + (width * fraction).toInt(),
        width = 0
    )
}

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
