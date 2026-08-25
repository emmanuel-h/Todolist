package fr.mandarine.todolist.ui.paper

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import fr.mandarine.todolist.R
import kotlinx.coroutines.delay

private const val HINT_INK = 1f
private const val HINT_BREATH_IN = 0.35f
private const val HINT_BREATH_OUT = 0.55f
private const val BREATH_LABEL = "hintBreath"
private const val BREATH_ALPHA_LABEL = "hintAlpha"
private const val PEN_SETTLE_MILLIS = 250L

/**
 * The one line every new row is written on: a bare rule carrying nothing but the
 * hint and, once the pen is on it, a blue caret. There is no submit glyph and no
 * way to cancel — the keyboard's own Done commits the line and leaves a fresh
 * caret waiting, and back, a tap on the paper or dismissing the keyboard puts the
 * pen down.
 */
@Composable
fun InkAddLine(
    text: String,
    onTextChange: (String) -> Unit,
    onCommit: (String) -> Unit,
    armed: Boolean,
    onPenUp: () -> Unit,
    onPenDown: () -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
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

    RuledRow(modifier = modifier, onClick = { focusRequester.requestFocus() }) {
        OnRuleSlot(modifier = Modifier.weight(1f), alignment = Alignment.TopStart) {
            GhostHint(shown = text.isEmpty(), style = style, ink = hintAlpha)
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = fieldModifier
                    .fillMaxWidth()
                    .seatOnRule()
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
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text.isNotBlank()) {
                            onCommit(text)
                            haptics.submit()
                        }
                        focusRequester.requestFocus()
                    }
                )
            )
        }
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
