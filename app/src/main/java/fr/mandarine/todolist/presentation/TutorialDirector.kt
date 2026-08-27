package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.domain.TutorialStep
import java.time.LocalDate

class TutorialDirector(
    private val stage: TutorialStage,
    private val overlay: TutorialOverlay,
    private val tutorialViewModel: TutorialViewModel,
    private val pace: TutorialPace,
    private val today: () -> LocalDate
) {

    suspend fun playOpening() {
        if (stage.screen != TutorialScreen.LISTS) return

        overlay.narrate(TutorialLine.WRITE_A_LIST)
        pace.beat(500)
        point(TutorialAnchor.CreateListButton, 300)
        pace.beat(200)
        overlay.tap()
        if (!stage.perform(TutorialAction.OpenListCreateRow)) {
            abandon()
            return
        }
        pace.beat(350)

        point(TutorialAnchor.ListNameField, 200)
        pace.beat(150)
        stage.perform(TutorialAction.TypeListName(DEMO_LIST_NAME))
        pace.beat(300)

        point(TutorialAnchor.TargetDateButton, 250)
        stage.boundsOf(TutorialAnchor.ListCreateRow)?.let {
            overlay.showCaption(TutorialCaption.TARGET_DATE, it)
        }
        pace.beat(900)

        point(TutorialAnchor.DueDateButton, 250)
        overlay.updateCaption(TutorialCaption.DUE_DATE)
        pace.beat(750)
        overlay.tap()
        stage.perform(TutorialAction.OpenDueDatePicker)
        pace.beat(700)

        stage.perform(TutorialAction.PickDueDate(today().plusDays(1)))
        overlay.hideCaption()
        pace.beat(300)

        point(TutorialAnchor.SubmitListButton, 200)
        pace.beat(200)
        overlay.tap()
        stage.perform(TutorialAction.SubmitList)

        val listId = stage.awaitDemoListId()
        if (listId == null) {
            abandon()
            return
        }
        tutorialViewModel.onDemoListCreated(listId)
    }

    suspend fun play(step: TutorialStep) {
        when (step) {
            TutorialStep.A_DAY_AND_A_NOTE -> onLists { showBannerThenAdvance() }
            TutorialStep.OPEN_IT -> onLists { openDemoList() }
            TutorialStep.WRITE_ITEMS -> onItems { addDemoItems() }
            TutorialStep.TICK_AND_MOVE -> onItems { completeAndReorder() }
            TutorialStep.EDIT_AND_TEAR -> {
                if (stage.screen == TutorialScreen.ITEMS) {
                    returnToLists()
                } else {
                    deleteDemoList()
                }
            }
        }
    }

    /**
     * A scene says its line before it does anything, and the line is up for the
     * whole of the scene. Announcing it halfway through means the reader reads a
     * sentence about something they have already watched happen.
     */
    private suspend fun openWith(line: TutorialLine, restMillis: Long) {
        overlay.narrate(line)
        pace.beat(restMillis)
    }

    private suspend fun showBannerThenAdvance() {
        openWith(TutorialLine.A_DAY_AND_A_NOTE, 550)
        stage.bannerContent()?.let { overlay.showBanner(it) }
        pace.beat(250)
        tutorialViewModel.advanceStep()
    }

    private suspend fun openDemoList() {
        openWith(TutorialLine.OPEN_IT, 700)
        point(TutorialAnchor.FirstListRow, 250)
        pace.beat(450)
        overlay.tap()
        if (!stage.perform(TutorialAction.OpenFirstList)) {
            abandon()
            return
        }
        tutorialViewModel.advanceStep()
    }

    private suspend fun addDemoItems() {
        openWith(TutorialLine.WRITE_ITEMS, 700)
        point(TutorialAnchor.ItemGhostRow, 250)
        pace.beat(200)
        overlay.tap()
        if (stage.perform(TutorialAction.OpenItemAddRow)) {
            pace.beat(350)
        }

        addItem(DEMO_ITEM_FIRST)
        pace.beat(450)
        addItem(DEMO_ITEM_SECOND)
        pace.beat(400)

        tutorialViewModel.advanceStep()
    }

    private suspend fun addItem(title: String) {
        stage.perform(TutorialAction.TypeItemTitle(title))
        pace.beat(200)
        point(TutorialAnchor.SubmitItemButton, 150)
        pace.beat(150)
        overlay.tap()
        stage.perform(TutorialAction.SubmitItem)
    }

    /**
     * A tick, the same tick rubbed out, and a row carried up the page. It used to
     * tick a second row afterwards as well, which taught nothing the first tick
     * had not and left the demo list finished — so the last scene went looking for
     * "the first row" and found the reader's, not the demo's.
     */
    private suspend fun completeAndReorder() {
        openWith(TutorialLine.TICK_AND_MOVE, 500)
        tapToggle(TutorialAnchor.ActiveItemToggle(0), TutorialAction.ToggleActiveItem(0), 250, 200, 450)

        pace.beat(150)
        tapToggle(
            TutorialAnchor.CompletedItemToggle(0),
            TutorialAction.ToggleCompletedItem(0),
            250,
            200,
            450
        )

        pace.beat(150)
        dragActiveItemToTop()

        pace.beat(150)
        tapToggle(TutorialAnchor.ActiveItemToggle(0), TutorialAction.ToggleActiveItem(0), 200, 150, 350)

        pace.beat(200)
        tutorialViewModel.advanceStep()
    }

    private suspend fun dragActiveItemToTop() {
        if (stage.boundsOf(TutorialAnchor.ActiveItemDragHandle(1)) == null) return

        point(TutorialAnchor.ActiveItemDragHandle(1), 250)
        pace.beat(250)
        overlay.grip()
        pace.beat(400)

        stage.perform(TutorialAction.MoveActiveItem(1, 0))
        pace.beat(250)

        point(TutorialAnchor.ActiveItemRow(0), 300)
        pace.beat(300)
        overlay.release()
        pace.beat(250)

        stage.perform(TutorialAction.CommitReorder(1, 0))
        pace.beat(400)
    }

    private suspend fun returnToLists() {
        overlay.narrate(TutorialLine.EDIT_AND_TEAR)
        pace.beat(300)
        if (!stage.perform(TutorialAction.NavigateBack)) abandon()
    }

    /**
     * A row is a page and it comes away in the hand, both ways: pulled towards the
     * end it opens the sheet the list is edited on — which is where a day is put on
     * a list that has none — and pulled towards the start it tears off. The row is
     * held aside for real while the hand crosses it, so the reader watches the
     * corner turn and sees what is underneath rather than watching a hand travel
     * over a row that never moves.
     */
    private suspend fun deleteDemoList() {
        openWith(TutorialLine.EDIT_AND_TEAR, 600)
        val row = stage.boundsOf(TutorialAnchor.DeleteListButton)
        if (row == null) {
            abandon()
            return
        }

        overlay.glideTo(row.alongRow(EDIT_FROM), 250)
        pace.beat(250)
        overlay.grip()
        pace.beat(300)
        pullAcross(row, EDIT_FROM, EDIT_TO, row.width * EDIT_REACH)
        pace.beat(150)
        overlay.release()
        if (!stage.perform(TutorialAction.OpenFirstListEditor)) {
            stage.perform(TutorialAction.LetFirstListGo)
            abandon()
            return
        }
        pace.beat(900)
        stage.perform(TutorialAction.CloseEditor)
        pace.beat(350)

        overlay.glideTo(row.alongRow(TEAR_FROM), 250)
        pace.beat(250)
        overlay.grip()
        pace.beat(300)
        pullAcross(row, TEAR_FROM, TEAR_TO, -row.width * TEAR_REACH)
        if (!stage.perform(TutorialAction.RequestDeleteFirstList)) {
            stage.perform(TutorialAction.LetFirstListGo)
            overlay.release()
            abandon()
            return
        }
        stage.perform(TutorialAction.LetFirstListGo)
        pace.beat(150)
        overlay.release()
        pace.beat(450)

        if (!stage.perform(TutorialAction.ConfirmDeleteFirstList)) {
            abandon()
            return
        }
        pace.beat(600)

        tutorialViewModel.advanceStep()
    }

    /**
     * A beat that cannot be played is the end of the demonstration, not a pause in
     * it. Returning quietly left the hand up over a page the tour had stopped
     * driving, and left the demo's own list on that page for the reader to find
     * and wonder about; ending it tears the list off the way the way out does.
     */
    private fun abandon() {
        tutorialViewModel.skip()
    }

    /**
     * The hand and the paper move together. Gliding the hand and then jumping the
     * row to where it ended up read as two things happening near each other rather
     * than as one hand dragging one page.
     */
    private suspend fun pullAcross(
        row: TutorialBounds,
        from: Float,
        to: Float,
        reach: Float
    ) {
        for (step in 1..PULL_STEPS) {
            val at = pullStepAt(from, to, reach, step, PULL_STEPS)
            overlay.glideTo(row.alongRow(at.handAt), PULL_STEP_MILLIS)
            stage.perform(TutorialAction.PullFirstList(at.pixels))
        }
    }

    private suspend fun tapToggle(
        anchor: TutorialAnchor,
        action: TutorialAction,
        glideMillis: Long,
        beforeTapMillis: Long,
        afterTapMillis: Long
    ) {
        if (stage.boundsOf(anchor) == null) return
        point(anchor, glideMillis)
        pace.beat(beforeTapMillis)
        overlay.tap()
        stage.perform(action)
        pace.beat(afterTapMillis)
    }

    private suspend fun point(anchor: TutorialAnchor, durationMillis: Long) {
        val bounds = stage.boundsOf(anchor)
        if (bounds != null) {
            overlay.glideTo(bounds, durationMillis)
        }
    }

    private suspend fun onLists(block: suspend () -> Unit) {
        if (stage.screen == TutorialScreen.LISTS) block()
    }

    private suspend fun onItems(block: suspend () -> Unit) {
        if (stage.screen == TutorialScreen.ITEMS) block()
    }

    private companion object {
        const val TEAR_FROM = 0.86f
        const val TEAR_TO = 0.18f
        const val TEAR_REACH = 0.30f
        const val EDIT_FROM = 0.14f
        const val EDIT_TO = 0.62f
        const val EDIT_REACH = 0.26f
        const val PULL_STEPS = 6
        const val PULL_STEP_MILLIS = 70L
        const val DEMO_LIST_NAME = "🛒 Groceries"
        const val DEMO_ITEM_FIRST = "🍎 Apples"
        const val DEMO_ITEM_SECOND = "🥖 Bread"
    }
}

internal class PullStep(val handAt: Float, val pixels: Float)

/**
 * Where the hand has got to and how far the paper has come with it, a fraction of
 * the way through a pull. Both are read off the one fraction, which is what makes
 * the hand and the page move together rather than merely near each other.
 */
internal fun pullStepAt(from: Float, to: Float, reach: Float, step: Int, steps: Int): PullStep {
    val fraction = step.toFloat() / steps
    return PullStep(handAt = from + (to - from) * fraction, pixels = reach * fraction)
}
