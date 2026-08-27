package fr.mandarine.todolist.ui.tutorial

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialStep
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialCaption
import fr.mandarine.todolist.presentation.TutorialUiState
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialOverlayStateTest {

    @Test
    fun `should centre the hand on the anchor it points at`() {
        val target = handTargetFor(
            bounds = TutorialBounds(left = 100, top = 200, width = 60, height = 40),
            originX = 0,
            originY = 0,
            handSizePx = 20f
        )

        assertEquals(120f, target.x, 0f)
        assertEquals(210f, target.y, 0f)
    }

    @Test
    fun `should subtract the overlay origin from a screen anchor`() {
        val target = handTargetFor(
            bounds = TutorialBounds(left = 100, top = 200, width = 60, height = 40),
            originX = 30,
            originY = 70,
            handSizePx = 20f
        )

        assertEquals(90f, target.x, 0f)
        assertEquals(140f, target.y, 0f)
    }

    @Test
    fun `should place the caption a gap below the anchor it explains`() {
        val top = captionTopFor(
            below = TutorialBounds(left = 0, top = 300, width = 100, height = 80),
            originY = 50,
            gapPx = 12f
        )

        assertEquals(342f, top, 0f)
    }

    @Test
    fun `should fill one dot before the first step starts`() {
        assertEquals(1, filledDotsFor(TutorialUiState.ReadyToStart))
    }

    @Test
    fun `should fill one dot per step reached`() {
        assertEquals(2, filledDotsFor(TutorialUiState.Active(TutorialStep.A_DAY_AND_A_NOTE)))
        assertEquals(4, filledDotsFor(TutorialUiState.Active(TutorialStep.WRITE_ITEMS)))
        assertEquals(6, filledDotsFor(TutorialUiState.Active(TutorialStep.EDIT_AND_TEAR)))
    }

    /**
     * The opening scene is a scene. It used to share the first dot with the beat
     * after it, so the longest and busiest part of the tour showed no progress at
     * all until it was over.
     */
    @Test
    fun `should give the opening scene a dot of its own`() {
        assertEquals(1, filledDotsFor(TutorialUiState.ReadyToStart))
        assertEquals(2, filledDotsFor(TutorialUiState.Active(TutorialStep.A_DAY_AND_A_NOTE)))
    }

    @Test
    fun `should have one dot on the pill for every scene of the tour`() {
        assertEquals(PROGRESS_DOT_COUNT, filledDotsFor(TutorialUiState.Active(TutorialStep.EDIT_AND_TEAR)))
        assertEquals(PROGRESS_DOT_COUNT, TutorialStep.entries.size + 1)
    }

    @Test
    fun `should fill no dot while the tutorial is not running`() {
        assertEquals(0, filledDotsFor(TutorialUiState.Hidden))
        assertEquals(0, filledDotsFor(TutorialUiState.Dismissed))
    }

    @Test
    fun `should hold the banner above its own top edge before it slides in`() {
        val hidden = bannerTranslationFor(
            progress = 0f,
            heightPx = 120f,
            gapPx = 32f,
            statusBarPx = 60f
        )

        assertEquals(-152f, hidden, 0f)
    }

    @Test
    fun `should rest the banner on the status bar once it has slid in`() {
        val shown = bannerTranslationFor(
            progress = 1f,
            heightPx = 120f,
            gapPx = 32f,
            statusBarPx = 60f
        )

        assertEquals(60f, shown, 0f)
    }

    @Test
    fun `should darken the rim as the hand presses into a tap`() {
        assertEquals(HAND_RIM_ALPHA_REST, handRimAlpha(1f), 0.001f)
        assertEquals(HAND_RIM_ALPHA_PRESSED, handRimAlpha(HAND_TAP_SCALE), 0.001f)
        assertTrue(handRimAlpha(0.86f) > HAND_RIM_ALPHA_REST)
    }

    @Test
    fun `should hold the hand against the page for the whole of a grip`() {
        assertEquals(1f, handPress(HAND_GRIP_SCALE), 0.001f)
        assertEquals(HAND_RIM_ALPHA_PRESSED, handRimAlpha(HAND_GRIP_SCALE), 0.001f)
    }

    @Test
    fun `should read the hand as lifted while it is merely resting`() {
        assertEquals(0f, handPress(1f), 0.001f)
        assertEquals(0f, handPress(1.4f), 0.001f)
    }

    @Test
    fun `should remember which anchor the hand was aimed at`() {
        val state = TutorialOverlayState()

        assertNull(state.aimedAnchor)

        state.aimAt(TutorialAnchor.CreateListButton, TutorialBounds(0, 0, 10, 10))

        assertEquals(TutorialAnchor.CreateListButton, state.aimedAnchor)
    }

    @Test
    fun `should aim the hand at nothing when the anchor does not resolve`() {
        val state = TutorialOverlayState()
        state.aimAt(TutorialAnchor.CreateListButton, TutorialBounds(0, 0, 10, 10))

        state.aimAt(TutorialAnchor.DeleteListButton, null)

        assertNull(state.aimedAnchor)
    }

    @Test
    fun `should hand back the bounds it was asked to aim at`() {
        val state = TutorialOverlayState()
        val bounds = TutorialBounds(left = 4, top = 5, width = 6, height = 7)

        assertEquals(bounds, state.aimAt(TutorialAnchor.FirstListRow, bounds))
        assertNull(state.aimAt(TutorialAnchor.FirstListRow, null))
    }

    @Test
    fun `should take the hand off the page again when the tutorial begins`() = onFrames {
        val state = TutorialOverlayState()
        state.aimAt(TutorialAnchor.CreateListButton, TutorialBounds(0, 0, 10, 10))

        state.begin()

        assertNull(state.aimedAnchor)
    }

    @Test
    fun `should stay hidden until the tutorial begins`() {
        val state = TutorialOverlayState()

        assertFalse(state.visible)
    }

    @Test
    fun `should park the hand off screen when the tutorial begins`() = onFrames {
        val state = TutorialOverlayState()
        state.handSizePx = 20f
        state.glideTo(TutorialBounds(0, 0, 10, 10), 20)

        state.begin()

        assertTrue(state.visible)
        assertEquals(HAND_PARKED_Y, state.hand.value.y, 0f)
        assertEquals(1f, state.overlayAlpha.value, 0f)
    }

    @Test
    fun `should glide the hand onto the anchor it is pointed at`() = onFrames {
        val state = TutorialOverlayState()
        state.handSizePx = 20f
        state.begin()

        state.glideTo(TutorialBounds(left = 100, top = 200, width = 60, height = 40), 60)

        assertEquals(Offset(120f, 210f), state.hand.value)
    }

    @Test
    fun `should return the hand to its resting size after a tap`() = onFrames {
        val state = TutorialOverlayState()

        state.tap()

        assertEquals(1f, state.handScale.value, 0.001f)
    }

    @Test
    fun `should press the hand down while it grips and lift it again on release`() = onFrames {
        val state = TutorialOverlayState()

        state.grip()
        assertEquals(HAND_GRIP_SCALE, state.handScale.value, 0.001f)

        state.release()
        assertEquals(1f, state.handScale.value, 0.001f)
    }

    @Test
    fun `should show the caption below the row it explains`() = onFrames {
        val state = TutorialOverlayState()
        state.captionGapPx = 12f

        state.showCaption(
            TutorialCaption.TARGET_DATE,
            TutorialBounds(left = 0, top = 300, width = 100, height = 80)
        )

        assertEquals(TutorialCaption.TARGET_DATE, state.caption)
        assertEquals(392f, state.captionTop, 0f)
        assertEquals(1f, state.captionAlpha.value, 0.001f)
    }

    @Test
    fun `should swap the caption without moving the slip`() = onFrames {
        val state = TutorialOverlayState()
        state.showCaption(TutorialCaption.TARGET_DATE, TutorialBounds(0, 300, 100, 80))
        val top = state.captionTop

        state.updateCaption(TutorialCaption.DUE_DATE)

        assertEquals(TutorialCaption.DUE_DATE, state.caption)
        assertEquals(top, state.captionTop, 0f)
        assertEquals(1f, state.captionTextAlpha.value, 0.001f)
    }

    @Test
    fun `should drop the caption when it is hidden`() = onFrames {
        val state = TutorialOverlayState()
        state.showCaption(TutorialCaption.TARGET_DATE, TutorialBounds(0, 300, 100, 80))

        state.hideCaption()

        assertNull(state.caption)
        assertEquals(0f, state.captionAlpha.value, 0.001f)
    }

    @Test
    fun `should drop the banner once it has slid back out`() = onFrames {
        val state = TutorialOverlayState()

        state.showBanner(TutorialBannerContent("Groceries", LocalDate.of(2026, 3, 14)))

        assertNull(state.banner)
        assertEquals(0f, state.bannerProgress.value, 0.001f)
    }

    @Test
    fun `should fade the overlay away when the tutorial is dismissed`() = onFrames {
        val state = TutorialOverlayState()
        state.begin()

        state.fadeOut()

        assertFalse(state.visible)
        assertEquals(0f, state.overlayAlpha.value, 0.001f)
    }

    @Test
    fun `should leave a hidden overlay opaque so a replay can show it again`() = onFrames {
        val state = TutorialOverlayState()

        state.fadeOut()

        assertEquals(1f, state.overlayAlpha.value, 0f)
    }

    @Test
    fun `should show the overlay without disturbing the hand when a screen is entered`() = onFrames {
        val state = TutorialOverlayState()
        state.handSizePx = 20f
        state.begin()
        state.glideTo(TutorialBounds(100, 200, 60, 40), 40)

        state.show()

        assertTrue(state.visible)
        assertEquals(Offset(120f, 210f), state.hand.value)
    }

    @Test
    fun `should read anchor bounds against the origin it was told about`() = onFrames {
        val state = TutorialOverlayState()
        state.handSizePx = 20f
        state.setOrigin(TutorialBounds(left = 30, top = 70, width = 1080, height = 1920))

        state.glideTo(TutorialBounds(left = 100, top = 200, width = 60, height = 40), 40)

        assertEquals(Offset(90f, 140f), state.hand.value)
    }

    private fun onFrames(block: suspend () -> Unit) = runTest {
        withContext(ImmediateFrameClock()) { block() }
    }

    private class ImmediateFrameClock : MonotonicFrameClock {
        private var nanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            nanos += FRAME_NANOS
            return onFrame(nanos)
        }

        private companion object {
            const val FRAME_NANOS = 16_000_000L
        }
    }
}
