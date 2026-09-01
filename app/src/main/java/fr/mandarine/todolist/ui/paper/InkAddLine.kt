package fr.mandarine.todolist.ui.paper

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import fr.mandarine.todolist.R
import kotlinx.coroutines.delay

private const val HINT_INK = 1f

/**
 * The breath moves the hint without moving it out of reach: the low end used to
 * put pencil over paper at 1.6:1, and the rule spends most of its cycle down
 * there. It now breathes across the top of the range, where the difference still
 * reads as movement and the hint stays legible the whole way.
 */
private const val HINT_BREATH_IN = 0.85f
private const val HINT_BREATH_OUT = 1f
private const val BREATH_LABEL = "hintBreath"
private const val BREATH_ALPHA_LABEL = "hintAlpha"
private const val PEN_SETTLE_MILLIS = 250L

/**
 * The one line every new row is written on: a bare rule carrying the hint, a blue
 * caret once the pen is on it, and — from the moment there is something to commit
 * — a tick at the end of the rule.
 *
 * The tick and the keyboard's own Done are the same act, and both leave a fresh
 * caret waiting. The line used to have only Done, which is a key a reader has to
 * know is load-bearing before they will press it; nothing on the page said the
 * line could be finished at all, only that it could be abandoned. Back, a tap on
 * the paper or dismissing the keyboard still put the pen down.
 */
@Composable
fun InkAddLine(
    text: String,
    onTextChange: (String) -> Unit,
    onCommit: (String) -> Unit,
    armed: Boolean,
    onPenUp: () -> Unit,
    onPenDown: () -> Unit,
    spoken: String,
    commitSpoken: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    breathing: Boolean = false,
    animated: Boolean = true
) {
    val palette = LocalPaperPalette.current
    val haptics = rememberPaperHaptics()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var penOnPaper by remember { mutableStateOf(false) }

    ArmedFocus(armed, penOnPaper, focusRequester, focusManager)
    PenDownOnKeyboardLeaving(penOnPaper, focusManager)

    BackHandler(enabled = penOnPaper) { focusManager.clearFocus() }

    val hintAlpha = breathingAlpha(breathing && !penOnPaper && animated)

    val commit: () -> Unit = {
        if (text.isNotBlank()) {
            onCommit(text)
            haptics.submit()
        }
        focusRequester.requestFocus()
    }

    RuledRow(modifier = modifier, onClick = { focusRequester.requestFocus() }) {
        OnRuleSlot(modifier = Modifier.weight(1f), alignment = Alignment.TopStart) {
            GhostHint(shown = text.isEmpty(), style = style, ink = hintAlpha)
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .seatOnRule()
                    .semantics { contentDescription = spoken }
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            penOnPaper = true
                            onPenUp()
                        } else if (penOnPaper) {
                            penOnPaper = false
                            onPenDown()
                        }
                    },
                textStyle = style.trimmedToGlyphs().copy(color = palette.inked(InkTone.Words)),
                singleLine = true,
                cursorBrush = SolidColor(palette.inked(InkTone.Acted)),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { commit() })
            )
        }
        CommitMark(
            shown = text.isNotBlank(),
            spoken = commitSpoken,
            onCommit = commit,
            animated = animated
        )
    }
}

/**
 * The tick that finishes the line. It is not on the rule until there is something
 * to finish — an always-present tick over an empty line is a control that does
 * nothing, and the reader learns to stop looking at it. It arrives by widening the
 * rule rather than by fading in over it, so the writing slot gives up the room
 * instead of having the glyph land on top of what is being written.
 */
@Composable
private fun RowScope.CommitMark(
    shown: Boolean,
    spoken: String,
    onCommit: () -> Unit,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = shown,
        enter = if (animated) {
            fadeIn(PaperMotion.rowEnter) + expandHorizontally(PaperMotion.rowUnfold)
        } else {
            fadeIn(PaperMotion.rowEnter)
        },
        exit = if (animated) {
            fadeOut(PaperMotion.rowExit) + shrinkHorizontally(PaperMotion.rowFold)
        } else {
            fadeOut(PaperMotion.rowExit)
        }
    ) {
        InkIconButton(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = spoken,
            onClick = onCommit,
            modifier = modifier,
            tint = LocalPaperPalette.current.inked(InkTone.Acted),
            seat = IconSeat.OnRule,
            foot = GlyphFoot.check
        )
    }
}

/**
 * The ghost gives way to what is written over it rather than being replaced
 * between two frames: it is the same rule, and only the ink on it changes.
 */
@Composable
private fun GhostHint(shown: Boolean, style: TextStyle, ink: () -> Float) {
    val palette = LocalPaperPalette.current
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(PaperMotion.rowEnter),
        exit = fadeOut(PaperMotion.rowExit)
    ) {
        Text(
            text = stringResource(R.string.add_line_hint),
            modifier = Modifier
                .seatOnRule()
                .graphicsLayer { alpha = ink() },
            style = style,
            color = palette.inked(InkTone.Margin)
        )
    }
}

@Composable
private fun ArmedFocus(
    armed: Boolean,
    penOnPaper: Boolean,
    focusRequester: FocusRequester,
    focusManager: FocusManager
) {
    val holding = rememberUpdatedState(penOnPaper)
    LaunchedEffect(armed) {
        if (armed) focusRequester.requestFocus() else if (holding.value) focusManager.clearFocus()
    }
}

/**
 * The keyboard swallows both back and the swipe-down that dismisses it, so the
 * line cannot learn from a back press that the pen should go down — it learns from
 * the keyboard leaving. A keyboard that was never up cannot leave, and a keyboard
 * that goes away because another window took the screen has not been dismissed, so
 * neither of those puts the pen down.
 */
@Composable
private fun PenDownOnKeyboardLeaving(
    penOnPaper: Boolean,
    focusManager: FocusManager
) {
    val keyboardUp = keyboardVisible()
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val sheetLaid = LocalPaperVeil.current.laid
    var keyboardSeen by remember { mutableStateOf(false) }

    LaunchedEffect(penOnPaper, keyboardUp, windowFocused, sheetLaid) {
        when {
            !penOnPaper -> keyboardSeen = false
            keyboardUp -> keyboardSeen = true
            keyboardSeen && windowFocused && !sheetLaid -> {
                delay(PEN_SETTLE_MILLIS)
                focusManager.clearFocus()
            }
        }
    }
}

/**
 * An empty page invites rather than instructs: the hint breathes on the first rule
 * instead of naming itself. It is the only thing on either screen that moves on
 * its own, and it stops when the reader has asked for stillness.
 */
@Composable
private fun breathingAlpha(active: Boolean): () -> Float {
    if (!active) return { HINT_INK }
    val breath = rememberInfiniteTransition(label = BREATH_LABEL)
    val alpha = breath.animateFloat(
        initialValue = HINT_BREATH_IN,
        targetValue = HINT_BREATH_OUT,
        animationSpec = PaperMotion.breath,
        label = BREATH_ALPHA_LABEL
    )
    return { alpha.value }
}
