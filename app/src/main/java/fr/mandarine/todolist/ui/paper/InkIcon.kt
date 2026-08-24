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

@Composable
fun InkIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalPaperPalette.current.ink,
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
    seat: IconSeat = IconSeat.Centred
) {
    val onRule = seat == IconSeat.OnRule
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val squash by animateFloatAsState(
        targetValue = if (pressed) NIB_SQUASH else NIB_REST,
        animationSpec = PaperMotion.nibSquash,
        label = NIB_LABEL
    )
    Box(
        modifier = modifier
            .width(PaperDimens.iconButton)
            .height(if (onRule) LocalPagePitch.current else PaperDimens.iconButton)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = if (onRule) Alignment.BottomCenter else Alignment.Center
    ) {
        InkIcon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.graphicsLayer {
                scaleX = squash
                scaleY = squash
            },
            tint = if (enabled) tint else tint.copy(alpha = DISABLED_TINT_ALPHA)
        )
    }
}
