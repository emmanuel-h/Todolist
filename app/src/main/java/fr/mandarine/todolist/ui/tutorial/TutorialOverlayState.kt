package fr.mandarine.todolist.ui.tutorial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialCaption
import fr.mandarine.todolist.presentation.TutorialLine
import fr.mandarine.todolist.presentation.TutorialOverlay
import fr.mandarine.todolist.presentation.TutorialPace
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.ui.paper.PaperMotion

internal const val HAND_PARKED_Y = 4000f
internal const val HAND_TAP_SCALE = 0.72f
internal const val HAND_GRIP_SCALE = HAND_TAP_SCALE
internal const val PROGRESS_DOT_COUNT = 6

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
class TutorialOverlayState(private val pace: TutorialPace = TutorialPace()) : TutorialOverlay {

    /**
     * Whether a demo is on the paper at all, which is a moment wider than
     * [visible] — the opening beat is running before the hand has arrived. Back
     * cancels the tour for the whole of it.
     */
    var running by mutableStateOf(false)

    var visible by mutableStateOf(false)
        private set
    var filledDots by mutableStateOf(0)
    var caption by mutableStateOf<TutorialCaption?>(null)
        private set
    var captionTop by mutableStateOf(0f)
        private set
    var banner by mutableStateOf<TutorialBannerContent?>(null)
        private set

    var narration by mutableStateOf<TutorialLine?>(null)
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
    val narrationAlpha = Animatable(0f)
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
        narrationAlpha.snapTo(PARKED)
        overlayAlpha.snapTo(PARKED)
        caption = null
        banner = null
        narration = null
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
     *
     * A reader who has said they have seen enough is not shown the travel at all:
     * the hand is already wherever the rest of the scene needs it.
     */
    /**
     * The same line twice is one line, not a blink: a scene replayed on a page that
     * caught up a frame late must not make the slip flicker out and back.
     */
    override suspend fun narrate(line: TutorialLine) {
        if (narration == line) return
        if (narration != null) fade(narrationAlpha, PARKED, PaperMotion.rowExit)
        narration = line
        fade(narrationAlpha, SHOWN, PaperMotion.rowEnter)
    }

    override suspend fun glideTo(bounds: TutorialBounds, durationMillis: Long) {
        val target = handTargetFor(bounds, originX, originY, handSizePx)
        if (pace.hurrying) hand.snapTo(target) else hand.animateTo(target, PaperMotion.handGlide)
    }

    override suspend fun dragTo(bounds: TutorialBounds) {
        hand.snapTo(handTargetFor(bounds, originX, originY, handSizePx))
    }

    override suspend fun tap() {
        press(HAND_TAP_SCALE)
        pace.beat(TAP_HOLD_MILLIS)
        press(RESTING)
    }

    override suspend fun grip() {
        press(HAND_GRIP_SCALE)
    }

    override suspend fun release() {
        press(RESTING)
    }

    override suspend fun showCaption(caption: TutorialCaption, below: TutorialBounds) {
        this.caption = caption
        captionTop = captionTopFor(below, originY, captionGapPx)
        captionTextAlpha.snapTo(SHOWN)
        captionAlpha.snapTo(PARKED)
        fade(captionAlpha, SHOWN, PaperMotion.rowEnter)
    }

    override suspend fun updateCaption(caption: TutorialCaption) {
        fade(captionTextAlpha, PARKED, PaperMotion.rowExit)
        this.caption = caption
        fade(captionTextAlpha, SHOWN, PaperMotion.rowEnter)
    }

    override suspend fun hideCaption() {
        fade(captionAlpha, PARKED, PaperMotion.rowExit)
        caption = null
    }

    /**
     * The banner takes the slip rather than dimming it. It used to be shown over a
     * narration that was only faded out by how far the banner had slid in — so when
     * the banner slid away the sentence faded back up, sat there for half a second,
     * and was immediately replaced by the next scene's. Three things arriving at the
     * top of the page in two seconds, and the one worth reading was the middle one.
     */
    override suspend fun showBanner(content: TutorialBannerContent) {
        if (narration != null) {
            fade(narrationAlpha, PARKED, PaperMotion.rowExit)
            narration = null
        }
        banner = content
        bannerProgress.snapTo(PARKED)
        fade(bannerProgress, SHOWN, PaperMotion.sheetSettle)
        pace.beat(BANNER_HOLD_MILLIS)
        fade(bannerProgress, PARKED, PaperMotion.sheetSettle)
        banner = null
    }

    private suspend fun press(scale: Float) {
        if (pace.hurrying) handScale.snapTo(scale) else handScale.animateTo(scale, PaperMotion.pickUp)
    }

    private suspend fun fade(
        alpha: Animatable<Float, AnimationVector1D>,
        to: Float,
        spec: AnimationSpec<Float>
    ) {
        if (pace.hurrying) alpha.snapTo(to) else alpha.animateTo(to, spec)
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

/**
 * The opening scene — the pad taken, the name written, a day circled and the line
 * committed — is a scene, and it now has a dot of its own. It used to share the
 * first dot with the beat that follows it, so the tour's longest and busiest scene
 * showed no progress at all and the pill did not move until a third of it was over.
 */
internal fun filledDotsFor(state: TutorialUiState): Int = when (state) {
    TutorialUiState.ReadyToStart -> OPENING_DOT
    is TutorialUiState.Active -> state.step.ordinal + OPENING_DOT + 1
    else -> 0
}

private const val OPENING_DOT = 1

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
