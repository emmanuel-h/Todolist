package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

private const val DISABLED_TINT_ALPHA = 0.38f
private const val NIB_SQUASH = 0.92f
private const val NIB_REST = 1f
private const val NIB_LABEL = "nibSquash"

enum class IconSeat { Centred, OnRule }

/**
 * Where the ink stops inside a glyph's own box, as a fraction of that box: every
 * glyph is stroked on the same 24 unit grid, so the foot is the lowest point of
 * its path plus the half nib the round cap adds under it.
 */
@Immutable
object GlyphFoot {
    const val arrow = 20f / 24f
    const val chevron = 19f / 24f
    const val check = 18f / 24f
    const val trash = 23f / 24f
    const val pencil = 23f / 24f
}

@Composable
fun InkIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalPaperPalette.current.inked(InkTone.Words),
    size: Dp = PaperDimens.iconGlyph
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}

@Composable
fun InkIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalPaperPalette.current.inkSoft,
    enabled: Boolean = true,
    seat: IconSeat = IconSeat.Centred,
    foot: Float = GlyphFoot.arrow
) {
    val onRule = seat == IconSeat.OnRule
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val squash by animateFloatAsState(
        targetValue = if (pressed) NIB_SQUASH else NIB_REST,
        animationSpec = PaperMotion.pickUp,
        label = NIB_LABEL
    )
    Box(
        modifier = modifier
            .width(PaperDimens.iconButton)
            .height(if (onRule) LocalPagePitch.current else PaperDimens.iconButton)
            .clickable(
                interactionSource = interactionSource,
                indication = PaperFocusMark,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = if (onRule) Alignment.TopCenter else Alignment.Center
    ) {
        val seated = if (onRule) Modifier.seatGlyphOnRule(foot) else Modifier
        InkIcon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = seated.graphicsLayer {
                scaleX = squash
                scaleY = squash
            },
            tint = if (enabled) tint else tint.copy(alpha = DISABLED_TINT_ALPHA)
        )
    }
}
