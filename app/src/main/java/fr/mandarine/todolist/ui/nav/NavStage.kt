package fr.mandarine.todolist.ui.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialStage
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost

/**
 * A page the demo's hand can be standing on: the beats it answers, and the
 * rectangles the hand is pointed at while it is the page on top.
 */
interface PageStage : TutorialStage {
    val anchors: TutorialAnchorHost
}

/**
 * One window means one stage. Which screen the demo is standing on is no longer a
 * question of which activity is resumed but of which page is on top of the back
 * stack, and the page of items hands itself in for as long as it is open.
 */
class NavStage(
    private val backStack: MutableList<NavKey>,
    private val lists: PageStage
) : TutorialStage {

    private var items by mutableStateOf<PageStage?>(null)

    var animationsEnabled by mutableStateOf(true)

    var recordingAnchors by mutableStateOf(false)

    val anchors: TutorialAnchorHost = TopOfStackAnchors { items?.anchors ?: lists.anchors }

    val onItems: Boolean get() = backStack.lastOrNull() is ItemsRoute

    fun attach(stage: PageStage) {
        items = stage
    }

    fun detach(stage: PageStage) {
        if (items === stage) items = null
    }

    fun open(list: TodoList) {
        backStack.add(ItemsRoute(list.id))
    }

    fun leave() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    override val screen: TutorialScreen
        get() = if (onItems) TutorialScreen.ITEMS else TutorialScreen.LISTS

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = onTop?.boundsOf(anchor)

    override suspend fun perform(action: TutorialAction): Boolean =
        onTop?.perform(action) ?: false

    override suspend fun awaitDemoListId(): String? = onTop?.awaitDemoListId()

    override fun bannerContent(): TutorialBannerContent? = onTop?.bannerContent()

    private val onTop: TutorialStage? get() = if (onItems) items else lists
}

private class TopOfStackAnchors(
    private val top: () -> TutorialAnchorHost
) : TutorialAnchorHost {

    override var recordingAnchors: Boolean
        get() = top().recordingAnchors
        set(value) {
            top().recordingAnchors = value
        }

    override fun putBounds(anchor: TutorialAnchor, bounds: TutorialBounds) {
        top().putBounds(anchor, bounds)
    }

    override fun removeBounds(anchor: TutorialAnchor) {
        top().removeBounds(anchor)
    }

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = top().boundsOf(anchor)
}
