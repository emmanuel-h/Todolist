package fr.mandarine.todolist.ui.tutorial

import androidx.activity.compose.BackHandler
import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialCaption
import fr.mandarine.todolist.presentation.TutorialLine
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.byLamplight
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperSlipCaption
import fr.mandarine.todolist.ui.paper.ReminderNote
import fr.mandarine.todolist.ui.paper.reminderSlipText
import fr.mandarine.todolist.ui.paper.paperSlip
import fr.mandarine.todolist.ui.paper.PaperMotion
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.paperSheet
import fr.mandarine.todolist.ui.paper.raised
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
private val CAPTION_GLYPH_GAP = 8.dp
private val CAPTION_GAP = 12.dp
private val BANNER_MARGIN = 12.dp
private val BANNER_GAP = 32.dp
private val PROGRESS_BOTTOM_MARGIN = 12.dp
private val PROGRESS_DOT_SIZE = 8.dp
private val PROGRESS_DOT_GAP = 6.dp

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
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    /**
     * Registered here rather than on the window, and above the early return so it
     * covers the opening beat too.
     *
     * setContent composes after onCreate has returned, so a callback added to the
     * dispatcher during onCreate is registered before the navigation display's
     * own and before the add line's — and back is dispatched newest first. The
     * tutorial's callback therefore never won: back peeled the page instead of
     * cancelling the tour, and the demo stalled with the hand still up. Composed
     * after the page stack, it outranks it.
     */
    BackHandler(enabled = state.running, onBack = onSkip)

    if (!state.visible) return

    val root = LocalView.current
    val density = LocalDensity.current
    SideEffect {
        state.handSizePx = with(density) { HAND_SIZE.toPx() }
        state.captionGapPx = with(density) { CAPTION_GAP.toPx() }
    }
    val statusBarPx = WindowInsets.statusBars.getTop(density).toFloat()
    val arrival = remember { Animatable(OFF_THE_PAGE) }
    LaunchedEffect(Unit) { arrival.animateTo(ON_THE_PAGE, PaperMotion.rowEnter) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { state.setOrigin(it.screenBounds(root)) }
            .graphicsLayer { alpha = state.overlayAlpha.value * arrival.value }
            .pointerInput(Unit) { swallowTouches() }
    ) {
        state.narration?.let { NarrationSlip(it, state) }
        state.banner?.let { BannerSlip(it, state, statusBarPx) }
        PhantomHand(state, anchors)
        state.caption?.let { CaptionSlip(it, state) }
        ProgressSlip(
            filledDots = state.filledDots,
            onNext = onNext,
            onSkip = onSkip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
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
    val disc = if (palette.byLamplight) palette.lift else palette.paperSheet
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
            .raised(CircleShape, palette) {
                dropShadow(CircleShape) {
                    val press = handPress(state.handScale.value)
                    radius = lerp(HAND_SHADOW_LIFTED, HAND_SHADOW_TOUCHING, press).toPx()
                    offset = Offset(
                        OFF_THE_PAGE,
                        lerp(HAND_DROP_LIFTED, HAND_DROP_TOUCHING, press).toPx()
                    )
                    alpha = HAND_SHADOW_ALPHA
                    color = shadowInk
                }
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

/**
 * What the tour is doing, in a sentence, pinned where it cannot move: a line that
 * followed the thing it described would have the reader's eye chasing it around
 * the page instead of watching the hand.
 *
 * The mock reminder takes the slip away rather than stacking with it — the two want
 * the same strip of paper, and the banner is itself the point of the scene whose
 * line it replaces.
 */
@Composable
private fun BoxScope.NarrationSlip(line: TutorialLine, state: TutorialOverlayState) {
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(BANNER_MARGIN)
            .graphicsLayer { alpha = state.narrationAlpha.value }
            .paperSlip()
    ) {
        Text(
            text = stringResource(narrationStringRes(line)),
            modifier = Modifier.padding(horizontal = SLIP_PADDING, vertical = CAPTION_PADDING),
            color = LocalPaperPalette.current.inked(InkTone.Words),
            style = PaperType.prose,
            textAlign = TextAlign.Center
        )
    }
}

internal fun narrationStringRes(line: TutorialLine): Int = when (line) {
    TutorialLine.WRITE_A_LIST -> R.string.tutorial_write_a_list
    TutorialLine.A_DAY_AND_A_NOTE -> R.string.tutorial_a_day_and_a_note
    TutorialLine.OPEN_IT -> R.string.tutorial_open_it
    TutorialLine.WRITE_ITEMS -> R.string.tutorial_write_items
    TutorialLine.TICK_AND_MOVE -> R.string.tutorial_tick_and_move
    TutorialLine.EDIT_AND_TEAR -> R.string.tutorial_edit_and_tear
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
            color = LocalPaperPalette.current.inked(InkTone.Words),
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
    ) {
        PaperSlipCaption(
            painter = painterResource(captionGlyph(caption)),
            text = stringResource(captionStringRes(caption)),
            modifier = Modifier.graphicsLayer { alpha = state.captionTextAlpha.value }
        )
    }
}

/**
 * Where the demonstration has got to, and the two ways out of the beat it is on:
 * done with this one, and done with all of them.
 */
@Composable
private fun ProgressSlip(
    filledDots: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = stringResource(R.string.tutorial_next),
            onClick = onNext,
            modifier = Modifier.padding(start = 8.dp),
            tint = LocalPaperPalette.current.inkSoft
        )
        InkIconButton(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.cancel),
            onClick = onSkip,
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
            .background(if (filled) palette.inked(InkTone.Acted) else palette.rule, CircleShape)
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

internal fun captionGlyph(caption: TutorialCaption): Int = when (caption) {
    TutorialCaption.TARGET_DATE -> R.drawable.ic_event
    TutorialCaption.DUE_DATE -> R.drawable.ic_alarm
}

internal fun captionStringRes(caption: TutorialCaption): Int = when (caption) {
    TutorialCaption.TARGET_DATE -> R.string.date_kind_target_caption
    TutorialCaption.DUE_DATE -> R.string.date_kind_due_caption
}

/**
 * The slip the tour ends on says what a reminder will look like, so it has to be
 * written by whatever writes the real ones. It was its own sentence here until the
 * page started raising real slips of its own.
 */
internal fun bannerTextFor(content: TutorialBannerContent): String =
    reminderSlipText(
        ReminderNote(content.listName, content.dueDate),
        Locale.getDefault(Locale.Category.FORMAT)
    )
