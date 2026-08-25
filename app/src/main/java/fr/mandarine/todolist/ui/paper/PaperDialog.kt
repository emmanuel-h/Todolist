package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

private const val SETTLED = 1f
private const val LIFTED = 0f
private const val NO_DIM = 0f
private const val STRAIGHT_DOWN = 0f
private const val AMBIENT_ALPHA = 0.18f
private const val CONTACT_ALPHA = 0.25f
private const val SETTLE_LABEL = "sheetSettle"

private val SHEET_WIDTH = 360.dp
private val SHEET_MARGIN = 24.dp
private val SHEET_RISE = 8.dp
private val AMBIENT_RADIUS = 18.dp
private val AMBIENT_DROP = 8.dp
private val CONTACT_RADIUS = 2.dp
private val CONTACT_DROP = 1.dp
private val WRITING_MARGIN = 20.dp

/**
 * A smaller sheet laid on the page rather than a card floating above it: square
 * corners, the page's own grain, a warm ambient shadow with a tight contact
 * shadow under it, and no outline. The platform's grey dim is turned off and the
 * sheet asks the page for its warm veil instead, so the screen stays one material
 * and a sheet laid on a sheet costs the page nothing further. The sheet settles
 * onto the page instead of popping.
 */
@Composable
fun PaperDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalPaperPalette.current
    val settling = remember { MutableTransitionState(false).apply { targetState = true } }
    val leave = remember { { settling.targetState = false } }
    val dismiss = rememberUpdatedState(onDismissRequest)

    LaunchedEffect(settling.isIdle, settling.targetState) {
        if (settling.isIdle && !settling.targetState) dismiss.value()
    }

    VeilWhileLaid(LocalPaperVeil.current, settling.targetState)

    Dialog(
        onDismissRequest = leave,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        UndimmedWindow()
        val transition = rememberTransition(settling, label = SETTLE_LABEL)
        val settle = transition.animateFloat(
            transitionSpec = {
                if (targetState) PaperMotion.rowEnter else PaperMotion.rowExit
            },
            label = SETTLE_LABEL
        ) { shown -> if (shown) SETTLED else LIFTED }
        val shadowInk = palette.shadow

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = leave
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(SHEET_MARGIN),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .widthIn(max = SHEET_WIDTH)
                        .graphicsLayer {
                            alpha = settle.value
                            translationY = SHEET_RISE.toPx() * (SETTLED - settle.value)
                        }
                        .dropShadow(RectangleShape) {
                            radius = AMBIENT_RADIUS.toPx()
                            offset = Offset(STRAIGHT_DOWN, AMBIENT_DROP.toPx())
                            color = shadowInk
                            alpha = AMBIENT_ALPHA
                        }
                        .dropShadow(RectangleShape) {
                            radius = CONTACT_RADIUS.toPx()
                            offset = Offset(STRAIGHT_DOWN, CONTACT_DROP.toPx())
                            color = shadowInk
                            alpha = CONTACT_ALPHA
                        }
                        .paperSheet(tone = palette.paperShade, lit = palette.paperSheet)
                        .pointerInput(Unit) { detectTapGestures { } }
                        .padding(WRITING_MARGIN),
                    content = content
                )
            }
        }
    }
}

/**
 * The platform dims the page behind a dialog with a flat grey, which is the one
 * colour the paper never uses. The window's own dim is turned off so the veil can
 * be drawn in ink-brown instead, and it has to be reached through the dialog's
 * window — a cast that only holds inside dialog content, never in a preview.
 */
@Composable
private fun UndimmedWindow() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    SideEffect { window.setDimAmount(NO_DIM) }
}
