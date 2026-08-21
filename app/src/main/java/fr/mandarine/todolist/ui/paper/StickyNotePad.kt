package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import fr.mandarine.todolist.R
import kotlinx.coroutines.launch

private const val BACK_SHEET_ROTATION = 4f
private const val MID_SHEET_ROTATION = 2f

@Composable
private fun StickyNoteSheetSurface(
    color: Color,
    rotationDegrees: Float,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(PaperDimens.stickyCorner)
    Box(
        modifier = modifier
            .size(PaperDimens.stickySheet)
            .graphicsLayer { rotationZ = rotationDegrees }
            .background(color, shape)
            .border(PaperDimens.rule, PaperInk.stickyNoteEdge, shape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun StickyNotePad(
    onTake: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    taken: Boolean = false,
    reducedMotion: Boolean = false,
    painter: Painter = painterResource(R.drawable.ic_add)
) {
    val scope = rememberCoroutineScope()
    val peel = remember { Animatable(STICKY_PEEL_REST) }
    val settle = remember { Animatable(STICKY_SETTLE_DONE) }
    var peeling by remember { mutableStateOf(false) }
    val previouslyTaken = remember { mutableStateOf(taken) }

    LaunchedEffect(taken) {
        val returning = previouslyTaken.value && !taken && !reducedMotion
        previouslyTaken.value = taken
        if (returning) {
            settle.snapTo(STICKY_SETTLE_START)
            settle.animateTo(STICKY_SETTLE_DONE, PaperMotion.sheetSettle)
        }
    }

    Box(
        modifier = modifier.size(PaperDimens.stickyPad),
        contentAlignment = Alignment.Center
    ) {
        StickyNoteSheetSurface(PaperInk.stickyNoteBack, BACK_SHEET_ROTATION, Modifier) {}
        StickyNoteSheetSurface(PaperInk.stickyNoteMid, MID_SHEET_ROTATION, Modifier) {}

        if (!taken) {
            val resting = stickyNoteSettleAt(settle.value)
            StickyNoteSheetSurface(
                color = PaperInk.stickyNote,
                rotationDegrees = resting.rotationDegrees,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = resting.scale
                        scaleY = resting.scale
                        alpha = resting.alpha
                    }
                    .clickable {
                        onTake()
                        if (!reducedMotion) {
                            peeling = true
                            scope.launch {
                                peel.snapTo(STICKY_PEEL_REST)
                                peel.animateTo(STICKY_PEEL_LIFTED, PaperMotion.sheetLift)
                                peel.animateTo(STICKY_PEEL_GONE, PaperMotion.sheetSettle)
                                peeling = false
                            }
                        }
                    }
            ) {
                InkIcon(
                    painter = painter,
                    contentDescription = contentDescription,
                    tint = PaperInk.ink
                )
            }
        }

        if (peeling) {
            val flying = stickyNotePeelAt(peel.value)
            StickyNoteSheetSurface(
                color = PaperInk.stickyNote,
                rotationDegrees = flying.rotationDegrees,
                modifier = Modifier.graphicsLayer {
                    scaleX = flying.scale
                    scaleY = flying.scale
                    alpha = flying.alpha
                    translationX = -PaperDimens.stickyPeelTravelX.toPx() * flying.travelFraction
                    translationY = -PaperDimens.stickyPeelTravelY.toPx() * flying.travelFraction
                }
            ) {}
        }
    }
}
