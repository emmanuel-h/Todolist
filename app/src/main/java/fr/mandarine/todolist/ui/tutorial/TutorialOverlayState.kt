package fr.mandarine.todolist.ui.tutorial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialCaption
import fr.mandarine.todolist.presentation.TutorialOverlay
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.ui.paper.PaperMotion
import kotlinx.coroutines.delay

internal const val HAND_PARKED_Y = 4000f
internal const val HAND_TAP_SCALE = 0.72f
internal const val HAND_GRIP_SCALE = HAND_TAP_SCALE
internal const val PROGRESS_DOT_COUNT = 5

private const val TAP_HOLD_MILLIS = 130L
private const val BANNER_HOLD_MILLIS = 2200L
private const val PARKED = 0f
private const val SHOWN = 1f
private const val RESTING = 1f

/**
 * The overlay is composed for the whole life of the screen and shown by state, so
 * there is nothing to attach or detach. The view version had to re-add itself to
 * the decor view on replay, and to guard that with a test; neither is possible now.
 */
class TutorialOverlayState : TutorialOverlay {

    var visible by mutableStateOf(false)
        private set
    var filledDots by mutableStateOf(0)
    var caption by mutableStateOf<TutorialCaption?>(null)
        private set
    var captionTop by mutableStateOf(0f)
        private set
    var banner by mutableStateOf<TutorialBannerContent?>(null)
        private set

    /**
     * A beat whose control no longer exists resolves to no bounds at all, and a
     * control can also leave under the finger that just used it. Either way the
     * hand comes off the page rather than hovering over where the thing used to
     * be, which is what a stale aim looked like.
     */
    var aimedAnchor by mutableStateOf<TutorialAnchor?>(null)
        private set

    val hand = Animatable(Offset(0f, HAND_PARKED_Y), Offset.VectorConverter)
    val handScale = Animatable(1f)
    val captionAlpha = Animatable(0f)
    val captionTextAlpha = Animatable(1f)
    val bannerProgress = Animatable(0f)
    val overlayAlpha = Animatable(1f)

    /**
     * Anchors are reported in screen coordinates because the hand used to live in a
     * window overlay. It now lives in the screen's own composition, so every target
     * is offset by where that composition sits on the screen.
     */
    internal var originX = 0
        private set
    internal var originY = 0
        private set
    internal var handSizePx = 0f
    internal var captionGapPx = 0f

    fun setOrigin(bounds: TutorialBounds) {
        originX = bounds.left
        originY = bounds.top
    }

    fun aimAt(anchor: TutorialAnchor, bounds: TutorialBounds?): TutorialBounds? {
        aimedAnchor = if (bounds == null) null else anchor
        return bounds
    }

    fun show() {
        visible = true
    }

    /**
     * The overlay arrives the way it leaves — the page is not hidden behind it in
     * one frame — and the hand is parked off the paper until the first beat calls
     * it on.
     */
    suspend fun begin() {
        hand.snapTo(Offset(PARKED, HAND_PARKED_Y))
        handScale.snapTo(RESTING)
        captionAlpha.snapTo(PARKED)
        captionTextAlpha.snapTo(SHOWN)
        bannerProgress.snapTo(PARKED)
        overlayAlpha.snapTo(PARKED)
        caption = null
        banner = null
        aimedAnchor = null
        visible = true
        overlayAlpha.animateTo(SHOWN, PaperMotion.rowEnter)
    }

    suspend fun fadeOut() {
        if (!visible) return
        overlayAlpha.animateTo(PARKED, PaperMotion.rowExit)
        visible = false
    }

    /**
     * The script asks for a duration, but a hand that crosses the page in the time
     * a hand crosses a row reads as two different hands. It travels on the page's
     * own spring instead, so a long reach takes longer than a short one.
     */
    override suspend fun glideTo(bounds: TutorialBounds, durationMillis: Long) {
        hand.animateTo(
            targetValue = handTargetFor(bounds, originX, originY, handSizePx),
            animationSpec = PaperMotion.handGlide
        )
    }

    override suspend fun tap() {
        handScale.animateTo(HAND_TAP_SCALE, PaperMotion.pickUp)
        delay(TAP_HOLD_MILLIS)
        handScale.animateTo(RESTING, PaperMotion.pickUp)
    }

    override suspend fun grip() {
        handScale.animateTo(HAND_GRIP_SCALE, PaperMotion.pickUp)
    }

    override suspend fun release() {
        handScale.animateTo(RESTING, PaperMotion.pickUp)
    }

    override suspend fun showCaption(caption: TutorialCaption, below: TutorialBounds) {
        this.caption = caption
        captionTop = captionTopFor(below, originY, captionGapPx)
        captionTextAlpha.snapTo(SHOWN)
        captionAlpha.snapTo(PARKED)
        captionAlpha.animateTo(SHOWN, PaperMotion.rowEnter)
    }

    override suspend fun updateCaption(caption: TutorialCaption) {
        captionTextAlpha.animateTo(PARKED, PaperMotion.rowExit)
        this.caption = caption
        captionTextAlpha.animateTo(SHOWN, PaperMotion.rowEnter)
    }

    override suspend fun hideCaption() {
        captionAlpha.animateTo(PARKED, PaperMotion.rowExit)
        caption = null
    }

    override suspend fun showBanner(content: TutorialBannerContent) {
        banner = content
        bannerProgress.snapTo(PARKED)
        bannerProgress.animateTo(SHOWN, PaperMotion.sheetSettle)
        delay(BANNER_HOLD_MILLIS)
        bannerProgress.animateTo(PARKED, PaperMotion.sheetSettle)
        banner = null
    }
}

internal fun handTargetFor(
    bounds: TutorialBounds,
    originX: Int,
    originY: Int,
    handSizePx: Float
): Offset = Offset(
    x = bounds.left - originX + bounds.width / 2f - handSizePx / 2f,
    y = bounds.top - originY + bounds.height / 2f - handSizePx / 2f
)

internal fun captionTopFor(below: TutorialBounds, originY: Int, gapPx: Float): Float =
    below.top - originY + below.height + gapPx

internal fun filledDotsFor(state: TutorialUiState): Int = when (state) {
    TutorialUiState.ReadyToStart -> 1
    is TutorialUiState.Active -> state.step.ordinal + 1
    else -> 0
}

/**
 * The banner slides in from above its own top edge, so the travel is only known
 * once the slip has been measured — the draw layer reads its own height rather
 * than the composable measuring itself twice.
 */
internal fun bannerTranslationFor(
    progress: Float,
    heightPx: Float,
    gapPx: Float,
    statusBarPx: Float
): Float {
    val hidden = -(heightPx + gapPx)
    return hidden + (statusBarPx - hidden) * progress
}

/**
 * How far the fingertip has come down onto the paper: the disc squashes, its
 * shadow collapses under it and its rim darkens, all read from the one scale the
 * script animates.
 */
internal fun handPress(scale: Float): Float =
    ((1f - scale) / (1f - HAND_TAP_SCALE)).coerceIn(0f, 1f)

internal fun handRimAlpha(scale: Float): Float =
    HAND_RIM_ALPHA_REST + (HAND_RIM_ALPHA_PRESSED - HAND_RIM_ALPHA_REST) * handPress(scale)

internal const val HAND_RIM_ALPHA_REST = 0.6f
internal const val HAND_RIM_ALPHA_PRESSED = 0.9f
