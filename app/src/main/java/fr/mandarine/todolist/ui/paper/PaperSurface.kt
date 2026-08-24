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

@Immutable
internal class PaperSheetBrushes(val light: Brush, val grain: Brush, val corners: Brush)

internal fun paperSheetBrushes(
    tile: ImageBitmap,
    lit: Color,
    tone: Color,
    vignette: Color,
    size: Size
): PaperSheetBrushes = PaperSheetBrushes(
    light = Brush.verticalGradient(listOf(lit, tone)),
    grain = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)),
    corners = Brush.radialGradient(
        colors = listOf(Color.Transparent, vignette),
        center = Offset(size.width / HALF, size.height / HALF),
        radius = (VIGNETTE_RADIUS_FRACTION * maxOf(size.width, size.height))
            .coerceAtLeast(SMALLEST_RADIUS)
    )
)

internal fun DrawScope.drawPaperSheet(brushes: PaperSheetBrushes) {
    drawRect(brushes.light)
    drawRect(brushes.grain, alpha = GRAIN_ALPHA, blendMode = BlendMode.Multiply)
    drawRect(brushes.corners)
}

@Composable
fun Modifier.paperSheet(
    tone: Color = LocalPaperPalette.current.paper,
    lit: Color = LocalPaperPalette.current.paperSheet
): Modifier {
    val vignette = LocalPaperPalette.current.vignette
    val tile = paperGrainTile(LocalDensity.current.density)
    return this.drawWithCache {
        val brushes = paperSheetBrushes(tile, lit, tone, vignette, size)
        onDrawBehind { drawPaperSheet(brushes) }
    }
}

@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .paperSheet(),
        content = content
    )
}
