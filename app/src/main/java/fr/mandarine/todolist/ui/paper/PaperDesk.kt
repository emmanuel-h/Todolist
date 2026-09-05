package fr.mandarine.todolist.ui.paper

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

private const val DESK_SHADOW_ALPHA = 0.16f
private const val ON_THE_DESK = 0f

private val DESK_SHADOW = 18.dp
private val DESK_SHADOW_DROP = 8.dp

/**
 * How much desk there is to lay the page on. On a phone the page is the whole
 * window and there is no desk to see; a wider window is a wider sheet, so the
 * margin widens with it rather than the writing running the width of a table; and
 * once the window is wider than any sheet the composition becomes literal — one
 * page of paper lying on a desk, with room beside it for the pad.
 */
enum class PageFit { FillsTheWindow, KeepsMargins, LiesOnADesk }

val LocalPageFit = staticCompositionLocalOf { PageFit.FillsTheWindow }

val LocalPaperGutter = staticCompositionLocalOf { PaperDimens.gutter }

@Composable
fun rememberPageFit(): PageFit {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return remember(sizeClass) { pageFitFor(sizeClass) }
}

internal fun pageFitFor(sizeClass: WindowSizeClass): PageFit = when {
    sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
        PageFit.LiesOnADesk
    sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
        PageFit.KeepsMargins
    else -> PageFit.FillsTheWindow
}

internal fun gutterFor(fit: PageFit): Dp =
    if (fit == PageFit.FillsTheWindow) PaperDimens.gutter else PaperDimens.wideGutter

/**
 * The ground the window is painted with: the page itself where the page fills the
 * window, and the desk it lies on once there is a desk.
 */
@Composable
fun Modifier.paperGround(): Modifier {
    val palette = LocalPaperPalette.current
    return if (LocalPageFit.current == PageFit.LiesOnADesk) {
        paperSheet(tone = palette.desk, lit = palette.deskLit)
    } else {
        paperSheet()
    }
}

/**
 * A sheet on a desk is an object with edges, so it is given its own paper and the
 * shadow it drops onto the wood. Where the page is the window there is nothing to
 * drop a shadow onto and the sheet is already painted underneath.
 */
@Composable
internal fun Modifier.laidOnTheDesk(): Modifier {
    if (LocalPageFit.current != PageFit.LiesOnADesk) return this
    val palette = LocalPaperPalette.current
    return this
        .dropShadow(RectangleShape) {
            radius = DESK_SHADOW.toPx()
            alpha = DESK_SHADOW_ALPHA
            color = palette.shadow
            offset = Offset(ON_THE_DESK, DESK_SHADOW_DROP.toPx())
        }
        .paperSheet()
}
