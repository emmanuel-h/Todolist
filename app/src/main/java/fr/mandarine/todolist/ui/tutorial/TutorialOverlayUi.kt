package fr.mandarine.todolist.ui.tutorial

import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialCaption
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.paperSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HAND_SIZE = 44.dp
private val HAND_RIM = 1.dp
private const val HAND_DISC_ALPHA = 0.55f
private const val HAND_PRESS_SQUASH = 0.08f
private const val HAND_SHADOW_ALPHA = 0.18f
private val HAND_SHADOW_LIFTED = 10.dp
private val HAND_SHADOW_TOUCHING = 2.dp
private val HAND_DROP_LIFTED = 4.dp
private val HAND_DROP_TOUCHING = 1.dp
private const val HAND_AIM_LABEL = "handAim"
private const val OFF_THE_PAGE = 0f
private const val ON_THE_PAGE = 1f

private val SLIP_PADDING = 16.dp
private val CAPTION_PADDING = 14.dp
private val CAPTION_GAP = 12.dp
private val BANNER_MARGIN = 12.dp
private val BANNER_GAP = 32.dp
private val PROGRESS_BOTTOM_MARGIN = 12.dp
private val PROGRESS_DOT_SIZE = 8.dp
private val PROGRESS_DOT_GAP = 6.dp

/**
 * A shadowless slip of paper laid on the page, the same construction the rename
 * dialog uses. The view overlay drew three elevated Material cards, which were the
 * last drop shadows left in the app.
 */
@Composable
private fun Modifier.paperSlip(): Modifier {
    val palette = LocalPaperPalette.current
    return paperSheet(tone = palette.paperShade).border(PaperDimens.rule, palette.rule)
}

/**
 * The demo drives the screen underneath itself, so while it runs that screen is
 * not something anyone may touch — nor something a screen reader may wander into
 * and read out from under the narration.
 */
fun Modifier.behindTutorial(active: Boolean): Modifier =
    if (active) semantics { hideFromAccessibility() } else this

@Composable
fun TutorialOverlay(
    state: TutorialOverlayState,
    anchors: TutorialAnchorHost,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.visible) return

    val root = LocalView.current
    val density = LocalDensity.current
    SideEffect {
        state.handSizePx = with(density) { HAND_SIZE.toPx() }
        state.captionGapPx = with(density) { CAPTION_GAP.toPx() }
    }
    val statusBarPx = WindowInsets.statusBars.getTop(density).toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { state.setOrigin(it.screenBounds(root)) }
            .graphicsLayer { alpha = state.overlayAlpha.value }
            .pointerInput(Unit) { swallowTouches() }
    ) {
        state.banner?.let { BannerSlip(it, state, statusBarPx) }
        PhantomHand(state, anchors)
        state.caption?.let { CaptionSlip(it, state) }
        ProgressSlip(
            filledDots = state.filledDots,
            onSkip = onSkip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = PROGRESS_BOTTOM_MARGIN)
        )
    }
}

/**
 * A fingertip of paper rather than a blue wash: blue belongs to the live caret and
 * the wet tick. It rests above the page on a warm shadow, and the shadow collapses
 * under it as the finger comes down, so a tap reads as contact and not as a fade.
 */
@Composable
private fun PhantomHand(state: TutorialOverlayState, anchors: TutorialAnchorHost) {
    val palette = LocalPaperPalette.current
    val disc = palette.paperSheet
    val rimInk = palette.pencil
    val shadowInk = palette.ink
    val onPage by remember(state, anchors) {
        derivedStateOf { state.aimedAnchor?.let { anchors.boundsOf(it) } != null }
    }
    val aim = animateFloatAsState(
        targetValue = if (onPage) ON_THE_PAGE else OFF_THE_PAGE,
        animationSpec = PaperMotion.rowExit,
        label = HAND_AIM_LABEL
    )
    Box(
        Modifier
            .size(HAND_SIZE)
            .graphicsLayer {
                translationX = state.hand.value.x
                translationY = state.hand.value.y
                alpha = aim.value
                val squash = ON_THE_PAGE - HAND_PRESS_SQUASH * handPress(state.handScale.value)
                scaleX = squash
                scaleY = squash
            }
            .dropShadow(CircleShape) {
                val press = handPress(state.handScale.value)
                radius = lerp(HAND_SHADOW_LIFTED, HAND_SHADOW_TOUCHING, press).toPx()
                offset = Offset(
                    OFF_THE_PAGE,
                    lerp(HAND_DROP_LIFTED, HAND_DROP_TOUCHING, press).toPx()
                )
                alpha = HAND_SHADOW_ALPHA
                color = shadowInk
            }
            .drawBehind {
                val rim = HAND_RIM.toPx()
                drawCircle(disc.copy(alpha = HAND_DISC_ALPHA))
                drawCircle(
                    color = rimInk.copy(alpha = handRimAlpha(state.handScale.value)),
                    radius = size.minDimension / 2f - rim / 2f,
                    style = Stroke(width = rim)
                )
            }
            .clearAndSetSemantics {}
    )
}

@Composable
private fun BoxScope.BannerSlip(
    content: TutorialBannerContent,
    state: TutorialOverlayState,
    statusBarPx: Float
) {
    val gapPx = with(LocalDensity.current) { BANNER_GAP.toPx() }
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(BANNER_MARGIN)
            .graphicsLayer {
                translationY = bannerTranslationFor(
                    progress = state.bannerProgress.value,
                    heightPx = size.height,
                    gapPx = gapPx,
                    statusBarPx = statusBarPx
                )
                alpha = if (size.height > 0f) 1f else 0f
            }
            .paperSlip()
    ) {
        Text(
            text = bannerTextFor(content),
            modifier = Modifier.padding(SLIP_PADDING),
            color = LocalPaperPalette.current.ink,
            style = PaperType.prose
        )
    }
}

@Composable
private fun BoxScope.CaptionSlip(caption: TutorialCaption, state: TutorialOverlayState) {
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .graphicsLayer {
                alpha = state.captionAlpha.value
                translationY = state.captionTop
            }
            .paperSlip()
    ) {
        Text(
            text = captionEmoji(caption) + " " + stringResource(captionStringRes(caption)),
            modifier = Modifier
                .padding(CAPTION_PADDING)
                .graphicsLayer { alpha = state.captionTextAlpha.value },
            color = LocalPaperPalette.current.ink,
            style = PaperType.prose
        )
    }
}

@Composable
private fun ProgressSlip(filledDots: Int, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .paperSlip()
            .padding(start = SLIP_PADDING, top = 8.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(PROGRESS_DOT_COUNT) { index ->
            ProgressDot(
                filled = index < filledDots,
                leadingGap = if (index == 0) 0.dp else PROGRESS_DOT_GAP
            )
        }
        InkIconButton(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.cancel),
            onClick = onSkip,
            modifier = Modifier.padding(start = 8.dp),
            tint = LocalPaperPalette.current.inkSoft
        )
    }
}

@Composable
private fun ProgressDot(filled: Boolean, leadingGap: Dp) {
    val palette = LocalPaperPalette.current
    Box(
        Modifier
            .padding(start = leadingGap)
            .size(PROGRESS_DOT_SIZE)
            .background(if (filled) palette.inkBlue else palette.rule, CircleShape)
            .clearAndSetSemantics {}
    )
}

/**
 * The overlay drives the app itself, so nothing underneath may be touched while it
 * is up — but its own skip button must still answer, which is why the gesture is
 * taken on the main pass rather than ahead of the children.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.swallowTouches() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Main).changes.forEach {
                if (!it.isConsumed) it.consume()
            }
        }
    }
}

internal fun captionEmoji(caption: TutorialCaption): String = when (caption) {
    TutorialCaption.TARGET_DATE -> "📅"
    TutorialCaption.DUE_DATE -> "⏰"
}

internal fun captionStringRes(caption: TutorialCaption): Int = when (caption) {
    TutorialCaption.TARGET_DATE -> R.string.date_kind_target_caption
    TutorialCaption.DUE_DATE -> R.string.date_kind_due_caption
}

internal fun bannerTextFor(content: TutorialBannerContent): String =
    "🔔 ${content.listName}${bannerDateSuffix(content.dueDate)}"

private fun bannerDateSuffix(dueDate: LocalDate?): String {
    if (dueDate == null) return ""
    val locale = Locale.getDefault(Locale.Category.FORMAT)
    val pattern = DateFormat.getBestDateTimePattern(locale, "dM")
    return " ⏰ ${dueDate.format(DateTimeFormatter.ofPattern(pattern, locale))}"
}
