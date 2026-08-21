package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.domain.TutorialStep
import java.time.LocalDate
import kotlinx.coroutines.delay

class TutorialDirector(
    private val stage: TutorialStage,
    private val overlay: TutorialOverlay,
    private val tutorialViewModel: TutorialViewModel,
    private val today: () -> LocalDate
) {

    suspend fun playOpening() {
        if (stage.screen != TutorialScreen.LISTS) return

        delay(1200)
        point(TutorialAnchor.CreateListButton, 600)
        delay(400)
        overlay.tap()
        if (!stage.perform(TutorialAction.OpenListCreateRow)) return
        delay(700)

        point(TutorialAnchor.ListNameField, 400)
        delay(300)
        stage.perform(TutorialAction.TypeListName(DEMO_LIST_NAME))
        delay(600)

        point(TutorialAnchor.TargetDateButton, 500)
        stage.boundsOf(TutorialAnchor.ListCreateRow)?.let {
            overlay.showCaption(TutorialCaption.TARGET_DATE, it)
        }
        delay(1800)

        point(TutorialAnchor.DueDateButton, 500)
        overlay.updateCaption(TutorialCaption.DUE_DATE)
        delay(1500)
        overlay.tap()
        stage.perform(TutorialAction.OpenDueDatePicker)
        delay(1400)

        stage.perform(TutorialAction.PickDueDate(today().plusDays(1)))
        overlay.hideCaption()
        delay(600)

        point(TutorialAnchor.SubmitListButton, 400)
        delay(400)
        overlay.tap()
        stage.perform(TutorialAction.SubmitList)

        val listId = stage.awaitDemoListId() ?: return
        tutorialViewModel.onDemoListCreated(listId)
    }

    suspend fun play(step: TutorialStep) {
        when (step) {
            TutorialStep.CREATE_LIST -> onLists { showBannerThenAdvance() }
            TutorialStep.SET_DUE_DATE -> onLists { openDemoList() }
            TutorialStep.OPEN_LIST -> onItems { addDemoItems() }
            TutorialStep.COMPLETE_AND_REORDER -> onItems { completeAndReorder() }
            TutorialStep.DELETE_LIST -> {
                if (stage.screen == TutorialScreen.ITEMS) {
                    returnToLists()
                } else {
                    deleteDemoList()
                }
            }
        }
    }

    private suspend fun showBannerThenAdvance() {
        delay(500)
        stage.bannerContent()?.let { overlay.showBanner(it) }
        tutorialViewModel.advanceStep()
    }

    private suspend fun openDemoList() {
        delay(800)
        point(TutorialAnchor.FirstListRow, 500)
        delay(400)
        overlay.tap()
        if (!stage.perform(TutorialAction.OpenFirstList)) return
        tutorialViewModel.advanceStep()
    }

    private suspend fun addDemoItems() {
        delay(1400)
        point(TutorialAnchor.ItemGhostRow, 500)
        delay(400)
        overlay.tap()
        if (stage.perform(TutorialAction.OpenItemAddRow)) {
            delay(700)
        }

        addItem(DEMO_ITEM_FIRST)
        delay(900)
        addItem(DEMO_ITEM_SECOND)
        delay(800)

        tutorialViewModel.advanceStep()
    }

    private suspend fun addItem(title: String) {
        stage.perform(TutorialAction.TypeItemTitle(title))
        delay(400)
        point(TutorialAnchor.SubmitItemButton, 300)
        delay(300)
        overlay.tap()
        stage.perform(TutorialAction.SubmitItem)
    }

    private suspend fun completeAndReorder() {
        delay(800)
        tapToggle(TutorialAnchor.ActiveItemToggle(0), TutorialAction.ToggleActiveItem(0), 500, 400, 900)

        delay(300)
        tapToggle(
            TutorialAnchor.CompletedItemToggle(0),
            TutorialAction.ToggleCompletedItem(0),
            500,
            400,
            900
        )

        delay(300)
        dragActiveItemToTop()

        delay(300)
        tapToggle(TutorialAnchor.ActiveItemToggle(0), TutorialAction.ToggleActiveItem(0), 400, 300, 700)

        delay(300)
        tapToggle(TutorialAnchor.ActiveItemToggle(0), TutorialAction.ToggleActiveItem(0), 400, 300, 800)

        delay(400)
        tutorialViewModel.advanceStep()
    }

    private suspend fun dragActiveItemToTop() {
        if (stage.boundsOf(TutorialAnchor.ActiveItemDragHandle(1)) == null) return

        point(TutorialAnchor.ActiveItemDragHandle(1), 500)
        delay(400)
        overlay.grip()
        delay(200)

        stage.perform(TutorialAction.MoveActiveItem(1, 0))
        delay(100)

        point(TutorialAnchor.ActiveItemRow(0), 600)
        delay(400)
        overlay.release()
        delay(200)

        stage.perform(TutorialAction.CommitReorder(1, 0))
        delay(800)
    }

    private suspend fun returnToLists() {
        delay(600)
        stage.perform(TutorialAction.NavigateBack)
    }

    private suspend fun deleteDemoList() {
        delay(1200)
        point(TutorialAnchor.DeleteListButton, 500)
        delay(400)
        overlay.tap()
        if (!stage.perform(TutorialAction.RequestDeleteFirstList)) return
        delay(900)

        point(TutorialAnchor.ConfirmDeleteButton, 400)
        delay(400)
        overlay.tap()
        stage.perform(TutorialAction.ConfirmDeleteFirstList)
        delay(1200)

        tutorialViewModel.advanceStep()
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
        delay(beforeTapMillis)
        overlay.tap()
        stage.perform(action)
        delay(afterTapMillis)
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
        const val DEMO_LIST_NAME = "🛒 Groceries"
        const val DEMO_ITEM_FIRST = "🍎 Apples"
        const val DEMO_ITEM_SECOND = "🥖 Bread"
    }
}
