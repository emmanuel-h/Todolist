package fr.mandarine.todolist.ui.paper

import android.graphics.Bitmap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import java.util.concurrent.ConcurrentHashMap

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
private const val LIT_GROUND = 1f
private const val UNLIT_GROUND = 0f

/**
 * Paper takes its texture from the light it is under. In daylight the fibres sit
 * in shadow, so the tile is a bright ground pitted with dark flecks and multiplied
 * into the sheet; under a lamp the same fibres are the only thing catching light,
 * so the tile is a dark ground lit by pale flecks and screened onto it. Both are
 * baked from one lattice, so a sheet keeps its texture whatever it is read by.
 */
enum class PaperGrain(
    internal val ground: Float,
    internal val fleck: Float,
    internal val blend: BlendMode
) {
    DarkFleck(LIT_GROUND, -GRAIN_DEPTH, BlendMode.Multiply),
    PaleFibre(UNLIT_GROUND, GRAIN_DEPTH, BlendMode.Screen)
}

internal fun paperGrainOn(tone: Color): PaperGrain =
    if (tone.unlit) PaperGrain.PaleFibre else PaperGrain.DarkFleck

private class GrainLattice(val cells: Int, val values: FloatArray)

private data class GrainKey(val density: Float, val grain: PaperGrain)

private object PaperGrainCache {
    val baked = ConcurrentHashMap<GrainKey, ImageBitmap>()
}

/**
 * Baking a tile costs a quarter of a million pixels of noise, so it is done once
 * per density and kept. The keeping is atomic: a read-modify-write over a plain
 * field could drop a tile baked concurrently, and two threads arriving together
 * would each bake their own. Here the second one waits for the first rather than
 * repeating its work — which is what the preload is for, and what a composition
 * arriving before the preload finished used to miss.
 *
 * Only the density on screen is worth keeping; a tile for a density nothing is
 * drawn at any more is dropped.
 */
fun paperGrainTile(density: Float, grain: PaperGrain): ImageBitmap {
    PaperGrainCache.baked.keys.removeAll { it.density != density }
    return PaperGrainCache.baked.computeIfAbsent(GrainKey(density, grain)) {
        bakePaperGrainTile(density, grain)
    }
}

internal fun bakePaperGrainTile(density: Float, grain: PaperGrain): ImageBitmap {
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
            val shade = (grain.ground + grain.fleck * noise + fibres[index]).coerceIn(0f, 1f)
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
