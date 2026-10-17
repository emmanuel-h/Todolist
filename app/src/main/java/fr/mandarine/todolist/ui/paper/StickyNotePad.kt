package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import kotlinx.coroutines.delay

private const val BACK_SHEET_ROTATION = 6f
private const val MID_SHEET_ROTATION = -3f
private const val TOP_SHEET_ROTATION = -3f
private const val FOLD_DEGREES = -75f
private const val BECKON_DEGREES = -12f
private const val PERSPECTIVE = 12f
private const val AMBIENT_ALPHA = 0.26f
private const val CONTACT_ALPHA = 0.3f
private const val HALO_ALPHA = 0.32f
private const val RING_REACH = 0.58f
private const val RING_START_DEGREES = -55f
private const val RING_SWEEP_DEGREES = 340f
private const val RING_TAIL_DEGREES = 40f
private const val RING_TAIL_REACH = 1.07f
private const val PEN_TILT_DEGREES = -35f
private const val PEN_SHADOW_ALPHA = 0.18f
private const val PEN_SHADE_ALPHA = 0.55f
private const val PEN_SHINE_ALPHA = 0.45f
private const val PEN_LIFTED = 1f
private const val PEN_RESTING = 0f
private const val PEN_LABEL = "penLift"
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
private val HALO_RADIUS = 12.dp
private val DENT_RADIUS = 2.dp
private val DENT_DROP = 1.dp
private val SHEET_STEP = 1.dp
private val SHEET_INSET = (PaperDimens.stickyPad - PaperDimens.stickySheet) / 2
private val CARD_HEAD = 14.dp
private val CARD_RULE_STEP = 11.dp
private val CARD_RULE_WIDTH = 1.dp
private val GLYPH_INSET = PaddingValues(start = 8.dp, top = 7.dp)
private val PEN_NIB_AT = DpOffset(45.dp, 42.dp)
private val PEN_LENGTH = 42.dp
private val RING_STROKE = 1.6.dp
private val PEN_BARREL = 7.dp
private val PEN_NIB = 9.dp
private val PEN_NIB_WIDTH = 5.dp
private val PEN_GRIP = 8.dp
private val PEN_GRIP_HEIGHT = 6.dp
private val PEN_BAND_FROM_END = 26.dp
private val PEN_BAND = 2.dp
private val PEN_CLIP_FROM_END = 22.dp
private val PEN_CLIP_STROKE = 1.6.dp
private val PEN_SHADOW_DROP = DpOffset(2.dp, 4.dp)
private val PEN_LIFT_RISE = 10.dp

/**
 * Blank index cards, cut from the same stock as the lists on the board, so the pad
 * reads as "one more of these". The card at the bottom carries the shadow for the
 * stack; every card takes the page's grain and the ruled lines a list is written
 * on, under a plain head where its name will go.
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
    val sunkenInk = palette.paperSunken
    val ruleInk = palette.rule
    Box(
        modifier = Modifier
            .size(PaperDimens.stickySheet)
            .then(modifier)
            .then(if (shadowed) Modifier.padShadow(palette) else Modifier)
            .clip(SHEET_SHAPE)
            .paperSheet(tone = color, lit = color)
            .drawBehind { drawCardRules(ruleInk) }
            .innerShadow(SHEET_SHAPE) {
                radius = DENT_RADIUS.toPx()
                offset = Offset(STRAIGHT_DOWN, DENT_DROP.toPx())
                this.color = sunkenInk
                alpha = dent()
            },
        contentAlignment = Alignment.TopStart,
        content = content
    )
}

/**
 * The circle a reader draws round something on a page so it is not missed: one
 * stroke of red ink that starts at the top right, goes all the way round and runs
 * a little past where it began, drifting just outside itself rather than closing.
 * It is centred on the glyph, so the cross sits in the middle of it.
 */
private fun DrawScope.drawInkedRing(ink: Color) {
    val reach = size.minDimension * RING_REACH
    val ring = Path().apply {
        arcTo(
            rect = Rect(center, reach),
            startAngleDegrees = RING_START_DEGREES,
            sweepAngleDegrees = RING_SWEEP_DEGREES,
            forceMoveTo = true
        )
        val tail = Rect(center, reach * RING_TAIL_REACH)
        arcTo(
            rect = tail,
            startAngleDegrees = RING_START_DEGREES + RING_SWEEP_DEGREES,
            sweepAngleDegrees = RING_TAIL_DEGREES,
            forceMoveTo = false
        )
    }
    drawPath(ring, ink, style = Stroke(width = RING_STROKE.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawCardRules(ink: Color) {
    val step = CARD_RULE_STEP.toPx()
    val width = CARD_RULE_WIDTH.toPx()
    var y = CARD_HEAD.toPx() + step
    while (y < size.height) {
        drawLine(ink, Offset(0f, y), Offset(size.width, y), width)
        y += step
    }
}

/**
 * A fountain pen put down across the cards, nib towards the board: the pad is
 * something to write on, and the pen says so before anything is tapped. It is drawn
 * along a level line from the nib and turned as a whole, so every part of it stays
 * a plain rectangle.
 */
private fun DrawScope.drawPen(palette: PaperPalette) {
    val nib = Offset(PEN_NIB_AT.x.toPx(), PEN_NIB_AT.y.toPx())
    val length = PEN_LENGTH.toPx()
    val barrel = PEN_BARREL.toPx()
    val half = barrel / 2
    val nibEnd = PEN_NIB.toPx()
    val gripEnd = nibEnd + PEN_GRIP.toPx()
    rotate(PEN_TILT_DEGREES, pivot = nib) {
        translate(nib.x, nib.y) {
            translate(PEN_SHADOW_DROP.x.toPx(), PEN_SHADOW_DROP.y.toPx()) {
                drawRoundRect(
                    color = palette.shadow,
                    topLeft = Offset(nibEnd, -half),
                    size = Size(length - nibEnd, barrel),
                    cornerRadius = CornerRadius(half),
                    alpha = PEN_SHADOW_ALPHA
                )
            }
            val tip = Path().apply {
                moveTo(0f, 0f)
                lineTo(nibEnd, -PEN_NIB_WIDTH.toPx() / 2)
                lineTo(nibEnd, PEN_NIB_WIDTH.toPx() / 2)
                close()
            }
            drawPath(tip, palette.pencil)
            val grip = PEN_GRIP_HEIGHT.toPx()
            drawRect(palette.inkAmber, Offset(nibEnd, -grip / 2), Size(gripEnd - nibEnd, grip))
            drawRoundRect(
                color = palette.inkBlue,
                topLeft = Offset(gripEnd, -half),
                size = Size(length - gripEnd, barrel),
                cornerRadius = CornerRadius(half)
            )
            drawRoundRect(
                color = palette.inkBlueDeep,
                topLeft = Offset(gripEnd, 0f),
                size = Size(length - gripEnd, half),
                cornerRadius = CornerRadius(half),
                alpha = PEN_SHADE_ALPHA
            )
            drawLine(
                color = palette.inkBluePale,
                start = Offset(gripEnd + half, -half / 2),
                end = Offset(length - half, -half / 2),
                strokeWidth = CARD_RULE_WIDTH.toPx(),
                alpha = PEN_SHINE_ALPHA
            )
            val band = length - PEN_BAND_FROM_END.toPx()
            drawRect(palette.inkAmber, Offset(band, -half), Size(PEN_BAND.toPx(), barrel))
            drawLine(
                color = palette.inkAmber,
                start = Offset(length - PEN_CLIP_FROM_END.toPx(), -half),
                end = Offset(length - half, -half),
                strokeWidth = PEN_CLIP_STROKE.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Cream cards over cream cards: the stack would sink into a list it overlaps if
 * its shadow only fell below it, so each card also carries an even halo on every
 * side that marks its edge against whatever is under it.
 */
private fun Modifier.padShadow(palette: PaperPalette): Modifier = raised(SHEET_SHAPE, palette) {
    dropShadow(SHEET_SHAPE) {
        radius = HALO_RADIUS.toPx()
        color = palette.shadow
        alpha = HALO_ALPHA
    }.dropShadow(SHEET_SHAPE) {
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

/**
 * What the sheet under the taken one says and does. Taking a sheet off the pad
 * starts something, and the sheet now showing is how that is called off again — so
 * the pad goes on answering to a press for as long as the sheet it handed over is
 * still out.
 */
@Immutable
class StickyNotePutBack(
    val painter: Painter,
    val contentDescription: String,
    val onPress: () -> Unit
)

/**
 * A pad, not a single sheet. Taking one off it lifts the top sheet and carries it
 * away; the sheets under it stay in the corner, because a pad that vanishes when a
 * sheet is taken from it is not a pad. What is left showing answers to [putBack].
 *
 * The lift is quick and the carry is not: coming free of the glue is a snap of the
 * wrist, and crossing the page is the length of the page, which is the one thing
 * the slow spring is for.
 *
 * [landing] names the point in the pad's own parent that the taken sheet is being
 * put down on, so the carry ends somewhere rather than in mid-air. Without one the
 * sheet drifts the pad's own length and is gone, which is all a pad standing on its
 * own can say.
 */
@Composable
fun StickyNotePad(
    onTake: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    taken: Boolean = false,
    reducedMotion: Boolean = false,
    beckons: Boolean = false,
    painter: Painter = painterResource(R.drawable.ic_add),
    landing: (() -> Offset)? = null,
    putBack: StickyNotePutBack? = null
) {
    val peel = remember { Animatable(STICKY_PEEL_REST) }
    val settle = remember { Animatable(STICKY_SETTLE_DONE) }
    val beckon = remember { Animatable(STICKY_FLAT) }
    val haptics = rememberPaperHaptics()
    var peeling by remember { mutableStateOf(false) }
    var returning by remember { mutableStateOf(false) }
    val previouslyTaken = remember { mutableStateOf(taken) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var seat by remember { mutableStateOf(Offset.Zero) }
    val dent = animateFloatAsState(
        targetValue = if (pressed) DENT_PRESSED else NO_DENT,
        animationSpec = PaperMotion.rowEnter,
        label = DENT_LABEL
    )
    /**
     * Taking a card takes the pen with it, to write on the card; it is put back
     * down when the card is.
     */
    val penLift = animateFloatAsState(
        targetValue = if (taken) PEN_LIFTED else PEN_RESTING,
        animationSpec = if (reducedMotion) snap() else PaperMotion.pickUp,
        label = PEN_LABEL
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
                peel.animateTo(STICKY_PEEL_GONE, PaperMotion.pageMove)
                peeling = false
            }
            haptics.land()
        } else if (reducedMotion) {
            return@LaunchedEffect
        } else if (landing == null) {
            settle.snapTo(STICKY_SETTLE_START)
            settle.animateTo(STICKY_SETTLE_DONE, PaperMotion.sheetSettle)
        } else {
            /**
             * Putting the sheet back is the same journey walked the other way: it
             * comes up off the line, is carried down the page on the same slow
             * spring, and lies flat on the pad — where it is indistinguishable from
             * the sheet at rest, so the two change places with nothing to see.
             */
            haptics.pickUp()
            returning = true
            peel.snapTo(STICKY_PEEL_GONE)
            peel.animateTo(STICKY_PEEL_LIFTED, PaperMotion.pageMove)
            peel.animateTo(STICKY_PEEL_REST, PaperMotion.pickUp)
            returning = false
            haptics.land()
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
    val flying = peeling || returning || settle.isRunning || beckon.isRunning
    Box(
        modifier = modifier
            .size(PaperDimens.stickyPad)
            .onGloballyPositioned { seat = it.positionInParent() }
            .preferredFrameRate(
                if (flying) FrameRateCategory.High else FrameRateCategory.Default
            ),
        contentAlignment = Alignment.Center
    ) {
        StickyNoteSheetSurface(
            color = palette.paperShade,
            modifier = Modifier.graphicsLayer {
                rotationZ = BACK_SHEET_ROTATION
                translationX = SHEET_STEP.toPx()
                translationY = SHEET_STEP.toPx()
            },
            shadowed = true
        )
        val showing = putBack.takeIf { taken }
        StickyNoteSheetSurface(
            color = palette.paperSheet,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = MID_SHEET_ROTATION
                    translationX = -SHEET_STEP.toPx()
                }
                .then(
                    if (showing == null) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = PaperFocusMark,
                            role = Role.Button,
                            onClick = showing.onPress
                        )
                    }
                ),
            shadowed = true,
            dent = { if (showing == null) NO_DENT else dent.value }
        ) {
            if (showing != null) {
                InkIcon(
                    painter = showing.painter,
                    contentDescription = showing.contentDescription,
                    modifier = Modifier.padding(GLYPH_INSET),
                    tint = palette.ink
                )
            }
        }
        if (!taken && !returning) {
            StickyNoteSheetSurface(
                color = palette.paperSheet,
                modifier = Modifier
                    .graphicsLayer {
                        val resting = stickyNoteSettleAt(settle.value)
                        transformOrigin = GLUED_EDGE
                        cameraDistance = PERSPECTIVE * density
                        rotationX = BECKON_DEGREES * beckon.value
                        rotationZ = TOP_SHEET_ROTATION + resting.rotationDegrees
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
                shadowed = true,
                dent = { dent.value }
            ) {
                InkIcon(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .padding(GLYPH_INSET)
                        .drawBehind { drawInkedRing(palette.inkRed) },
                    tint = palette.inkRed
                )
            }
        }

        Spacer(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = PEN_LIFTED - penLift.value
                    translationY = -PEN_LIFT_RISE.toPx() * penLift.value
                }
                .drawBehind { drawPen(palette) }
        )

        if (peeling || returning) {
            StickyNoteSheetSurface(
                color = palette.paperSheet,
                modifier = Modifier.graphicsLayer {
                    val flying = stickyNotePeelAt(peel.value)
                    val carried = stickyNoteCarryTo(
                        landing = landing?.invoke(),
                        seat = seat,
                        sheetInset = SHEET_INSET.toPx(),
                        drift = Offset(
                            -PaperDimens.stickyPeelTravelX.toPx(),
                            -PaperDimens.stickyPeelTravelY.toPx()
                        )
                    )
                    transformOrigin = GLUED_EDGE
                    cameraDistance = PERSPECTIVE * density
                    rotationX = FOLD_DEGREES * flying.foldFraction
                    rotationZ = TOP_SHEET_ROTATION + flying.rotationDegrees
                    scaleX = flying.scale
                    scaleY = flying.scale
                    alpha = flying.alpha
                    translationX = carried.x * flying.travelFraction
                    translationY = carried.y * flying.travelFraction
                }
            ) {
                /**
                 * The glyph is written on the sheet, not on the pad, so it goes
                 * where the sheet goes and comes back with it. Leaving it behind
                 * meant the sheet arrived home blank and the glyph appeared on it a
                 * beat later, out of nowhere.
                 */
                InkIcon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(GLYPH_INSET)
                        .drawBehind { drawInkedRing(palette.inkRed) },
                    tint = palette.inkRed
                )
            }
        }
    }
}
