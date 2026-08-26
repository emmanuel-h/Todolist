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

    /**
     * Put the page back the way the demo found it. A tour that stops partway
     * through has half-driven controls open on the reader's own page — a create
     * row with the demo's name typed into it, a calendar, a sheet, a row held
     * aside — and those are the demo's, not theirs. The half-written row was the
     * worse one: the next tap anywhere on the page submitted it, and the reader
     * was left owning a list they had watched somebody else start.
     */
    fun abandon()
}
