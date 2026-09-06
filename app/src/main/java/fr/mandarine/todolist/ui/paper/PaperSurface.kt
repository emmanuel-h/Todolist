package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

private const val GRAIN_ALPHA = 0.35f
private const val VIGNETTE_RADIUS_FRACTION = 0.95f
private const val SMALLEST_RADIUS = 1f
private const val SMALLEST_SHEET = 1f
private const val HALF = 2f
private const val OPAQUE = 1f
private const val TRANSPARENT = 0f
private val UNPLACED = Float.NaN

@Immutable
internal class PaperSheetBrushes(
    val light: Brush,
    val grain: Brush,
    val corners: Brush,
    val blend: BlendMode
)

/**
 * Every brush here is told the sheet it is for. A brush handed no measurements takes
 * them from whatever is being drawn with it, which for a strip sampling one line of a
 * sheet means the sheet's whole light compressed into that line — the very thing the
 * strip exists to avoid.
 */
internal fun paperSheetBrushes(
    tile: ImageBitmap,
    lit: Color,
    tone: Color,
    vignette: Color,
    size: Size,
    grain: PaperGrain
): PaperSheetBrushes = PaperSheetBrushes(
    light = Brush.verticalGradient(
        colors = listOf(lit, tone),
        endY = size.height.coerceAtLeast(SMALLEST_SHEET)
    ),
    grain = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)),
    blend = grain.blend,
    corners = Brush.radialGradient(
        colors = listOf(Color.Transparent, vignette),
        center = Offset(size.width / HALF, size.height / HALF),
        radius = (VIGNETTE_RADIUS_FRACTION * maxOf(size.width, size.height))
            .coerceAtLeast(SMALLEST_RADIUS)
    )
)

internal fun DrawScope.drawPaperSheet(
    brushes: PaperSheetBrushes,
    alpha: Float = OPAQUE,
    sheet: Size = size
) {
    if (alpha <= TRANSPARENT) return
    drawRect(brushes.light, size = sheet, alpha = alpha)
    drawRect(brushes.grain, size = sheet, alpha = GRAIN_ALPHA * alpha, blendMode = brushes.blend)
    drawRect(brushes.corners, size = sheet, alpha = alpha)
}

@Composable
fun Modifier.paperSheet(
    tone: Color = LocalPaperPalette.current.paper,
    lit: Color = LocalPaperPalette.current.paperSheet
): Modifier {
    val vignette = LocalPaperPalette.current.vignette
    val grain = paperGrainOn(tone)
    val tile = paperGrainTile(LocalDensity.current.density, grain)
    return this.drawWithCache {
        val brushes = paperSheetBrushes(tile, lit, tone, vignette, size, grain)
        onDrawBehind { drawPaperSheet(brushes) }
    }
}

/**
 * A strip of the page rather than a sheet of its own. A sheet is lit at its top and
 * shaded at its corners, and a sheet only a line tall runs the whole of that inside
 * its own few millimetres: laid at the foot of the page it came out at the lit end
 * of a gradient the page had long since spent, and read as a white card lying on
 * cream.
 *
 * So the strip draws the whole sheet the window is painted with and shows the reader
 * the part of it it happens to be standing on. Being told where it stands is what a
 * fixed tone cannot do: the strip rides up the page with the keyboard, and a tone
 * taken from the foot of the page is wrong everywhere else. Before it has been
 * placed it stands at the foot, which is where it spends its life.
 *
 * The clip is what keeps it a strip. Nothing clips a draw to the bounds of the thing
 * that asked for it, so the sheet drawn here to be sampled a line at a time will
 * otherwise be drawn whole, over the page and everything written on it.
 */
@Composable
fun Modifier.paperStrip(): Modifier {
    val palette = LocalPaperPalette.current
    val grain = paperGrainOn(palette.paper)
    val tile = paperGrainTile(LocalDensity.current.density, grain)
    val window = LocalWindowInfo.current.containerSize
    var placed by remember { mutableFloatStateOf(UNPLACED) }
    return this
        .onPlaced { placed = it.positionInRoot().y }
        .drawWithCache {
            val sheet = Size(size.width, window.height.toFloat().coerceAtLeast(size.height))
            val stands = if (placed.isNaN()) sheet.height - size.height else placed
            val brushes = paperSheetBrushes(
                tile = tile,
                lit = palette.paperSheet,
                tone = palette.paper,
                vignette = palette.vignette,
                size = sheet,
                grain = grain
            )
            onDrawBehind {
                clipRect {
                    translate(top = -stands) { drawPaperSheet(brushes, sheet = sheet) }
                }
            }
        }
}

/**
 * A sheet whose opacity is read at draw time, so a row can lift off the page and
 * lay back down without recomposing once per frame.
 */
@Composable
fun Modifier.paperSheetFading(
    opacity: () -> Float,
    tone: Color = LocalPaperPalette.current.paperSheet,
    lit: Color = LocalPaperPalette.current.paperSheet
): Modifier {
    val vignette = LocalPaperPalette.current.vignette
    val grain = paperGrainOn(tone)
    val tile = paperGrainTile(LocalDensity.current.density, grain)
    return this.drawWithCache {
        val brushes = paperSheetBrushes(tile, lit, tone, vignette, size, grain)
        onDrawBehind { drawPaperSheet(brushes, opacity()) }
    }
}

/**
 * The page, and the one veil drawn over it: every sheet laid on the page dims it
 * here, once, so a calendar opened over an edit slip costs the page no more light
 * than the slip alone did.
 */
@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .paperGround()
    ) {
        content()
        PageVeil()
    }
}
