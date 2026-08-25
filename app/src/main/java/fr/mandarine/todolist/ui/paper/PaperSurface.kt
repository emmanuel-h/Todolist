package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.platform.LocalDensity

private const val GRAIN_ALPHA = 0.35f
private const val VIGNETTE_RADIUS_FRACTION = 0.95f
private const val SMALLEST_RADIUS = 1f
private const val HALF = 2f
private const val OPAQUE = 1f
private const val TRANSPARENT = 0f

@Immutable
internal class PaperSheetBrushes(
    val light: Brush,
    val grain: Brush,
    val corners: Brush,
    val blend: BlendMode
)

internal fun paperSheetBrushes(
    tile: ImageBitmap,
    lit: Color,
    tone: Color,
    vignette: Color,
    size: Size,
    grain: PaperGrain
): PaperSheetBrushes = PaperSheetBrushes(
    light = Brush.verticalGradient(listOf(lit, tone)),
    grain = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)),
    blend = grain.blend,
    corners = Brush.radialGradient(
        colors = listOf(Color.Transparent, vignette),
        center = Offset(size.width / HALF, size.height / HALF),
        radius = (VIGNETTE_RADIUS_FRACTION * maxOf(size.width, size.height))
            .coerceAtLeast(SMALLEST_RADIUS)
    )
)

internal fun DrawScope.drawPaperSheet(brushes: PaperSheetBrushes, alpha: Float = OPAQUE) {
    if (alpha <= TRANSPARENT) return
    drawRect(brushes.light, alpha = alpha)
    drawRect(brushes.grain, alpha = GRAIN_ALPHA * alpha, blendMode = brushes.blend)
    drawRect(brushes.corners, alpha = alpha)
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
