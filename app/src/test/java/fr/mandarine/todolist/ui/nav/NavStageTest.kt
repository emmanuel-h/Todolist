package fr.mandarine.todolist.ui.nav

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost
import fr.mandarine.todolist.ui.tutorial.TutorialAnchors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavStageTest {

    private val backStack = NavBackStack<NavKey>(ListsRoute)
    private val lists = FakePage(TutorialScreen.LISTS)
    private val items = FakePage(TutorialScreen.ITEMS)
    private val stage = NavStage(backStack, lists)

    @Test
    fun `should stand on the page of lists while nothing is laid over it`() {
        assertEquals(TutorialScreen.LISTS, stage.screen)
        assertFalse(stage.onItems)
    }

    @Test
    fun `should stand on the page of items once a list is opened`() {
        stage.open(TodoList("list-1", "Groceries"))

        assertEquals(TutorialScreen.ITEMS, stage.screen)
        assertEquals(listOf(ListsRoute, ItemsRoute("list-1")), backStack.toList())
    }

    @Test
    fun `should hand every beat to the page that is on top`() {
        stage.open(TodoList("list-1", "Groceries"))
        stage.attach(items)

        assertTrue(runBlocking { stage.perform(TutorialAction.SubmitItem) })
        assertEquals(TutorialAction.SubmitItem, items.played)
        assertNull(lists.played)
    }

    @Test
    fun `should refuse a beat while the open page has not been handed in`() {
        stage.open(TodoList("list-1", "Groceries"))

        assertFalse(runBlocking { stage.perform(TutorialAction.SubmitItem) })
    }

    @Test
    fun `should go back to the page of lists once the open page is handed back`() {
        stage.open(TodoList("list-1", "Groceries"))
        stage.attach(items)
        stage.leave()
        stage.detach(items)

        assertTrue(runBlocking { stage.perform(TutorialAction.SubmitList) })
        assertEquals(TutorialAction.SubmitList, lists.played)
    }

    @Test
    fun `should keep a page that was replaced rather than the one handed back`() {
        stage.open(TodoList("list-1", "Groceries"))
        stage.attach(items)
        stage.detach(FakePage(TutorialScreen.ITEMS))

        assertTrue(runBlocking { stage.perform(TutorialAction.SubmitItem) })
    }

    @Test
    fun `should never peel the last page off the pad`() {
        stage.leave()

        assertEquals(listOf(ListsRoute), backStack.toList())
    }

    @Test
    fun `should peel only the page on top off the pad`() {
        stage.open(TodoList("list-1", "Groceries"))

        stage.leave()

        assertEquals(listOf(ListsRoute), backStack.toList())
        assertEquals(TutorialScreen.LISTS, stage.screen)
    }

    @Test
    fun `should point the hand at the rectangles of the page on top`() {
        val onLists = TutorialBounds(left = 1, top = 2, width = 3, height = 4)
        val onItems = TutorialBounds(left = 5, top = 6, width = 7, height = 8)
        lists.anchors.putBounds(TutorialAnchor.FirstListRow, onLists)
        items.anchors.putBounds(TutorialAnchor.FirstListRow, onItems)

        assertEquals(onLists, stage.anchors.boundsOf(TutorialAnchor.FirstListRow))

        stage.open(TodoList("list-1", "Groceries"))
        stage.attach(items)

        assertEquals(onItems, stage.anchors.boundsOf(TutorialAnchor.FirstListRow))
    }

    @Test
    fun `should measure the rectangles of the page on top only while the demo watches`() {
        stage.open(TodoList("list-1", "Groceries"))
        stage.attach(items)

        stage.anchors.recordingAnchors = true

        assertTrue(items.anchors.recordingAnchors)
        assertFalse(lists.anchors.recordingAnchors)
    }

    @Test
    fun `should drop the rectangles of the page on top when they go off the page`() {
        stage.open(TodoList("list-1", "Groceries"))
        stage.attach(items)
        stage.anchors.putBounds(
            TutorialAnchor.FirstListRow,
            TutorialBounds(left = 1, top = 2, width = 3, height = 4)
        )

        stage.anchors.removeBounds(TutorialAnchor.FirstListRow)

        assertNull(items.anchors.boundsOf(TutorialAnchor.FirstListRow))
    }

    /**
     * The page the demo left something on is often the one underneath: it opens a
     * create row on the page of lists and then walks into a list.
     */
    @Test
    fun `should put both pages back when the tour is abandoned`() {
        val items = FakePage(TutorialScreen.ITEMS)
        val lists = FakePage(TutorialScreen.LISTS)
        val backStack = mutableListOf<NavKey>(ListsRoute, ItemsRoute("list-1"))
        val stage = NavStage(backStack, lists)
        stage.attach(items)

        stage.abandon()

        assertEquals(1, lists.abandoned)
        assertEquals(1, items.abandoned)
    }

    private class FakePage(
        override val screen: TutorialScreen,
        override val anchors: TutorialAnchorHost = TutorialAnchors()
    ) : PageStage {

        var played: TutorialAction? = null
        var abandoned = 0

        override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = anchors.boundsOf(anchor)

        override suspend fun perform(action: TutorialAction): Boolean {
            played = action
            return true
        }

        override suspend fun awaitDemoListId(): String? = null

        override fun bannerContent(): TutorialBannerContent? = null

        override fun abandon() {
            abandoned += 1
        }
    }
}
