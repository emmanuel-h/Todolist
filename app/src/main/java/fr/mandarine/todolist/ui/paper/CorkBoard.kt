package fr.mandarine.todolist.ui.paper

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.domain.ListColour
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.random.Random

private const val CORK_TILE_PIXELS = 192
private const val CORK_SEED = 0xC0_4B
private const val GRANULES_PER_TILE = 900
private const val DARK_GRANULE_SHARE = 0.55f
private const val GRANULE_MIN_STRENGTH = 0.25f
private const val GRANULE_RANGE = 0.6f
private const val WIDE_GRANULE_SHARE = 0.2f
private const val MOTTLE_CELLS = 6
private const val MOTTLE_DEPTH = 0.18f
private const val TILT_RANGE_DEGREES = 1.8f
private const val TILT_BUCKETS = 255f
private const val TAPE_TILT_DEGREES = -3f
private const val TAPE_OPACITY = 0.82f
private const val DAY_TILT_DEGREES = 4f
private const val DAY_BAND_SHARE = 0.32f
private const val HALF = 2f

/** One cork tile per density and light, baked once and shared, like the paper grain. */
private data class CorkTileKey(val density: Float, val ground: Int, val dark: Int, val light: Int)

private object CorkTiles {
    val baked = ConcurrentHashMap<CorkTileKey, ImageBitmap>()
}

/**
 * Cork is a pressed mass of small granules, so the tile is a mottled ground with
 * granules scattered over it — darker pits and lighter crumbs — rather than noise
 * laid on a flat fill. It is baked once per density and palette from a fixed seed,
 * so every board is the same board and nothing random runs while drawing.
 */
internal fun corkTile(density: Float, ground: Color, dark: Color, light: Color): ImageBitmap {
    val key = CorkTileKey(density, ground.toArgb(), dark.toArgb(), light.toArgb())
    return CorkTiles.baked.computeIfAbsent(key) { bakeCorkTile(ground, dark, light) }
}

internal fun bakeCorkTile(ground: Color, dark: Color, light: Color): ImageBitmap {
    val side = CORK_TILE_PIXELS
    val random = Random(CORK_SEED)
    val mottle = FloatArray(MOTTLE_CELLS * MOTTLE_CELLS) { random.nextFloat() * HALF - 1f }
    val pixels = IntArray(side * side) { index ->
        val x = index % side
        val y = index / side
        val shade = mottleAt(mottle, x.toFloat() / side, y.toFloat() / side) * MOTTLE_DEPTH
        mixed(ground, if (shade < 0f) dark else light, abs(shade)).toArgb()
    }
    repeat(GRANULES_PER_TILE) {
        val x = random.nextInt(side)
        val y = random.nextInt(side)
        val towards = if (random.nextFloat() < DARK_GRANULE_SHARE) dark else light
        val strength = GRANULE_MIN_STRENGTH + random.nextFloat() * GRANULE_RANGE
        val spread = if (random.nextFloat() < WIDE_GRANULE_SHARE) 1 else 0
        for (dy in -spread..spread) {
            for (dx in -spread..spread) {
                val px = Math.floorMod(x + dx, side)
                val py = Math.floorMod(y + dy, side)
                val at = py * side + px
                pixels[at] = mixed(Color(pixels[at]), towards, strength).toArgb()
            }
        }
    }
    return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private fun mottleAt(cells: FloatArray, u: Float, v: Float): Float {
    val x = u * MOTTLE_CELLS
    val y = v * MOTTLE_CELLS
    val column = x.toInt()
    val row = y.toInt()
    val fx = smooth(x - column)
    val fy = smooth(y - row)
    fun at(c: Int, r: Int) = cells[Math.floorMod(r, MOTTLE_CELLS) * MOTTLE_CELLS + Math.floorMod(c, MOTTLE_CELLS)]
    val top = at(column, row) + (at(column + 1, row) - at(column, row)) * fx
    val bottom = at(column, row + 1) + (at(column + 1, row + 1) - at(column, row + 1)) * fx
    return top + (bottom - top) * fy
}

private fun smooth(f: Float): Float = f * f * (3f - 2f * f)

private fun mixed(from: Color, to: Color, amount: Float): Color = Color(
    red = from.red + (to.red - from.red) * amount,
    green = from.green + (to.green - from.green) * amount,
    blue = from.blue + (to.blue - from.blue) * amount
)

/**
 * The ground the lists are pinned to: cork, edge to edge and behind the system
 * bars. A board is not a page, so nothing here is ruled.
 *
 * [dimmed] is the same cork with the room's light turned down, which is what is
 * left behind a list lifted off the board.
 */
@Composable
fun Modifier.corkGround(dimmed: Boolean = false): Modifier {
    val palette = LocalPaperPalette.current
    val ground = if (dimmed) palette.corkDim else palette.cork
    val tile = corkTile(LocalDensity.current.density, ground, palette.corkFleckDark, palette.corkFleckLight)
    return drawWithCache {
        val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
        onDrawBehind { drawRect(brush) }
    }
}

@Composable
fun CorkSurface(
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().then(modifier).corkGround(dimmed)) {
        content()
        PageVeil()
    }
}

/**
 * A sheet of paper resting on the cork: the paper itself, grain and all, with the
 * soft shadow it casts on the board. [lifted] is the same sheet held up off the
 * board, which throws a longer, darker shadow.
 */
@Composable
fun Modifier.pinnedPaper(
    lifted: Boolean = false,
    tone: Color = LocalPaperPalette.current.paperSheet
): Modifier {
    val palette = LocalPaperPalette.current
    val radius = if (lifted) CorkDimens.liftedShadow else CorkDimens.restingShadow
    val drop = if (lifted) CorkDimens.liftedDrop else CorkDimens.restingDrop
    val alpha = if (lifted) CorkDimens.LIFTED_SHADOW_ALPHA else CorkDimens.RESTING_SHADOW_ALPHA
    return this
        .dropShadow(RectangleShape) {
            this.radius = radius.toPx()
            this.alpha = alpha
            color = palette.shadow
            offset = Offset(0f, drop.toPx())
        }
        .paperSheet(tone = tone, lit = palette.paperSheet)
}

/**
 * Each card sits a little askew, the way paper pinned by hand does. The angle comes
 * from the list's id, so a card keeps its tilt for as long as it is on the board and
 * two neighbours rarely lean the same way.
 */
fun cardTilt(id: String): Float =
    ((id.hashCode() and 0xFF) / TILT_BUCKETS * HALF - 1f) * TILT_RANGE_DEGREES

/**
 * The strip of tape a card is pinned up with. The reader's colour for a list goes
 * on paper and never on ink, and the tape is paper: a coloured list is taped in
 * its own wash, an uncoloured one in plain tape.
 */
@Composable
fun PaperTape(colour: Color, modifier: Modifier = Modifier, width: Dp = CorkDimens.tapeWidth) {
    Box(
        modifier = modifier
            .size(width, CorkDimens.tapeHeight)
            .graphicsLayer { rotationZ = TAPE_TILT_DEGREES }
            .drawWithCache {
                val tint = colour.copy(alpha = TAPE_OPACITY)
                onDrawBehind { drawRect(tint) }
            }
    )
}

/**
 * The tape a list is pinned with: its own wash, or plain tape for a list the reader
 * has not coloured.
 */
fun PaperPalette.tapeFor(colour: ListColour): Color =
    if (colour == ListColour.None) tape else highlightWash(colour)

/**
 * A page torn off a day-to-a-page calendar and clipped to a card's corner: the month
 * printed small on a band in the day's ink, the day's numeral under it. It is the
 * one place a card says it has a day, so an undated card has none.
 *
 * The band is the day's [tone] — red once missed, amber today, pencil otherwise —
 * and a target day already behind us is [struck] rather than reddened, the way the
 * date jot is. Pressing it is pressing the jot: it asks for the calendar on that day.
 */
@Composable
fun TearOffDay(
    month: String,
    day: String,
    tone: InkTone,
    spoken: String,
    modifier: Modifier = Modifier,
    struck: Boolean = false,
    pressLabel: String? = null,
    onPress: (() -> Unit)? = null
) {
    val palette = LocalPaperPalette.current
    val band = palette.inked(tone)
    Box(
        modifier = modifier
            .size(PaperDimens.touchTarget)
            .clearAndSetSemantics {
                contentDescription = spoken
                if (onPress != null) {
                    role = Role.Button
                    onClick(label = pressLabel) { onPress(); true }
                }
            }
            .then(if (onPress != null) Modifier.clickable(onClick = onPress) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .size(CorkDimens.dayWidth, CorkDimens.dayHeight)
                .graphicsLayer { rotationZ = DAY_TILT_DEGREES }
                .pinnedPaper()
                .drawWithCache {
                    val bandHeight = size.height * DAY_BAND_SHARE
                    onDrawBehind {
                        drawRect(band, size = size.copy(height = bandHeight))
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(CorkDimens.dayHeight * DAY_BAND_SHARE),
                contentAlignment = Alignment.Center
            ) {
                Text(text = month, style = PaperType.tearOffMonth, color = palette.paper, maxLines = 1)
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = day,
                    modifier = Modifier.wrapContentSize(),
                    style = PaperType.tearOffDay.copy(
                        textDecoration = if (struck) TextDecoration.LineThrough else null
                    ),
                    color = palette.inked(if (struck) InkTone.Crossed else InkTone.Words),
                    maxLines = 1
                )
            }
        }
    }
}

/** The measurements of things pinned to cork. */
@Immutable
object CorkDimens {
    val restingShadow = 6.dp
    val restingDrop = 3.dp
    val liftedShadow = 22.dp
    val liftedDrop = 12.dp
    const val RESTING_SHADOW_ALPHA = 0.32f
    const val LIFTED_SHADOW_ALPHA = 0.45f
    val tapeWidth = 64.dp
    val tapeHeight = 18.dp
    val tapeOverlap = 9.dp
    val dayWidth = 34.dp
    val dayHeight = 38.dp
    val cardMinWidth = 152.dp
    val cardGap = 14.dp
    val boardMargin = 14.dp
    val boardWidth = 960.dp
    val dayOverhang = 14.dp
}
