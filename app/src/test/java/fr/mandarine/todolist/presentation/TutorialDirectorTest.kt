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

/**
 * What the tour's own rests add up to, end to end — 2.5 seconds more than before
 * the rework, which is what six readable sentences cost. Quoted in `docs/SPEC.md`.
 *
 * The banner's own 2.2s hold and every tap's 130ms are not in here: they are taken
 * inside the overlay, and the fake this is measured against does not take them.
 */
private const val SCRIPTED_TOUR_MILLIS = 20_132L

/** How many times the paper is moved during one demonstrated swipe. */
private val DEMO_WORDS = TutorialDemoWords(
    listName = "🛒 Groceries",
    firstItem = "🍎 Apples",
    secondItem = "🥖 Bread"
)

private const val PULLS_PER_SWIPE = 14

/** How many times the hand moves while carrying a row up the page. */
private const val LIFTS_PER_DRAG = 12
private const val PULL_TOLERANCE = 0.0001f

private fun evenlyTo(reach: Float, steps: Int): List<Float> =
    (1..steps).map { reach * it / steps }

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
        return TutorialDirector(stage, overlay, viewModel, pace, { today }, DEMO_WORDS)
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
                TutorialAction.CloseDatePicker,
                TutorialAction.SubmitList
            ),
            stage.actions
        )
        assertEquals(
            listOf(
                "narrate:WRITE_A_LIST",
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

        assertEquals(5800L, testScheduler.currentTime)
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

    /**
     * The slip hangs under the rule the two date glyphs are on. Hung under the line
     * the name is written on — which is what it used to do — it landed squarely on
     * that rule and covered the glyph it was explaining.
     */
    @Test
    fun `should hang the caption under the glyphs rather than under the name`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).playOpening()

        assertEquals(
            stage.labelFor(checkNotNull(stage.boundsOf(TutorialAnchor.DueDateButton))),
            overlay.captionHungUnder
        )
    }

    @Test
    fun `should skip the caption when there is nothing on the page to hang it under`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.anchorAvailable = {
            it != TutorialAnchor.ListCreateRow &&
                it != TutorialAnchor.DueDateButton &&
                it != TutorialAnchor.TargetDateButton
        }

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

        directorFor(stage).play(TutorialStep.A_DAY_AND_A_NOTE)

        assertEquals(listOf("narrate:A_DAY_AND_A_NOTE", "banner:🛒 Groceries"), overlay.events)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should still advance the create list step when there is no banner content`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.banner = null

        directorFor(stage).play(TutorialStep.A_DAY_AND_A_NOTE)

        assertEquals(listOf("narrate:A_DAY_AND_A_NOTE"), overlay.events)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should ignore the create list step on the items screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.A_DAY_AND_A_NOTE)

        verify(exactly = 0) { viewModel.advanceStep() }
    }

    // ── SET_DUE_DATE ──

    @Test
    fun `should open the demo list then advance on the due date step`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).play(TutorialStep.OPEN_IT)

        assertEquals(listOf(TutorialAction.OpenFirstList), stage.actions)
        assertEquals(listOf("narrate:OPEN_IT", "glide:FirstListRow", "tap"), overlay.events)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should not advance the due date step when the list cannot be opened`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.canPerform = { it != TutorialAction.OpenFirstList }

        directorFor(stage).play(TutorialStep.OPEN_IT)

        verify(exactly = 0) { viewModel.advanceStep() }
        verify { viewModel.skip() }
    }

    @Test
    fun `should tap without gliding when the first list row has no bounds`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.anchorAvailable = { it != TutorialAnchor.FirstListRow }

        directorFor(stage).play(TutorialStep.OPEN_IT)

        assertEquals(listOf("narrate:OPEN_IT", "tap"), overlay.events)
    }

    @Test
    fun `should ignore the due date step on the items screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.OPEN_IT)

        assertTrue(stage.actions.isEmpty())
    }

    // ── OPEN_LIST ──

    @Test
    fun `should add both demo items then advance on the open list step`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.WRITE_ITEMS)

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

        directorFor(stage).play(TutorialStep.WRITE_ITEMS)

        assertTrue(stage.actions.contains(TutorialAction.TypeItemTitle("🍎 Apples")))
        assertTrue(stage.actions.contains(TutorialAction.TypeItemTitle("🥖 Bread")))
        assertEquals(2450L, testScheduler.currentTime)
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should ignore the open list step on the lists screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).play(TutorialStep.WRITE_ITEMS)

        assertTrue(stage.actions.isEmpty())
    }

    // ── COMPLETE_AND_REORDER ──

    @Test
    fun `should complete restore reorder and complete again then advance`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.TICK_AND_MOVE)

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

    /**
     * The one beat in the tour that teaches a row can be picked up must show it
     * being held. Moving the row and gliding the hand after it — which is what this
     * did — showed a row reordering itself and a disc arriving late.
     */
    @Test
    fun `should carry the row up the page under the hand rather than after it`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.TICK_AND_MOVE)

        val carrying = stage.timeline.indexOfFirst { it.startsWith("drag:") }
        val moved = stage.timeline.indexOf("do:MoveActiveItem")
        val lastCarry = stage.timeline.indexOfLast { it.startsWith("drag:") }

        assertTrue("${stage.timeline}", carrying in 0 until moved)
        assertTrue("${stage.timeline}", moved < lastCarry)
        assertTrue("${stage.timeline}", stage.timeline.count { it.startsWith("drag:") } > 1)
    }

    @Test
    fun `should carry the hand the whole way from the handle to the row it lands on`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.anchorAvailable = { it !is TutorialAnchor.ActiveItemToggle }

        directorFor(stage).play(TutorialStep.TICK_AND_MOVE)

        val handle = checkNotNull(stage.firstIssued("ActiveItemDragHandle"))
        val landing = checkNotNull(stage.firstIssued("ActiveItemRow"))
        val carried = overlay.carriedTo

        assertEquals(LIFTS_PER_DRAG, carried.size)
        assertEquals(handle.liftedTowards(landing, 1f).top, carried.last())
        assertTrue("$carried", carried.first() < carried.last())
        assertTrue("$carried", carried.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test
    fun `should keep hold of the row for the whole of the carry`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.TICK_AND_MOVE)

        val gripped = stage.timeline.indexOf("grip")
        val lastCarry = stage.timeline.indexOfLast { it.startsWith("drag:") }
        val released = stage.timeline.indexOf("release")

        assertTrue("${stage.timeline}", gripped in 0 until lastCarry)
        assertTrue("${stage.timeline}", lastCarry < released)
    }

    /**
     * Nine hundred milliseconds longer than it was, and almost all of it inside the
     * drag: the grip is held before the carry starts, and the carry itself is twelve
     * steps of the hand with the row changing places under it. A long-press-to-drag
     * that is over in a tenth of a second reads as a row that jumped by itself.
     */
    @Test
    fun `should hold the scripted pacing of the tick and move step`() = runTest {
        directorFor(RecordingStage(TutorialScreen.ITEMS)).play(TutorialStep.TICK_AND_MOVE)

        assertEquals(4810L, testScheduler.currentTime)
    }

    /**
     * What the tour costs a reader, end to end, counted in the rests the script
     * actually takes. `SPEC.md` quotes this number; when it changes, change it there
     * too rather than letting the two drift apart.
     *
     * It is the director's own rests only — the fake overlay does not hold a tap or
     * animate a glide — so the real thing is longer, and longer again on a slow
     * device where a spring takes what it takes.
     */
    @Test
    fun `should hold the scripted pacing of the whole tour`() = runTest {
        directorFor(RecordingStage(TutorialScreen.LISTS)).playOpening()
        for (step in TutorialStep.entries) {
            val screen = if (step == TutorialStep.WRITE_ITEMS || step == TutorialStep.TICK_AND_MOVE) {
                TutorialScreen.ITEMS
            } else {
                TutorialScreen.LISTS
            }
            directorFor(RecordingStage(screen)).play(step)
        }

        assertEquals(SCRIPTED_TOUR_MILLIS, testScheduler.currentTime)
    }

    /**
     * The principle behind the rebalance, not just its arithmetic: the scene that
     * teaches a gesture nobody discovers on their own is given more of the tour than
     * the scene that teaches typing into a line.
     */
    @Test
    fun `should give the gesture scenes more of the tour than the writing scene`() = runTest {
        directorFor(RecordingStage(TutorialScreen.ITEMS)).play(TutorialStep.WRITE_ITEMS)
        val writing = testScheduler.currentTime

        directorFor(RecordingStage(TutorialScreen.ITEMS)).play(TutorialStep.TICK_AND_MOVE)
        val gesturing = testScheduler.currentTime - writing

        assertTrue("$writing then $gesturing", gesturing > writing)
    }

    @Test
    fun `should skip the drag when no drag handle is present`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.anchorAvailable = { it != TutorialAnchor.ActiveItemDragHandle(1) }

        directorFor(stage).play(TutorialStep.TICK_AND_MOVE)

        assertTrue(stage.actions.none { it is TutorialAction.MoveActiveItem })
        assertTrue(stage.actions.none { it is TutorialAction.CommitReorder })
        verify { viewModel.advanceStep() }
    }

    @Test
    fun `should skip a toggle when its row is not on screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.anchorAvailable = { it != TutorialAnchor.CompletedItemToggle(0) }

        directorFor(stage).play(TutorialStep.TICK_AND_MOVE)

        assertTrue(stage.actions.none { it is TutorialAction.ToggleCompletedItem })
    }

    @Test
    fun `should ignore the complete and reorder step on the lists screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)

        directorFor(stage).play(TutorialStep.TICK_AND_MOVE)

        assertTrue(stage.actions.isEmpty())
    }

    // ── DELETE_LIST ──

    @Test
    fun `should navigate back without advancing when deleting from the items screen`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)

        directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

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

        directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

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

            directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

            val pulls = stage.actions
                .filterIsInstance<TutorialAction.PullFirstList>()
                .map { it.pixels }
            val towardsTheEditor = pulls.takeWhile { it > 0f }
            val towardsTheTear = pulls.dropWhile { it > 0f }

            /**
             * The stage hands out rows ten wide, so the reaches are exact: a
             * quarter of the row towards the editor and a little under a third of
             * it towards the tear, arrived at a fourteenth at a time. It used to be
             * six steps, each of which waited out a page-crossing spring; they are
             * shorter and there are more of them now, and the pull is the length of
             * a pull rather than of a haul.
             */
            assertEquals(PULLS_PER_SWIPE, towardsTheEditor.size)
            assertEquals(PULLS_PER_SWIPE, towardsTheTear.size)
            assertEquals(2.6f, towardsTheEditor.last(), PULL_TOLERANCE)
            assertEquals(-3.0f, towardsTheTear.last(), PULL_TOLERANCE)
            assertSteps(evenlyTo(2.6f, PULLS_PER_SWIPE), towardsTheEditor)
            assertSteps(evenlyTo(-3.0f, PULLS_PER_SWIPE), towardsTheTear)
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

        directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

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

            directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

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

        directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

        assertEquals(emptyList<TutorialAction>(), stage.actions)
        assertEquals(listOf("narrate:EDIT_AND_TEAR"), overlay.events)
        verify(exactly = 0) { viewModel.advanceStep() }
        verify { viewModel.skip() }
    }

    @Test
    fun `should abort the delete scene when the delete button refuses`() = runTest {
        val stage = RecordingStage(TutorialScreen.LISTS)
        stage.canPerform = { it != TutorialAction.RequestDeleteFirstList }

        directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

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

        directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

        assertEquals("ConfirmDeleteFirstList", stage.actions.beats().last())
        verify(exactly = 0) { viewModel.advanceStep() }
        verify { viewModel.skip() }
    }

    @Test
    fun `should end the tour when the demo cannot walk back out of the list`() = runTest {
        val stage = RecordingStage(TutorialScreen.ITEMS)
        stage.canPerform = { it != TutorialAction.NavigateBack }

        directorFor(stage).play(TutorialStep.EDIT_AND_TEAR)

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

        directorFor(stage).play(TutorialStep.WRITE_ITEMS)

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

        directorFor(RecordingStage(TutorialScreen.ITEMS)).play(TutorialStep.WRITE_ITEMS)

        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun `should end the rest that is already being taken when the reader says so`() = runTest {
        val director = directorFor(RecordingStage(TutorialScreen.ITEMS))
        val scene = launch { director.play(TutorialStep.WRITE_ITEMS) }

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

        /**
         * The stage's beats and the overlay's, in the order they happened. Kept
         * here rather than in either one because the only questions worth asking of
         * a drag are about the order the two of them went in.
         */
        val timeline = mutableListOf<String>()
        var canPerform: (TutorialAction) -> Boolean = { true }
        var anchorAvailable: (TutorialAnchor) -> Boolean = { true }
        var demoListId: String? = "demo-list"
        var banner: TutorialBannerContent? = null
        var abandoned = 0

        private val issued = mutableMapOf<TutorialBounds, String>()
        private var nextLeft = 1

        private companion object {
            const val ROW_PITCH = 10
        }

        /**
         * Rows ten wide and ten tall, each one lower down the page than the last.
         * They all used to sit at the same top, which made every question about
         * where the hand was carried have the same answer.
         */
        override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? {
            if (!anchorAvailable(anchor)) return null
            val bounds = TutorialBounds(nextLeft, nextLeft * ROW_PITCH, 10, 10)
            nextLeft++
            issued[bounds] = anchor::class.simpleName.orEmpty()
            return bounds
        }

        /**
         * A beat the hand travels across rather than lands on derives its point
         * from the row's own bounds, so a derived point is named for the row it
         * came from.
         */
        /**
         * The first rectangle handed out for a kind of anchor. Bounds are minted
         * fresh on every ask, so asking again after the scene has run gives a
         * different row than the one the scene was driven with.
         */
        fun firstIssued(label: String): TutorialBounds? =
            issued.entries.firstOrNull { it.value == label }?.key

        fun labelFor(bounds: TutorialBounds): String = issued[bounds]
            ?: issued.entries
                .firstOrNull { (issued, _) ->
                    issued.top == bounds.top && issued.height == bounds.height
                }
                ?.value
                .orEmpty()

        override suspend fun perform(action: TutorialAction): Boolean {
            timeline.add("do:${action::class.simpleName}")
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

        private fun note(event: String) {
            events.add(event)
            stage.timeline.add(event)
        }

        override suspend fun narrate(line: TutorialLine) {
            note("narrate:$line")
            yield()
        }

        override suspend fun glideTo(bounds: TutorialBounds, durationMillis: Long) {
            note("glide:${stage.labelFor(bounds)}")
            yield()
        }

        val carriedTo = mutableListOf<Int>()

        override suspend fun dragTo(bounds: TutorialBounds) {
            note("drag:${stage.labelFor(bounds)}")
            carriedTo.add(bounds.top)
            yield()
        }

        override suspend fun tap() {
            note("tap")
            yield()
        }

        override suspend fun grip() {
            note("grip")
            yield()
        }

        override suspend fun release() {
            note("release")
            yield()
        }

        var captionHungUnder: String? = null
            private set

        override suspend fun showCaption(caption: TutorialCaption, below: TutorialBounds) {
            events.add("caption:$caption")
            captionHungUnder = stage.labelFor(below)
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
class LiftedTowardsTest {

    private val handle = TutorialBounds(left = 4, top = 100, width = 20, height = 40)
    private val landing = TutorialBounds(left = 9, top = 300, width = 20, height = 60)

    @Test
    fun `should start a carry at the middle of the row being picked up`() {
        assertEquals(120, handle.liftedTowards(landing, 0f).top)
    }

    @Test
    fun `should end a carry at the middle of the row being landed on`() {
        assertEquals(330, handle.liftedTowards(landing, 1f).top)
    }

    @Test
    fun `should be halfway between the two middles halfway through a carry`() {
        assertEquals(225, handle.liftedTowards(landing, 0.5f).top)
    }

    @Test
    fun `should carry a row upwards when it is landing above where it started`() {
        assertEquals(120, landing.liftedTowards(handle, 1f).top)
        assertEquals(330, landing.liftedTowards(handle, 0f).top)
    }

    /**
     * A hand dragging by a handle stays over the handle, so the column is the one
     * the carry started in and only the height collapses.
     */
    @Test
    fun `should keep the carrying hand in the column it gripped in`() {
        val carried = handle.liftedTowards(landing, 0.5f)

        assertEquals(handle.left, carried.left)
        assertEquals(handle.width, carried.width)
        assertEquals(0, carried.height)
    }
}

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
