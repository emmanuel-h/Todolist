package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import kotlinx.coroutines.delay

private const val BACK_SHEET_ROTATION = -0.7f
private const val MID_SHEET_ROTATION = 0.6f
private const val FOLD_DEGREES = -75f
private const val BECKON_DEGREES = -12f
private const val PERSPECTIVE = 12f
private const val AMBIENT_ALPHA = 0.16f
private const val CONTACT_ALPHA = 0.22f
private const val GLUE_ALPHA = 0.55f
private const val DENT_PRESSED = 1f
private const val NO_DENT = 0f
private const val STRAIGHT_DOWN = 0f
private const val DENT_LABEL = "padDent"
private const val BECKON_DELAY_MILLIS = 600L

private val SHEET_SHAPE = RoundedCornerShape(PaperDimens.stickyCorner)
private val GLUED_EDGE = TransformOrigin(0.5f, 0f)
private val AMBIENT_RADIUS = 10.dp
private val AMBIENT_SIDEWAYS = 1.dp
private val AMBIENT_DROP = 4.dp
private val CONTACT_RADIUS = 1.dp
private val CONTACT_DROP = 1.dp
private val GLUE_RADIUS = 2.dp
private val GLUE_DROP = 1.dp
private val DENT_RADIUS = 2.dp
private val DENT_DROP = 1.dp
private val SHEET_STEP = 1.dp

/**
 * One shadow lies under the whole stack, so the pad reads as a single block of
 * paper rather than as three outlined cards: the sheet at the bottom carries a
 * wide warm ambient blur and a tight contact blur, and the sheets above it are
 * offset by a hair. Every sheet takes the page's own grain in its own colour and
 * a darker band along the glued top edge.
 */
@Composable
private fun StickyNoteSheetSurface(
    color: Color,
    modifier: Modifier,
    shadowed: Boolean = false,
    dent: () -> Float = { NO_DENT },
    content: @Composable BoxScope.() -> Unit = {}
) {
    val palette = LocalPaperPalette.current
    val glueInk = palette.stickyNoteBack
    val sunkenInk = palette.paperSunken
    Box(
        modifier = Modifier
            .size(PaperDimens.stickySheet)
            .then(modifier)
            .then(if (shadowed) Modifier.padShadow(palette) else Modifier)
            .clip(SHEET_SHAPE)
            .paperSheet(tone = color, lit = color)
            .innerShadow(SHEET_SHAPE) {
                radius = GLUE_RADIUS.toPx()
                offset = Offset(STRAIGHT_DOWN, GLUE_DROP.toPx())
                this.color = glueInk
                alpha = GLUE_ALPHA
            }
            .innerShadow(SHEET_SHAPE) {
                radius = DENT_RADIUS.toPx()
                offset = Offset(STRAIGHT_DOWN, DENT_DROP.toPx())
                this.color = sunkenInk
                alpha = dent()
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

private fun Modifier.padShadow(palette: PaperPalette): Modifier = raised(SHEET_SHAPE, palette) {
    dropShadow(SHEET_SHAPE) {
        radius = AMBIENT_RADIUS.toPx()
        offset = Offset(AMBIENT_SIDEWAYS.toPx(), AMBIENT_DROP.toPx())
        color = palette.shadow
        alpha = AMBIENT_ALPHA
    }.dropShadow(SHEET_SHAPE) {
        radius = CONTACT_RADIUS.toPx()
        offset = Offset(STRAIGHT_DOWN, CONTACT_DROP.toPx())
        color = palette.shadow
        alpha = CONTACT_ALPHA
    }
}

@Composable
fun StickyNotePad(
    onTake: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    taken: Boolean = false,
    reducedMotion: Boolean = false,
    beckons: Boolean = false,
    painter: Painter = painterResource(R.drawable.ic_add)
) {
    val peel = remember { Animatable(STICKY_PEEL_REST) }
    val settle = remember { Animatable(STICKY_SETTLE_DONE) }
    val beckon = remember { Animatable(STICKY_FLAT) }
    val haptics = rememberPaperHaptics()
    var peeling by remember { mutableStateOf(false) }
    val previouslyTaken = remember { mutableStateOf(taken) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dent = animateFloatAsState(
        targetValue = if (pressed) DENT_PRESSED else NO_DENT,
        animationSpec = PaperMotion.rowEnter,
        label = DENT_LABEL
    )

    LaunchedEffect(taken) {
        val was = previouslyTaken.value
        previouslyTaken.value = taken
        if (was == taken) return@LaunchedEffect
        if (taken) {
            haptics.pickUp()
            if (!reducedMotion) {
                peeling = true
                peel.snapTo(STICKY_PEEL_REST)
                peel.animateTo(STICKY_PEEL_LIFTED, PaperMotion.pickUp)
                peel.animateTo(STICKY_PEEL_GONE, PaperMotion.sheetSettle)
                peeling = false
            }
            haptics.land()
        } else if (!reducedMotion) {
            settle.snapTo(STICKY_SETTLE_START)
            settle.animateTo(STICKY_SETTLE_DONE, PaperMotion.sheetSettle)
        }
    }

    /**
     * A page with nothing written on it says so by lifting the top sheet once, a
     * beat after the page arrives, and laying it back down.
     */
    LaunchedEffect(beckons, reducedMotion) {
        if (!beckons || reducedMotion || taken) return@LaunchedEffect
        delay(BECKON_DELAY_MILLIS)
        beckon.animateTo(STICKY_FOLDED, PaperMotion.pickUp)
        beckon.animateTo(STICKY_FLAT, PaperMotion.sheetSettle)
    }

    val palette = LocalPaperPalette.current
    val flying = peeling || settle.isRunning || beckon.isRunning
    Box(
        modifier = modifier
            .size(PaperDimens.stickyPad)
            .preferredFrameRate(
                if (flying) FrameRateCategory.High else FrameRateCategory.Default
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!taken) {
            StickyNoteSheetSurface(
                color = palette.stickyNoteBack,
                modifier = Modifier.graphicsLayer {
                    rotationZ = BACK_SHEET_ROTATION
                    translationX = SHEET_STEP.toPx()
                    translationY = SHEET_STEP.toPx()
                },
                shadowed = true
            )
            StickyNoteSheetSurface(
                color = palette.stickyNoteMid,
                modifier = Modifier.graphicsLayer {
                    rotationZ = MID_SHEET_ROTATION
                    translationX = -SHEET_STEP.toPx()
                }
            )
            StickyNoteSheetSurface(
                color = palette.stickyNote,
                modifier = Modifier
                    .graphicsLayer {
                        val resting = stickyNoteSettleAt(settle.value)
                        transformOrigin = GLUED_EDGE
                        cameraDistance = PERSPECTIVE * density
                        rotationX = BECKON_DEGREES * beckon.value
                        rotationZ = resting.rotationDegrees
                        scaleX = resting.scale
                        scaleY = resting.scale
                        alpha = resting.alpha
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = PaperFocusMark,
                        role = Role.Button,
                        onClick = onTake
                    ),
                dent = { dent.value }
            ) {
                InkIcon(
                    painter = painter,
                    contentDescription = contentDescription,
                    tint = palette.stickyNoteInk
                )
            }
        }

        if (peeling) {
            StickyNoteSheetSurface(
                color = palette.stickyNote,
                modifier = Modifier.graphicsLayer {
                    val flying = stickyNotePeelAt(peel.value)
                    transformOrigin = GLUED_EDGE
                    cameraDistance = PERSPECTIVE * density
                    rotationX = FOLD_DEGREES * flying.foldFraction
                    rotationZ = flying.rotationDegrees
                    scaleX = flying.scale
                    scaleY = flying.scale
                    alpha = flying.alpha
                    translationX = -PaperDimens.stickyPeelTravelX.toPx() * flying.travelFraction
                    translationY = -PaperDimens.stickyPeelTravelY.toPx() * flying.travelFraction
                }
            )
        }
    }
}
