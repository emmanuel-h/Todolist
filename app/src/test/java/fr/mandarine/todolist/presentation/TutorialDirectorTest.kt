package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.domain.TutorialStep
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
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
    private lateinit var pace: TutorialPace

    @Before
    fun setUp() {
        viewModel = mockk(relaxed = true)
        pace = TutorialPace()
    }

    private fun directorFor(stage: RecordingStage): TutorialDirector {
        overlay = RecordingOverlay(stage)
        return TutorialDirector(stage, overlay, viewModel, pace) { today }
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

        assertEquals(4350L, testScheduler.currentTime)
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
        verify { viewModel.skip() }
    }

    @Test
    fun `should not register a demo list when no list id materialises`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.demoListId = null

        directorFor(stage).playOpening()

        verify(exactly = 0) { viewModel.onDemoListCreated("demo-list") }
        verify { viewModel.skip() }
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
        verify { viewModel.skip() }
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
        assertEquals(2800L, testScheduler.currentTime)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should still add demo items when the ghost row cannot be opened`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.canPerform = { it != TutorialAction.OpenItemAddRow }

        directorFor(stage).play(TutorialStep.OPEN_LIST)

        assertTrue(stage.actions.contains(TutorialAction.TypeItemTitle("🍎 Apples")))
        assertTrue(stage.actions.contains(TutorialAction.TypeItemTitle("🥖 Bread")))
        assertEquals(2450L, testScheduler.currentTime)
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

        assertEquals(3900L, testScheduler.currentTime)
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
    /**
     * The scene shows a row coming away in the hand both ways before it tears one
     * off: towards the end it opens the sheet the list is edited on, which is the
     * only way onto a list that has no day yet, and towards the start it tears.
     */
    fun `should show both ways a row comes away then tear one off and advance`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        assertEquals(
            listOf(
                "PullFirstList",
                "OpenFirstListEditor",
                "CloseEditor",
                "PullFirstList",
                "RequestDeleteFirstList",
                "LetFirstListGo",
                "ConfirmDeleteFirstList"
            ),
            stage.actions.beats()
        )
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should pull the row towards the end for the editor and the start for the tear`() =
        runTest {
            val stage = RecordingStage(TutorialScreen.LISTS)

            directorFor(stage).play(TutorialStep.DELETE_LIST)

            val pulls = stage.actions
                .filterIsInstance<TutorialAction.PullFirstList>()
                .map { it.pixels }
            val towardsTheEditor = pulls.takeWhile { it > 0f }
            val towardsTheTear = pulls.dropWhile { it > 0f }

            /**
             * The stage hands out rows ten wide, so the reaches are exact: a
             * quarter of the row towards the editor and a little under a third of
             * it towards the tear, arrived at a sixth at a time.
             */
            assertSteps(listOf(0.4333f, 0.8667f, 1.3f, 1.7333f, 2.1667f, 2.6f), towardsTheEditor)
            assertSteps(listOf(-0.5f, -1.0f, -1.5f, -2.0f, -2.5f, -3.0f), towardsTheTear)
        }

    /**
     * A scene that cannot open the sheet has nothing left to show, and must not go
     * on to tear the row off — the reader would be shown the destructive half of
     * the lesson with the harmless half missing.
     */
    @Test
    fun `should give up the scene and let the row go when the editor refuses`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.canPerform = { it != TutorialAction.OpenFirstListEditor }

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        assertEquals(
            listOf("PullFirstList", "OpenFirstListEditor", "LetFirstListGo"),
            stage.actions.beats()
        )
        verify(exactly = 0) { viewModel.advanceStep() }
        verify { viewModel.skip() }
    }

    @Test
    fun `should let the row go rather than leave it held aside when the tear is refused`() =
        runTest {
            val stage = RecordingStage(TutorialScreen.LISTS)
            stage.canPerform = { it != TutorialAction.RequestDeleteFirstList }

            directorFor(stage).play(TutorialStep.DELETE_LIST)

            assertEquals(TutorialAction.LetFirstListGo, stage.actions.last())
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
        verify { viewModel.skip() }
    }

    @Test
    fun `should abort the delete scene when the delete button refuses`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.canPerform = { it != TutorialAction.RequestDeleteFirstList }

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        assertEquals("RequestDeleteFirstList", stage.actions.beats().last { it != "LetFirstListGo" })
        assertEquals("release", overlay.events.last())
        verify(exactly = 0) { viewModel.advanceStep() }
        verify { viewModel.skip() }
    }

    /**
     * A demonstration whose last beat cannot be played must still tear off its own
     * list. It used to return quietly, which left the hand up over a page nothing
     * was driving and the demo's list sitting on it for the reader to find.
     */
    @Test
    fun `should end the tour when the tear is never written through`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.canPerform = { it != TutorialAction.ConfirmDeleteFirstList }

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        assertEquals("ConfirmDeleteFirstList", stage.actions.beats().last())
        verify(exactly = 0) { viewModel.advanceStep() }
        verify { viewModel.skip() }
    }

    @Test
    fun `should end the tour when the demo cannot walk back out of the list`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.canPerform = { it != TutorialAction.NavigateBack }

        directorFor(stage).play(TutorialStep.DELETE_LIST)

        verify { viewModel.skip() }
    }

    // ── Seen enough ──

    /**
     * The reader saying they have understood the beat does not skip it: every
     * action is still driven through, in order, so the page arrives at the next
     * scene exactly where the unhurried scene would have left it.
     */
    @Test
    fun `should drive every action of a scene the reader has seen enough of`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        pace.hurry()

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
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should take no time at all over a scene the reader has seen enough of`() = runTest {
        pace.hurry()

        directorFor(RecordingStage(TutorialScreen.ITEMS)).play(TutorialStep.OPEN_LIST)

        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun `should end the rest that is already being taken when the reader says so`() = runTest {
        val director = directorFor(RecordingStage(TutorialScreen.ITEMS))
        val scene = launch { director.play(TutorialStep.OPEN_LIST) }

        advanceTimeBy(100)
        pace.hurry()
        scene.join()

        assertEquals(100L, testScheduler.currentTime)
    }

    /**
     * A pull is a run of steps, not one event — the hand and the paper move a
     * fraction at a time. What matters to a scene is the beats in order, so a run
     * of pulls counts as the one beat the reader sees.
     */
    private val stepTolerance = 0.001f

    private fun assertSteps(expected: List<Float>, actual: List<Float>) {
        assertEquals("$actual", expected.size, actual.size)
        expected.zip(actual).forEach { (want, got) -> assertEquals("$actual", want, got, stepTolerance) }
    }

    private fun List<TutorialAction>.beats(): List<String> =
        map { it::class.simpleName.orEmpty() }
            .fold(mutableListOf<String>()) { beats, name ->
                if (beats.lastOrNull() != name) beats += name
                beats
            }

    private class RecordingStage(override val screen: TutorialScreen) : TutorialStage {

        val actions = mutableListOf<TutorialAction>()
        var canPerform: (TutorialAction) -> Boolean = { true }
        var anchorAvailable: (TutorialAnchor) -> Boolean = { true }
        var demoListId: String? = "demo-list"
        var banner: TutorialBannerContent? = null
        var abandoned = 0

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

        override fun abandon() {
            abandoned += 1
        }
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

/**
 * The hand and the paper are read off one fraction, so a pull that is a third of
 * the way along has the hand a third of the way across the row and the row a third
 * of the way aside. They used to be worked out separately, which is how a hand can
 * arrive somewhere the page has not.
 */
class PullStepTest {

    @Test
    fun `should put the hand and the paper a third of the way through a third of a pull`() {
        val at = pullStepAt(from = 0.2f, to = 0.8f, reach = -30f, step = 1, steps = 3)

        assertEquals(0.4f, at.handAt, TOLERANCE)
        assertEquals(-10f, at.pixels, TOLERANCE)
    }

    @Test
    fun `should end a pull with the hand at its far end and the paper at full reach`() {
        val at = pullStepAt(from = 0.2f, to = 0.8f, reach = -30f, step = 3, steps = 3)

        assertEquals(0.8f, at.handAt, TOLERANCE)
        assertEquals(-30f, at.pixels, TOLERANCE)
    }

    @Test
    fun `should carry the hand backwards along the row when the pull goes that way`() {
        val at = pullStepAt(from = 0.9f, to = 0.1f, reach = -20f, step = 2, steps = 4)

        assertEquals(0.5f, at.handAt, TOLERANCE)
        assertEquals(-10f, at.pixels, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
