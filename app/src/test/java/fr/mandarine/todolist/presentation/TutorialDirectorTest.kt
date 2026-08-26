package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.domain.TutorialStep
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TutorialDirectorTest {

    private val today = LocalDate.of(2026, 8, 21)
    private val tomorrow = LocalDate.of(2026, 8, 22)

    private lateinit var overlay: RecordingOverlay
    private lateinit var viewModel: TutorialViewModel

    @Before
    fun setUp() {
        viewModel = mockk(relaxed = true)
    }

    private fun directorFor(stage: RecordingStage): TutorialDirector {
        overlay = RecordingOverlay(stage)
        return TutorialDirector(stage, overlay, viewModel) { today }
    }

    // ── Opening scene ──

    @Test
    fun `should run the whole opening scene when every anchor is present`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).playOpening()

        assertEquals(
            listOf(
                TutorialAction.OpenListCreateRow,
                TutorialAction.TypeListName("🛒 Groceries"),
                TutorialAction.OpenDueDatePicker,
                TutorialAction.PickDueDate(tomorrow),
                TutorialAction.SubmitList
            ),
            stage.actions
        )
        assertEquals(
            listOf(
                "glide:CreateListButton",
                "tap",
                "glide:ListNameField",
                "glide:TargetDateButton",
                "caption:TARGET_DATE",
                "glide:DueDateButton",
                "updateCaption:DUE_DATE",
                "tap",
                "hideCaption",
                "glide:SubmitListButton",
                "tap"
            ),
            overlay.events
        )
        verify { viewModel.onDemoListCreated("demo-list") }
    }

    @Test
    fun `should hold the scripted pacing of the opening scene`() = runTest {
        directorFor(RecordingStage(TutorialScreen.LISTS)).playOpening()

        assertEquals(8900L, testScheduler.currentTime)
    }

    @Test
    fun `should do nothing in the opening scene when hosted by the items screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).playOpening()

        assertTrue(stage.actions.isEmpty())
        assertTrue(overlay.events.isEmpty())
    }

    @Test
    fun `should abort the opening scene when the create row refuses to open`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.canPerform = { it != TutorialAction.OpenListCreateRow }

        directorFor(stage).playOpening()

        assertEquals(listOf(TutorialAction.OpenListCreateRow), stage.actions)
        verify(exactly = 0) { viewModel.onDemoListCreated("demo-list") }
    }

    @Test
    fun `should not register a demo list when no list id materialises`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.demoListId = null

        directorFor(stage).playOpening()

        verify(exactly = 0) { viewModel.onDemoListCreated("demo-list") }
    }

    @Test
    fun `should skip the caption when the create row has no bounds`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.anchorAvailable = { it != TutorialAnchor.ListCreateRow }

        directorFor(stage).playOpening()

        assertTrue(overlay.events.none { it.startsWith("caption:") })
    }

    @Test
    fun `should not glide when the target anchor has no bounds`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.anchorAvailable = { it != TutorialAnchor.CreateListButton }

        directorFor(stage).playOpening()

        assertTrue(overlay.events.none { it == "glide:CreateListButton" })
    }

    // ── CREATE_LIST ──

    @Test
    fun `should show the notification banner then advance on the create list step`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.banner = TutorialBannerContent("🛒 Groceries", tomorrow)

        directorFor(stage).play(TutorialStep.CREATE_LIST)

        assertEquals(listOf("banner:🛒 Groceries"), overlay.events)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should still advance the create list step when there is no banner content`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.banner = null

        directorFor(stage).play(TutorialStep.CREATE_LIST)

        assertTrue(overlay.events.isEmpty())
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should ignore the create list step on the items screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.CREATE_LIST)

        verify(exactly = 0) { viewModel.advanceStep() }
    }

    // ── SET_DUE_DATE ──

    @Test
    fun `should open the demo list then advance on the due date step`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).play(TutorialStep.SET_DUE_DATE)

        assertEquals(listOf(TutorialAction.OpenFirstList), stage.actions)
        assertEquals(listOf("glide:FirstListRow", "tap"), overlay.events)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should not advance the due date step when the list cannot be opened`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.canPerform = { it != TutorialAction.OpenFirstList }

        directorFor(stage).play(TutorialStep.SET_DUE_DATE)

        verify(exactly = 0) { viewModel.advanceStep() }
    }

    @Test
    fun `should tap without gliding when the first list row has no bounds`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.anchorAvailable = { it != TutorialAnchor.FirstListRow }

        directorFor(stage).play(TutorialStep.SET_DUE_DATE)

        assertEquals(listOf("tap"), overlay.events)
    }

    @Test
    fun `should ignore the due date step on the items screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.SET_DUE_DATE)

        assertTrue(stage.actions.isEmpty())
    }

    // ── OPEN_LIST ──

    @Test
    fun `should add both demo items then advance on the open list step`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.OPEN_LIST)

        assertEquals(
            listOf(
                TutorialAction.OpenItemAddRow,
                TutorialAction.TypeItemTitle("🍎 Apples"),
                TutorialAction.SubmitItem,
                TutorialAction.TypeItemTitle("🥖 Bread"),
                TutorialAction.SubmitItem
            ),
            stage.actions
        )
        assertEquals(5600L, testScheduler.currentTime)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should still add demo items when the ghost row cannot be opened`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.canPerform = { it != TutorialAction.OpenItemAddRow }

        directorFor(stage).play(TutorialStep.OPEN_LIST)

        assertTrue(stage.actions.contains(TutorialAction.TypeItemTitle("🍎 Apples")))
        assertTrue(stage.actions.contains(TutorialAction.TypeItemTitle("🥖 Bread")))
        assertEquals(4900L, testScheduler.currentTime)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should ignore the open list step on the lists screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).play(TutorialStep.OPEN_LIST)

        assertTrue(stage.actions.isEmpty())
    }

    // ── COMPLETE_AND_REORDER ──

    @Test
    fun `should complete restore reorder and complete again then advance`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.COMPLETE_AND_REORDER)

        assertEquals(
            listOf(
                TutorialAction.ToggleActiveItem(0),
                TutorialAction.ToggleCompletedItem(0),
                TutorialAction.MoveActiveItem(1, 0),
                TutorialAction.CommitReorder(1, 0),
                TutorialAction.ToggleActiveItem(0),
                TutorialAction.ToggleActiveItem(0)
            ),
            stage.actions
        )
        assertTrue(overlay.events.contains("grip"))
        assertTrue(overlay.events.contains("release"))
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should hold the scripted pacing of the complete and reorder step`() = runTest {
        directorFor(RecordingStage(TutorialScreen.ITEMS)).play(TutorialStep.COMPLETE_AND_REORDER)

        assertEquals(9200L, testScheduler.currentTime)
    }

    @Test
    fun `should skip the drag when no drag handle is present`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.anchorAvailable = { it != TutorialAnchor.ActiveItemDragHandle(1) }

        directorFor(stage).play(TutorialStep.COMPLETE_AND_REORDER)

        assertTrue(stage.actions.none { it is TutorialAction.MoveActiveItem })
        assertTrue(stage.actions.none { it is TutorialAction.CommitReorder })
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should skip a toggle when its row is not on screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.anchorAvailable = { it != TutorialAnchor.CompletedItemToggle(0) }

        directorFor(stage).play(TutorialStep.COMPLETE_AND_REORDER)

        assertTrue(stage.actions.none { it is TutorialAction.ToggleCompletedItem })
    }

    @Test
    fun `should ignore the complete and reorder step on the lists screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).play(TutorialStep.COMPLETE_AND_REORDER)

        assertTrue(stage.actions.isEmpty())
    }

    // ── DELETE_LIST ──

    @Test
    fun `should navigate back without advancing when deleting from the items screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        assertEquals(listOf(TutorialAction.NavigateBack), stage.actions)
        verify(exactly = 0) { viewModel.advanceStep() }
    }

    @Test
    fun `should delete and confirm then advance on the lists screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        assertEquals(
            listOf(
                TutorialAction.RequestDeleteFirstList,
                TutorialAction.ConfirmDeleteFirstList
            ),
            stage.actions
        )
        assertEquals(
            listOf("glide:DeleteListButton", "grip", "glide:DeleteListButton", "release"),
            overlay.events
        )
        verify { viewModel.advanceStep() }
    }

    /**
     * The demo tears the row off by dragging across it, so it needs the row's own
     * rectangle. A page with no row on it has none to give.
     */
    @Test
    fun `should abandon the delete scene when the row cannot be found`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.anchorAvailable = { it != TutorialAnchor.DeleteListButton }

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        assertEquals(emptyList<TutorialAction>(), stage.actions)
        assertEquals(emptyList<String>(), overlay.events)
        verify(exactly = 0) { viewModel.advanceStep() }
    }

    @Test
    fun `should abort the delete scene when the delete button refuses`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.canPerform = { it != TutorialAction.RequestDeleteFirstList }

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        assertEquals(listOf(TutorialAction.RequestDeleteFirstList), stage.actions)
        assertEquals("release", overlay.events.last())
        verify(exactly = 0) { viewModel.advanceStep() }
    }

    private class RecordingStage(override val screen: TutorialScreen) : TutorialStage {

        val actions = mutableListOf<TutorialAction>()
        var canPerform: (TutorialAction) -> Boolean = { true }
        var anchorAvailable: (TutorialAnchor) -> Boolean = { true }
        var demoListId: String? = "demo-list"
        var banner: TutorialBannerContent? = null

        private val issued = mutableMapOf<TutorialBounds, String>()
        private var nextLeft = 1

        override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? {
            if (!anchorAvailable(anchor)) return null
            val bounds = TutorialBounds(nextLeft++, 0, 10, 10)
            issued[bounds] = anchor::class.simpleName.orEmpty()
            return bounds
        }

        /**
         * A beat the hand travels across rather than lands on derives its point
         * from the row's own bounds, so a derived point is named for the row it
         * came from.
         */
        fun labelFor(bounds: TutorialBounds): String = issued[bounds]
            ?: issued.entries
                .firstOrNull { (issued, _) ->
                    issued.top == bounds.top && issued.height == bounds.height
                }
                ?.value
                .orEmpty()

        override suspend fun perform(action: TutorialAction): Boolean {
            actions.add(action)
            yield()
            return canPerform(action)
        }

        override suspend fun awaitDemoListId(): String? = demoListId

        override fun bannerContent(): TutorialBannerContent? = banner
    }

    private class RecordingOverlay(private val stage: RecordingStage) : TutorialOverlay {

        val events = mutableListOf<String>()

        override suspend fun glideTo(bounds: TutorialBounds, durationMillis: Long) {
            events.add("glide:${stage.labelFor(bounds)}")
            yield()
        }

        override suspend fun tap() {
            events.add("tap")
            yield()
        }

        override suspend fun grip() {
            events.add("grip")
            yield()
        }

        override suspend fun release() {
            events.add("release")
            yield()
        }

        override suspend fun showCaption(caption: TutorialCaption, below: TutorialBounds) {
            events.add("caption:$caption")
            yield()
        }

        override suspend fun updateCaption(caption: TutorialCaption) {
            events.add("updateCaption:$caption")
            yield()
        }

        override suspend fun hideCaption() {
            events.add("hideCaption")
            yield()
        }

        override suspend fun showBanner(content: TutorialBannerContent) {
            events.add("banner:${content.listName}")
            yield()
        }
    }
}
