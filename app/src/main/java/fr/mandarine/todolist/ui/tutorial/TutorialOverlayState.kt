package fr.mandarine.todolist.ui.tutorial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialCaption
import fr.mandarine.todolist.presentation.TutorialOverlay
import fr.mandarine.todolist.presentation.TutorialUiState
import kotlinx.coroutines.delay

internal const val HAND_PARKED_Y = 4000f
internal const val HAND_TAP_SCALE = 0.72f
internal const val HAND_GRIP_SCALE = 1.15f
internal const val PROGRESS_DOT_COUNT = 5

private const val TAP_MILLIS = 100
private const val TAP_HOLD_MILLIS = 130L
private const val GRIP_MILLIS = 150
private const val CAPTION_FADE_MILLIS = 300
private const val CAPTION_SWAP_MILLIS = 150
private const val BANNER_SLIDE_MILLIS = 350
private const val BANNER_HOLD_MILLIS = 2200L
private const val OVERLAY_FADE_MILLIS = 500

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

    fun show() {
        visible = true
    }

    suspend fun begin() {
        hand.snapTo(Offset(0f, HAND_PARKED_Y))
        handScale.snapTo(1f)
        captionAlpha.snapTo(0f)
        captionTextAlpha.snapTo(1f)
        bannerProgress.snapTo(0f)
        overlayAlpha.snapTo(1f)
        caption = null
        banner = null
        visible = true
    }

    suspend fun fadeOut() {
        if (!visible) return
        overlayAlpha.animateTo(0f, tween(OVERLAY_FADE_MILLIS))
        visible = false
    }

    override suspend fun glideTo(bounds: TutorialBounds, durationMillis: Long) {
        hand.animateTo(
            targetValue = handTargetFor(bounds, originX, originY, handSizePx),
            animationSpec = tween(durationMillis.toInt())
        )
    }

    override suspend fun tap() {
        handScale.animateTo(HAND_TAP_SCALE, tween(TAP_MILLIS))
        delay(TAP_HOLD_MILLIS)
        handScale.animateTo(1f, tween(TAP_MILLIS))
    }

    override suspend fun grip() {
        handScale.animateTo(HAND_GRIP_SCALE, tween(GRIP_MILLIS))
    }

    override suspend fun release() {
        handScale.animateTo(1f, tween(GRIP_MILLIS))
    }

    override suspend fun showCaption(caption: TutorialCaption, below: TutorialBounds) {
        this.caption = caption
        captionTop = captionTopFor(below, originY, captionGapPx)
        captionTextAlpha.snapTo(1f)
        captionAlpha.snapTo(0f)
        captionAlpha.animateTo(1f, tween(CAPTION_FADE_MILLIS))
    }

    override suspend fun updateCaption(caption: TutorialCaption) {
        captionTextAlpha.animateTo(0f, tween(CAPTION_SWAP_MILLIS))
        this.caption = caption
        captionTextAlpha.animateTo(1f, tween(CAPTION_SWAP_MILLIS))
    }

    override suspend fun hideCaption() {
        captionAlpha.animateTo(0f, tween(CAPTION_FADE_MILLIS))
        caption = null
    }

    override suspend fun showBanner(content: TutorialBannerContent) {
        banner = content
        bannerProgress.snapTo(0f)
        bannerProgress.animateTo(1f, tween(BANNER_SLIDE_MILLIS))
        delay(BANNER_HOLD_MILLIS)
        bannerProgress.animateTo(0f, tween(BANNER_SLIDE_MILLIS))
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
 * The rim is a fingertip pressing the page: it darkens as the hand shrinks into a
 * tap and stays light while the hand is merely resting or gripping.
 */
internal fun handRimAlpha(scale: Float): Float {
    val press = ((1f - scale) / (1f - HAND_TAP_SCALE)).coerceIn(0f, 1f)
    return HAND_RIM_ALPHA_REST + (HAND_RIM_ALPHA_PRESSED - HAND_RIM_ALPHA_REST) * press
}

internal const val HAND_RIM_ALPHA_REST = 0.55f
internal const val HAND_RIM_ALPHA_PRESSED = 0.9f
