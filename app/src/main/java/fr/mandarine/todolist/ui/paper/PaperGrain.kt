package fr.mandarine.todolist.ui.paper

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val FINE_SEED = 0x5EED
private const val COARSE_SEED = 0x0FA1
private const val FIBRE_SEED = 0xF18E
private const val FINE_WAVELENGTH_DP = 3f
private const val COARSE_WAVELENGTH_DP = 40f
private const val FINE_AMPLITUDE = 0.6f
private const val COARSE_AMPLITUDE = 0.4f
private const val GRAIN_DEPTH = 0.08f
private const val FIBRE_COUNT = 12
private const val FIBRE_SHORTEST = 0.12f
private const val FIBRE_LONGEST = 0.38f
private const val FIBRE_LIFT = 0.06f
private const val FIBRE_SPREAD = 0.5f
private const val CHANNEL_MAX = 255f
private const val OPAQUE_ALPHA = 0xFF shl 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val SMALLEST_LATTICE = 2

private class GrainLattice(val cells: Int, val values: FloatArray)

private class BakedGrain(val density: Float, val tile: ImageBitmap)

private object PaperGrainCache {
    @Volatile
    var baked: BakedGrain? = null
}

fun paperGrainTile(density: Float): ImageBitmap {
    val cached = PaperGrainCache.baked
    if (cached != null && cached.density == density) return cached.tile
    val tile = bakePaperGrainTile(density)
    PaperGrainCache.baked = BakedGrain(density, tile)
    return tile
}

internal fun bakePaperGrainTile(density: Float): ImageBitmap {
    val side = PaperDimens.GRAIN_TILE_PIXELS
    val fine = grainLattice(FINE_SEED, FINE_WAVELENGTH_DP * density)
    val coarse = grainLattice(COARSE_SEED, COARSE_WAVELENGTH_DP * density)
    val fibres = paperFibres(side)
    val pixels = IntArray(side * side)
    for (row in 0 until side) {
        val v = row.toFloat() / side
        for (column in 0 until side) {
            val u = column.toFloat() / side
            val noise = FINE_AMPLITUDE * fine.sample(u, v) + COARSE_AMPLITUDE * coarse.sample(u, v)
            val index = row * side + column
            val shade = (1f - GRAIN_DEPTH * noise + fibres[index]).coerceIn(0f, 1f)
            val channel = (shade * CHANNEL_MAX).roundToInt()
            pixels[index] =
                OPAQUE_ALPHA or (channel shl RED_SHIFT) or (channel shl GREEN_SHIFT) or channel
        }
    }
    return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private fun grainLattice(seed: Int, wavelengthPixels: Float): GrainLattice {
    val side = PaperDimens.GRAIN_TILE_PIXELS
    val cells = (side / wavelengthPixels).roundToInt().coerceIn(SMALLEST_LATTICE, side)
    val random = Random(seed)
    return GrainLattice(cells, FloatArray(cells * cells) { random.nextFloat() })
}

private fun GrainLattice.sample(u: Float, v: Float): Float {
    val x = u * cells
    val y = v * cells
    val column = floor(x).toInt()
    val row = floor(y).toInt()
    val acrossWeight = smoothStep(x - column)
    val downWeight = smoothStep(y - row)
    val left = column % cells
    val right = (column + 1) % cells
    val top = (row % cells) * cells
    val bottom = ((row + 1) % cells) * cells
    val upper = mix(values[top + left], values[top + right], acrossWeight)
    val lower = mix(values[bottom + left], values[bottom + right], acrossWeight)
    return mix(upper, lower, downWeight)
}

private fun paperFibres(side: Int): FloatArray {
    val lift = FloatArray(side * side)
    val random = Random(FIBRE_SEED)
    repeat(FIBRE_COUNT) {
        val row = random.nextInt(side)
        val start = random.nextInt(side)
        val span = FIBRE_SHORTEST + random.nextFloat() * (FIBRE_LONGEST - FIBRE_SHORTEST)
        val length = (side * span).roundToInt()
        val strength = FIBRE_LIFT * (1f - FIBRE_SPREAD + random.nextFloat() * FIBRE_SPREAD)
        val below = ((row + 1) % side) * side
        val along = row * side
        for (step in 0 until length) {
            val taper = sin(PI * step / length).toFloat()
            val column = (start + step) % side
            lift[along + column] += strength * taper
            lift[below + column] += strength * taper * FIBRE_SPREAD
        }
    }
    return lift
}

private fun smoothStep(fraction: Float): Float = fraction * fraction * (3f - 2f * fraction)

private fun mix(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
