package fr.mandarine.todolist.ui.paper

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

private const val GRAIN_SEED = 0x5EED
private const val GRAIN_MAX_ALPHA = 26
private const val GRAIN_ALPHA = 0.4f
private const val ALPHA_SHIFT = 24

internal fun paperGrainTile(): ImageBitmap {
    val side = PaperDimens.GRAIN_TILE_PIXELS
    val random = Random(GRAIN_SEED)
    val pixels = IntArray(side * side) {
        random.nextInt(GRAIN_MAX_ALPHA + 1) shl ALPHA_SHIFT
    }
    return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888).asImageBitmap()
}

@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val grainTile = remember { paperGrainTile() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .drawWithCache {
                val grain = ShaderBrush(
                    ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated)
                )
                val radius = PaperDimens.holeRadius.toPx()
                val centerX = PaperDimens.holeCenterX.toPx()
                val period = PaperDimens.holePeriod.toPx()
                val firstCenterY = PaperDimens.holeFirstCenterY.toPx()
                onDrawBehind {
                    drawRect(PaperInk.paper)
                    drawRect(brush = grain, alpha = GRAIN_ALPHA)
                    var centerY = firstCenterY
                    while (centerY - radius < size.height) {
                        drawCircle(PaperInk.hole, radius, Offset(centerX, centerY))
                        centerY += period
                    }
                }
            },
        content = content
    )
}
